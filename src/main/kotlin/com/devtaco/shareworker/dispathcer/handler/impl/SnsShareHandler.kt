package com.devtaco.shareworker.dispathcer.handler.impl

import com.devtaco.shareworker.config.GlobalConstants.OPERATION_TYPE_A
import com.devtaco.shareworker.config.GlobalConstants.OPERATION_TYPE_B
import com.devtaco.shareworker.config.GlobalConstants.OPERATION_TYPE_C
import com.devtaco.shareworker.dispathcer.handler.AbstractShareHandler
import com.devtaco.shareworker.event.kafka.payload.SnsSharePayload
import com.devtaco.shareworker.repository.DataManager
import com.devtaco.shareworker.repository.mongo.model.ShareLog
import com.devtaco.shareworker.repository.mongo.model.ShareTargetParams
import com.devtaco.shareworker.service.SlackService
import kotlinx.coroutines.*
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service-A로의 공유 작업을 처리하는 핸들러
 * 
 * 페이로드의 작업 타입(A, B, C)에 따라 적절한 처리를 수행하고
 * 처리 결과를 로그로 기록한다.
 * 
 * 주요 기능:
 * - 페이로드 유효성 검증
 * - 비동기 작업 처리
 * - 에러 처리 및 알림
 * - 작업 로그 기록
 */
@Component
class SnsShareHandler(
    private val dataManager: DataManager,
    slackService: SlackService,
) : AbstractShareHandler<SnsSharePayload>(slackService) {

    override val destination = DESTINATION
    override val payloadType = SnsSharePayload::class.java

    companion object {
        const val DESTINATION = "service-a"
    }

    /**
     * 코루틴 예외 처리기
     * 시스템 레벨에서 발생한 예외를 로깅하고 슬랙으로 알림을 보낸다
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logAndNotifyError(throwable, "SnsShareHandler Error")
    }

    /**
     * 주어진 이벤트가 이 핸들러에서 처리 가능한지 확인
     * 지원하는 작업 타입: A, B, C
     */
    override fun canHandle(event: SnsSharePayload): Boolean {
        return when (event.actionOperator) {
            OPERATION_TYPE_A, OPERATION_TYPE_B, OPERATION_TYPE_C -> true
            else -> false
        }
    }

    /**
     * 이벤트 처리의 메인 로직
     * 코루틴을 사용하여 비동기적으로 처리하며, 예외 처리와 로깅을 포함한다
     */
    override suspend fun handle(event: SnsSharePayload) {
        supervisorScope {
            launch(exceptionHandler) {
                try {
                    val result = withContext(Dispatchers.IO) {
                        sendToDestination(event)
                    }
                    saveOperationLog(result)
                } catch (e: Exception) {
                    handleFailure(event)
                    throw e
                }
            }
        }
    }

    /**
     * 페이로드를 대상 서비스로 전송
     * 작업 타입에 따라 적절한 처리 메서드를 호출한다
     */
    private suspend fun sendToDestination(event: SnsSharePayload): ShareLog {
        val serviceConfig = dataManager.findServiceConfig(DESTINATION)
        return when (event.actionOperator) {
            OPERATION_TYPE_A -> processTypeA(event, serviceConfig)
            OPERATION_TYPE_B -> processTypeB(event, serviceConfig)
            OPERATION_TYPE_C -> processTypeC(event, serviceConfig)
            else -> throw IllegalArgumentException("Invalid actionOperator: ${event.actionOperator}")
        }
    }

    /**
     * 타입 A 작업 처리를 위한 의사 구현
     * 실제 구현시에는 서비스 특화 로직이 구현될 것임
     */
    private suspend fun processTypeA(event: SnsSharePayload, apiParameter: ShareTargetParams): ShareLog {
        // Pseudo implementation for demonstration
        // Actual implementation would handle service-specific logic
        return createShareLog(event)
    }

    /**
     * 타입 B 작업 처리를 위한 의사 구현
     * 실제 구현시에는 서비스 특화 로직이 구현될 것임
     */
    private fun processTypeB(event: SnsSharePayload, apiParameter: ShareTargetParams): ShareLog {
        // Pseudo implementation for demonstration
        // Actual implementation would handle service-specific logic
        return createShareLog(event)
    }

    /**
     * 타입 C 작업 처리를 위한 의사 구현
     * 실제 구현시에는 서비스 특화 로직이 구현될 것임
     */
    private fun processTypeC(event: SnsSharePayload, apiParameter: ShareTargetParams): ShareLog {
        // Pseudo implementation for demonstration
        // Actual implementation would handle service-specific logic
        return createShareLog(event)
    }

    /**
     * 비지니스 로직 상 작업 실패 시 알림 및 로깅 처리
     */
    private fun handleFailure(event: SnsSharePayload) {
        val errorMessage = "${event.seq} content ${event.actionOperator} share failed"
        notifySlack("Service-A ${event.actionOperator} Failed", "$errorMessage -> Please check log")
        log.error { errorMessage }
    }

    /**
     * 작업 로그 생성
     * 작업의 실행 결과를 기록하기 위한 로그 객체를 생성한다
     */
    fun createShareLog(event: SnsSharePayload): ShareLog {
        return ShareLog(
            id = ObjectId.get(),
            seq = event.seq,
            action = event.actionOperator,
            isSuccess = true,
            createEpoch = Instant.now().truncatedTo(ChronoUnit.SECONDS).toEpochMilli(),
        )
    }
    
    private suspend fun saveOperationLog(log: ShareLog?) {
        log?.let { dataManager.saveLog(log) }
    }
}