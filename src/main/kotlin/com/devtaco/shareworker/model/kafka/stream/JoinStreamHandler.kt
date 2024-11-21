package com.devtaco.shareworker.model.kafka.stream

import com.devtaco.shareworker.config.KafkaTopicConfig.Companion.SHARE_REPARTITION_TOPIC
import com.devtaco.shareworker.config.KafkaTopicConfig.Companion.SUMMARY_REPARTITION_TOPIC
import com.devtaco.shareworker.model.kafka.consumer.ShareDataConsumer
import com.devtaco.shareworker.model.kafka.payload.Payload
import com.devtaco.shareworker.model.kafka.payload.ShareDataPayload
import com.devtaco.shareworker.model.kafka.payload.SnsSharePayload
import com.devtaco.shareworker.model.kafka.payload.SummaryCompletePayload
import com.devtaco.shareworker.model.kafka.producer.KafkaProducer
import com.devtaco.shareworker.model.PayloadChannel
import com.devtaco.shareworker.utils.moshiSerde
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * [SummaryCompletePayload] 와 [ShareDataPayload] 를 join 하여 [SnsSharePayload] 를 만들어내는 Stream Handler
 *
 * [ShareDataConsumer] 에서 받은 Payload 들을 [summaryJoinStream] 의 KStream 과 KTable 에 넣어 join 을 수행한다.
 *
 * 먼저 도착하는 data 는 KTable 에 넣어두고, 뒤에 도착하는 Summary 는 KStream 에 넣어 join 을 수행한다.
 *
 * key 값은 원출 에서 미리 설정해서 같은 키로 실어 보낸다. (identifier)
 *
 */

