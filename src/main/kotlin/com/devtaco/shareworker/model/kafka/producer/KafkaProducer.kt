package com.devtaco.shareworker.model.kafka.producer

import kotlinx.coroutines.future.await
import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {

    private val log = KotlinLogging.logger {}

    suspend fun produce(topic: String, key: String, jsonString: String?) {
        try {
            val result = kafkaTemplate.send(topic, key, jsonString).await() // 코루틴 방식으로 대기
            log.info {
                "Message sent successfully to $topic, " +
                        "partition=${result.recordMetadata.partition()}, offset=${result.recordMetadata.offset()}"
            }
        } catch (ex: Exception) {
            log.error { "Failed to send message to $topic: ${ex.message}" }
        }
    }

    suspend fun sendTombstone(topic: String, key: String) {
        try {
            kafkaTemplate.send(topic, key, null).await()
            log.info { "Tombstone message for key [$key] sent successfully to topic $topic ===> NULL" }
        } catch (ex: Exception) {
            log.error { "Failed to send tombstone message to $topic: ${ex.message}" }
        }
    }
}