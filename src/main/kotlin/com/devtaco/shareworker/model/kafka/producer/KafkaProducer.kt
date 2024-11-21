package com.devtaco.shareworker.model.kafka.producer

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class KafkaProducer(
    @Autowired
    private val kafkaTemplate: KafkaTemplate<String, String>?,
) {

    private val log = KotlinLogging.logger {}

    fun produce(topic: String, key: String, jsonString: String?) {
        val future: CompletableFuture<SendResult<String, String>> =
            kafkaTemplate!!.send(topic, key, jsonString).toCompletableFuture()
        future.thenAccept { result ->
            log.info { "Message sent successfully to $topic, partition=${result.recordMetadata.partition()}, offset=${result.recordMetadata.offset()}" }
        }.exceptionally { ex ->
            log.error { "Failed to send message to $topic: ${ex.message}" }
            null
        }
    }

    // tombstone 전송 메서드
    fun sendTombstone(topic: String, key: String): CompletableFuture<SendResult<String, String>> {
        log.info { "Sending tombstone message for key [$key] to topic $topic ===> NULL" }
        return kafkaTemplate!!.send(topic, key, null).toCompletableFuture()
    }
}