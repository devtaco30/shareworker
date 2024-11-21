package com.devtaco.shareworker.model

import com.devtaco.shareworker.model.kafka.payload.Payload
import kotlinx.coroutines.channels.Channel
import org.springframework.stereotype.Component

/**
 *
 * Kafka 에서 받은 Payload 를 순서대로 처리하기 위해 받는 채널
 *
 * [PayloadDistributor]가 receive 해서, 각 HandleService 로 넘기고, 내부의 handler 가 처리하게 한다.
 *
 */
@Component
class PayloadChannel<T : Payload> {

    private val pushPayloadChannel: Channel<T> = Channel(20)
    suspend fun sendPayload(payload: T) {
        this.pushPayloadChannel.send(payload)
    }

    suspend fun receivePayload(): Payload {
        return pushPayloadChannel.receive()
    }

}