package com.devtaco.shareworker.dispathcer.handler

import com.devtaco.shareworker.event.kafka.payload.Payload
import com.devtaco.shareworker.service.SlackService
import mu.KotlinLogging

/**
 * ShareHandler 인터페이스의 기본 구현을 제공하는 추상 클래스
 * 
 * 모든 핸들러에서 공통적으로 사용되는 기능들을 제공한다:
 * - 슬랙을 통한 알림 발송
 * - 에러 로깅 및 알림
 * - 로깅 유틸리티
 *
 * @param T 처리할 페이로드의 타입
 * @property slackService 슬랙 알림 서비스
 */
abstract class AbstractShareHandler<T: Payload>(
    private val slackService: SlackService
): ShareHandler<T> {

    protected val log = KotlinLogging.logger {}

    /**
     * 슬랙으로 메시지를 전송하는 유틸리티 메서드
     * 
     * @param title 메시지 제목
     * @param message 메시지 내용
     */
    protected fun notifySlack(title: String, message: String) {
        slackService.sendMessage(title, message)
    }

    /**
     * 에러 발생 시 로깅과 슬랙 알림을 함께 처리하는 유틸리티 메서드
     * 
     * @param throwable 발생한 예외
     * @param errorMessage 에러 메시지
     */
    protected fun logAndNotifyError(throwable: Throwable, errorMessage: String) {
        notifySlack("Error Notification", errorMessage)
        log.error("Error: $errorMessage", throwable)
    }
}