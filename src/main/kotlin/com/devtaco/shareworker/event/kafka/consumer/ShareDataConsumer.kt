package com.devtaco.shareworker.event.kafka.consumer

import com.devtaco.shareworker.config.KafkaTopicConfig.Companion.TOPIC_A
import com.devtaco.shareworker.config.KafkaTopicConfig.Companion.TOPIC_B
import com.devtaco.shareworker.event.kafka.payload.Payload
import com.devtaco.shareworker.event.kafka.payload.ShareDataPayload
import com.devtaco.shareworker.event.kafka.payload.SummaryCompletePayload
import com.devtaco.shareworker.event.kafka.producer.KafkaProducer
import com.devtaco.shareworker.event.channel.PayloadChannel
import com.devtaco.shareworker.utils.MoshiBuilder
import com.squareup.moshi.Moshi
import jakarta.validation.Validation
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class ShareDataConsumer(
    private val payloadChannel: PayloadChannel<Payload>,
    private val kafkaProducer: KafkaProducer
) {

    private val validator = Validation.buildDefaultValidatorFactory().validator
    private val moshi: Moshi = MoshiBuilder.moshi().newBuilder().build()

    @KafkaListener(topics = [SHARE_TOPIC], errorHandler = "kafkaErrorHandler")
    suspend fun receive(payload: String) {
        val shareDataPayload = parseAndValidate<ShareDataPayload>(payload)
        payloadChannel.sendPayload(shareDataPayload)
        kafkaProducer.produce(TOPIC_A, shareDataPayload.identifier, payload)
    }

    @KafkaListener(topics = [SUMMARY_COMPLETE], errorHandler = "kafkaErrorHandler")
    suspend fun joinSummaryComplete(payload: String) {
        val summaryCompletePayload = parseAndValidate<SummaryCompletePayload>(payload)
        kafkaProducer.produce(TOPIC_B, summaryCompletePayload.identifier, payload)
    }

    private inline fun <reified T> parseAndValidate(payload: String): T {
        val parsedPayload = moshi.adapter(T::class.java).fromJson(payload)
        validator.validate(parsedPayload).takeIf { it.isNotEmpty() }?.let {
            throw IllegalArgumentException("Invalid payload")
        }
        return parsedPayload!!
    }

    companion object {
        const val SHARE_TOPIC = "share-topic"
        const val SUMMARY_COMPLETE = "summary-complete-topic"
    }


}
