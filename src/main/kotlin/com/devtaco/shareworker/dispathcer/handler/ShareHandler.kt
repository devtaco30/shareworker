package com.devtaco.shareworker.dispathcer.handler

import com.devtaco.shareworker.event.kafka.payload.Payload

/**
 * 공유 작업을 처리하는 핸들러의 기본 인터페이스
 * 
 * 각각의 핸들러는 특정 타입의 페이로드를 처리하며,
 * 해당 페이로드를 지정된 대상(destination)으로 전송하는 역할을 수행한다.
 */
interface ShareHandler<T : Payload> {

    /**
     * 핸들러가 담당하는 대상 서비스의 식별자
     */
    val destination: String

    /**
     * 핸들러가 처리할 수 있는 페이로드의 타입
     * 제네릭 타입의 구체적인 클래스를 지정하여 타입 안전성을 보장한다
     */
    val payloadType: Class<T>

    /**
     * 주어진 이벤트를 이 핸들러가 처리할 수 있는지 확인
     * 
     * @param event 처리 여부를 확인할 페이로드
     * @return 처리 가능 여부
     */
    fun canHandle(event: T): Boolean

    /**
     * 실제 이벤트 처리를 수행하는 메서드
     * 코루틴을 사용하여 비동기적으로 처리한다
     * 
     * @param event 처리할 페이로드
     */
    suspend fun handle(event: T)
}