@Component
class JoinStreamHandler(
    private val applicationCoroutineScope: CoroutineScope,
    private val payloadChannel: PayloadChannel<Payload>,
    private val kafkaProducer: KafkaProducer,
) {

    @Value("\${spring.kafka.streams.application-id}")
    private lateinit var applicationId: String

    @Value("\${spring.kafka.streams.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    private lateinit var summaryJoinStream: KafkaStreams
    private lateinit var cleanupJob: Job  // 스케줄러 작업을 위한 Job 선언

    private val log = KotlinLogging.logger {}

    companion object {
        private const val ORIGIN_DATA_KTABLE = "originDataTable"
        private val CLEANUP_INTERVAL_MS = TimeUnit.HOURS.toMillis(1)
    }


    @PostConstruct
    fun init() {
        startStream()
        cleanUpScheduler()
    }

    fun startStream() {
        summaryJoinStream = createKafkaStreams()
        // Kafka Streams 상태 리스너 등록 -> 상태 변경 로그 출력
        // REBALANCING 중에는 데이터 처리가 중단될 수 있다. 상태가 RUNNER 로 바뀌어야 한다.
        summaryJoinStream.setStateListener { newState, oldState ->
            log.info { "Kafka Streams state changed from $oldState =======> $newState" }
        }
        summaryJoinStream.start()
    }

    private fun createKafkaStreams(): KafkaStreams {
        val props = Properties().apply {
            put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
            put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String()::class.java.name)
            put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String()::class.java.name)
            put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000L)  // 1분마다 커밋
            put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0)
            put(
                StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
                LogAndContinueExceptionHandler::class.java.name
            )
        }
        return KafkaStreams(createTopology().build(), props)
    }

    private fun getKTableStore(): ReadOnlyKeyValueStore<String, ShareDataPayload>? {
        return summaryJoinStream.store(
            StoreQueryParameters.fromNameAndType(
                ORIGIN_DATA_KTABLE,
                QueryableStoreTypes.keyValueStore()
            )
        )
    }


    /** KTable(newsOriginTable) 에서 1시간 이상 된 데이터를 삭제하는 메서드 */
    private suspend fun cleanupKTable() {
        if (summaryJoinStream.state() != KafkaStreams.State.RUNNING) {
            log.info { "KafkaStreams is not in RUNNING state. Skipping cleanup task." }
            return
        }

        try {
            val store = getKTableStore() ?: run {
                log.error { "Failed to get KTable store. Skipping cleanup task." }
                return
            }

            val currentTime = Instant.now().toEpochMilli()
            val maxItemsPerCleanup = 100
            val entriesToClean = mutableListOf<Pair<String, ShareDataPayload>>()

            // 스냅샷을 생성하여 현재 상태의 데이터를 정리 대상에 추가
            store.all().use { iterator ->
                while (iterator.hasNext() && entriesToClean.size < maxItemsPerCleanup) {
                    val entry = iterator.next()
                    val timestamp = entry.value.streamRegisterEpoch
                    if (currentTime - timestamp > CLEANUP_INTERVAL_MS) {
                        entriesToClean.add(entry.key to entry.value)
                    }
                }
            }

            // 스냅샷된 데이터를 기반으로 정리 작업 수행
            entriesToClean.forEach { (key, _) ->
                log.info { "Deleting expired entry for key=$key from KTable" }
                sendAndCheckTombstone(key, SHARE_REPARTITION_TOPIC)
            }

            log.info { "Completed cleanup for ${entriesToClean.size} expired entries from KTable." }
        } catch (e: InvalidStateStoreException) {
            log.warn { "State store is not available yet (possibly rebalancing). Skipping cleanup task and will retry later..." }
        }
    }

    /***
     * KafkaStremams 에 등록할 Topology (Ktable, KStream 정의)
     */
    private fun createTopology(): StreamsBuilder {
        val builder = StreamsBuilder()

        // KTable 정의
        val originDataTable: KTable<String, ShareDataPayload> = builder.table(
            SHARE_REPARTITION_TOPIC,
            Consumed.with(Serdes.String(), moshiSerde<ShareDataPayload>()),
            Materialized.`as`<String, ShareDataPayload, KeyValueStore<Bytes, ByteArray>>(
                ORIGIN_DATA_KTABLE
            )
                .withCachingDisabled()
        ).filter { _: String, value ->
            if (value.content.length <= 150) {
                log.info { "Content length is less than 150 characters. Sending immediately to SNS." }
                log.info { "Sending to SNS: key=${value.identifier}" }
                // content가 150자 이하일 경우, 바로 SnsSharePayload를 생성하여 전송
                val snsPayload = SnsSharePayload(
                    identifier = value.identifier,
                    seq = value.seq,
                    title = value.title,
                    content = value.content,
                    actionOperator = value.actionOperator,
                    summaryList = emptyList()  // 요약 데이터가 없으므로 빈 리스트로 설정
                )

                // payloadChannel로 즉시 전송
                applicationCoroutineScope.launch {
                    payloadChannel.sendPayload(snsPayload)
                }
                false // KTable에 저장하지 않음
            } else {
                log.info { "Content length is greater than 150 characters. Storing in KTable." }
                log.info { "Storing in KTable: key=${value.identifier}" }
                true // content가 150자 이상일 경우에만 KTable에 저장
            }
        }.mapValues { value ->
            value  // 모든 값을 그대로 반영
        }

        // KStream 정의
        // 해당 데이터는 join 이 끝나면 삭제된다. tombstone 메시지를 전송하여 삭제한다.
        val summaryDataStream: KStream<String, SummaryCompletePayload> = builder.stream(
            SUMMARY_REPARTITION_TOPIC,
            Consumed.with(Serdes.String(), moshiSerde<SummaryCompletePayload>())
        ).filter { key, value ->
            key != null && value != null
        }

        // KStream 에 데이터가 들어왔을 때, KTable 에 있는 데이터와 조인한다.
        summaryDataStream.join(
            originDataTable,
            this::joinDataWithSummary,
            Joined.with(Serdes.String(), moshiSerde(), moshiSerde())
        )

        return builder
    }

    /**
     * 실제 join 후 [PayloadChannel]로 보내는 로직
     */
    private fun joinDataWithSummary(
        summaryPayload: SummaryCompletePayload,
        sharePayload: ShareDataPayload?
    ): SnsSharePayload? {
        if (sharePayload == null) {
            log.warn { "No matching KTable data for key=${summaryPayload.identifier}. Join failed." }
            return null
        }

        val identifier = sharePayload.identifier

        val completePayload = SnsSharePayload(
            identifier = identifier,
            seq = sharePayload.seq,
            title = sharePayload.title,
            content = sharePayload.content,
            actionOperator = sharePayload.actionOperator,
            summaryList = summaryPayload.summaryList
        )

        // 비동기 작업을 별도로 실행
        applicationCoroutineScope.launch {
            payloadChannel.sendPayload(completePayload)
            sendAndCheckTombstone(identifier, SHARE_REPARTITION_TOPIC)
        }

        return completePayload
    }

    /**
     * Tombstone 메시지 (key 에 null 을 적용하면, delete 로 인식한다) 를 전송하고, 삭제 여부를 확인하는 메서드
     */
    suspend fun sendAndCheckTombstone(identifier: String, topic: String) {
        try {
            log.info { "Sending tombstone message for key [$identifier] to topic $topic ===> NULL" }

            // suspend 함수 호출
            kafkaProducer.sendTombstone(topic, identifier)

            log.info { "Tombstone message for key [$identifier] sent successfully to topic $topic" }

            // Tombstone 상태 확인
            checkTombstoneState(identifier)
        } catch (ex: Exception) {
            log.error { "Failed to send tombstone for key [$identifier]: ${ex.message}" }
        }
    }


    /**
     * Tombstone 메시지를 전송한 후, 해당 데이터가 삭제되었는지 확인헌더,
     */
    suspend fun checkTombstoneState(identifier: String) {
        log.info { "Checking tombstone state for key [$identifier]" }

        // 비동기적으로 5초 대기
        delay(5000L)

        val store = getKTableStore() ?: run {
            log.error { "Failed to get KTable store. Skipping tombstone check." }
            return
        }

        val result = store[identifier]
        log.info {
            if (result == null) "Key $identifier has been tombstoned and deleted from the KTable."
            else "Key $identifier is still present in the KTable."
        }
    }

    /**
     * 처리되지 못한 KTable(newsOriginTable) 데이터를 정기적으로 삭제하는 스케쥴러
     */
    fun cleanUpScheduler() {
        cleanupJob = applicationCoroutineScope.launch {
            // 초기 지연: 1분 -> KafkaStreams 가 시작되지 않으면, newsOriginTable 을 찾을 수 없다는 오류가 발생한다.
            delay(TimeUnit.MINUTES.toMillis(1))
            while (isActive) {
                // STATE 가 Running 이 아닌 상태에서 진입할 경우 문제가 발생할 수 있다. (REBALNCING 중에는 데이터 처리가 중단될 수 있다.)
                if (summaryJoinStream.state() == KafkaStreams.State.RUNNING) {
                    log.info { "Running cleanup task for expired KTable entries" }
                    cleanupKTable()
                } else {
                    log.info { "KafkaStreams is not in RUNNING state. Skipping cleanup task....." }
                }
                delay(CLEANUP_INTERVAL_MS) // 정기 실행 주기: 1시간
            }
        }
    }

    @PreDestroy
    fun close() {
        log.info { "Shutting down Kafka Streams..." }
        summaryJoinStream.close()
        log.info { "Kafka Streams shutdown complete." }
        log.info { "Shutting down cleanup scheduler..." }
        cleanupJob.cancel()  // 스케줄러 작업 취소
        log.info { "Cleanup scheduler shutdown complete." }

    }
}