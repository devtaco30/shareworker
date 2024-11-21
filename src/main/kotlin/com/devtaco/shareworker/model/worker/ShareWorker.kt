package com.devtaco.shareworker.model.worker

import com.devtaco.shareworker.model.kafka.payload.SnsSharePayload
import com.devtaco.shareworker.repository.mongo.model.ShareLog
import com.devtaco.shareworker.repository.mongo.model.ShareTarget
import com.devtaco.shareworker.repository.DataManager
import kotlinx.coroutines.*
import org.bson.types.ObjectId
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

object ShareWorker : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job // Worker가 Default 스레드에서 실행됨

    // 초기화가 완료되었는지 확인하기 위한 AtomicBoolean
    private val isInitialized = AtomicBoolean(false)

    private lateinit var target: ShareTarget
    private lateinit var dataManager: DataManager


    fun init(
        target: ShareTarget,
        dataManager: DataManager,
    ) {
        // 이미 초기화가 진행되었다면 함수를 빠져나옴
        if (isInitialized.get()) {
            return
        }

        ShareWorker.target = target
        ShareWorker.dataManager = dataManager

        isInitialized.set(true)
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        // TODO: 에러 처리
    }

    fun process(payload: SnsSharePayload) {
        if (!isInitialized.get()) {
            return
        }
        launch {
            handlePayload(payload)
        }
    }

    // 클래스 종료 시 리소스 정리
    fun cleanUp() {
        job.cancel()
        runBlocking { job.join() }
    }

    private suspend fun handlePayload(payload: SnsSharePayload) {
        supervisorScope {
            launch(exceptionHandler) {
                val log = handleAction(payload)
                saveOperationLog(log)
            }
        }
    }

    private fun handleAction(payload: SnsSharePayload): ShareLog? {
        return when (payload.actionOperator) {
            "I" -> {
                // TODO: something to do for INSERT
                createShareLog(payload, true)
            }

            "U" -> {
                val latestLog =
                    dataManager.findLatestShareLogs(payload.seq, target.name)

                // TODO: something to do for UPDATE
                createShareLog(payload, true)
            }
            else -> null
        }
    }

    private fun createShareLog(
        payload: SnsSharePayload,
        isSuccess: Boolean,
    ): ShareLog {
        return ShareLog(
            id = ObjectId(),
            seq = payload.seq,
            action = payload.actionOperator, //
            isSuccess = isSuccess,
            createEpoch = Instant.now().truncatedTo(ChronoUnit.SECONDS).toEpochMilli(),
        )
    }

    private suspend fun saveOperationLog(log: ShareLog?) {
        log?.let { dataManager.saveLog(it) }
    }
}
