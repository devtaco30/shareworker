package com.devtaco.shareworker.dispathcer

import com.devtaco.shareworker.dispathcer.handler.ShareHandler
import com.devtaco.shareworker.event.channel.PayloadChannel
import com.devtaco.shareworker.event.kafka.payload.Payload
import com.devtaco.shareworker.event.kafka.payload.ShareDataPayload
import com.devtaco.shareworker.event.kafka.payload.SnsSharePayload
import com.devtaco.shareworker.repository.DataManager
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.stereotype.Service

/**
 * 페이로드를 적절한 핸들러로 분배하는 디스패처
 * 
 * 시스템에 등록된 모든 핸들러들을 관리하고,
 * 각 페이로드를 처리할 수 있는 적절한 핸들러에게 작업을 분배한다.
 * 
 * 주요 기능:
 * - 핸들러 활성화 상태 관리
 * - 페이로드 타입에 따른 핸들러 매칭
 * - 비동기 작업 처리
 * - 에러 처리 및 로깅
 */
@Service
class DestinationDispatcher(
    private val handlers: List<ShareHandler<*>>,
    private val payloadChannel: PayloadChannel<Payload>,
    private val coroutineScope: CoroutineScope,
    private val dataManager: DataManager
) {
    private val log = KotlinLogging.logger {}
    
    /**
     * 활성화된 핸들러 목록
     * Pair의 첫 번째 요소는 핸들러, 두 번째 요소는 활성화 여부
     */
    private lateinit var activeHandlers: List<Pair<ShareHandler<*>, Boolean>>

    /**
     * 디스패처 초기화
     * 설정에 따라 각 핸들러의 활성화 상태를 결정하고
     * 페이로드 처리를 위한 코루틴을 시작한다
     */
    @PostConstruct
    fun onCreate() {
        activeHandlers = handlers.map { handler ->
            val contractor = dataManager.findServiceConfig(handler.destination)
            val isActive = contractor?.execute == true
            handler to isActive
        }

        coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
            log.error("Coroutine encountered an exception", throwable)
        }) {
            while (isActive) {
                processPayloads()
            }
        }
    }

    /**
     * 페이로드 처리 메인 로직
     * 
     * 1. 페이로드 채널에서 메시지를 수신
     * 2. 활성화된 핸들러 중 해당 페이로드를 처리할 수 있는 핸들러를 찾음
     * 3. 적절한 핸들러에게 작업을 위임
     */
    private suspend fun processPayloads() {
        val payload = payloadChannel.receivePayload()
        
        coroutineScope {
            activeHandlers.forEach { (handler, isActive) ->
                if (isActive) {
                    when (payload) {
                        is ShareDataPayload -> {
                            if (handler.payloadType == ShareDataPayload::class.java) {
                                @Suppress("UNCHECKED_CAST")
                                val newsHandler = handler as ShareHandler<ShareDataPayload>
                                if (newsHandler.canHandle(payload)) {
                                    newsHandler.handle(payload)
                                }
                            }
                        }

                        is SnsSharePayload -> {
                            if (handler.payloadType == SnsSharePayload::class.java) {
                                @Suppress("UNCHECKED_CAST")
                                val newsHandler = handler as ShareHandler<SnsSharePayload>
                                if (newsHandler.canHandle(payload)) {
                                    newsHandler.handle(payload)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}