package com.devtaco.shareworker.model

import com.devtaco.shareworker.model.kafka.payload.Payload
import com.devtaco.shareworker.model.kafka.payload.SnsSharePayload
import com.devtaco.shareworker.model.worker.ShareWorker
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PayloadDistributor(
    @Autowired private val payloadChannel: PayloadChannel<Payload>,
    @Autowired private val coroutineScope: CoroutineScope,
) {

    private val log = KotlinLogging.logger {}

    private val workers: List<Pair<(SnsSharePayload) -> Boolean, (SnsSharePayload) -> Unit>> =
        listOf(
            Pair({ _ -> true }, ShareWorker::process),
        )

    @PostConstruct
    fun onCreate() {
        coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
            log.error("Coroutine encountered an exception", throwable)
        }) {
            while (isActive) {
                shareData()
            }
        }
    }

    private suspend fun shareData() {
        coroutineScope {
            try {
                when (val payload = payloadChannel.receivePayload()) {
                    is SnsSharePayload -> {
                        // 기본 조건에 맞는 모든 worker 실행
                        workers.forEach { (condition, worker) ->
                            if (condition(payload)) {
                                launch { worker(payload) }
                            }
                        }
                    }
                    else -> log.error { "Payload is not an instance of target Payload" }
                }
            } catch (e: Exception) {
                log.error { "Error processing payload: ${e.message}" }
            }
        }
    }

}
