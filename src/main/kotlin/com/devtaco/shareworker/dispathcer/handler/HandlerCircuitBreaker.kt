package com.devtaco.shareworker.dispathcer.handler

import java.time.Duration
import java.time.Instant



interface CircuitBreakerHandler {
    val circuitBreaker: HandlerCircuitBreaker
}

/**
 * 장애 전파 방지를 위한 CircuitBreaker
 * 연속된 실패 횟수와 차단 시간을 기반으로 요청을 제어한다
 */
class HandlerCircuitBreaker(
    private val failureThreshold: Int = 5,        // 연속 실패 허용 횟수
    private val resetTimeout: Duration = Duration.ofMinutes(1)  // 차단 후 재시도까지 대기 시간
) {
    private var failureCount = 0
    private var lastFailureTime: Instant? = null
    private var state = State.NORMAL

    enum class State {
        NORMAL,     // 정상 동작 중
        BLOCKED,    // 차단됨
        TEST_MODE   // 차단 후 첫 요청 시도
    }

    fun shouldBlock(): Boolean = when(state) {
        State.NORMAL -> false
        State.BLOCKED -> checkBlockedState()
        State.TEST_MODE -> false
    }

    fun recordSuccess() {
        failureCount = 0
        state = State.NORMAL
    }

    fun recordFailure() {
        failureCount++
        lastFailureTime = Instant.now()
        
        if (failureCount >= failureThreshold) {
            state = State.BLOCKED
        }
    }

    fun getCurrentState(): String = state.name

    private fun checkBlockedState(): Boolean {
        lastFailureTime?.let { lastFailure ->
            if (Duration.between(lastFailure, Instant.now()) > resetTimeout) {
                state = State.TEST_MODE
                return false
            }
        }
        return true
    }
}

