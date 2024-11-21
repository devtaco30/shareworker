package com.devtaco.shareworker.model.kafka

import mu.KotlinLogging
import org.apache.kafka.clients.consumer.Consumer
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

/**
 * author: jack
 * <p>
 * description:
 *
 * 각 KafkaListener 설정에 이 ErrorHandler 를 등록하고 사용하게 하자.
 * <p>
 */

@Component
class KafkaErrorHandler : KafkaListenerErrorHandler {

    private val log = KotlinLogging.logger {}
    override fun handleError(
        message: Message<*>,
        e: ListenerExecutionFailedException
    ) {
        log.error {
            "KafkaErrorHandler.handleError() - kafkaMessage=[${message.payload}], errorMessage=[${e.message}]"
            e.printStackTrace()
        }
    }

    override fun handleError(
        message: Message<*>,
        e: ListenerExecutionFailedException,
        consumer: Consumer<*, *>
    ) {
        val errorMessage = buildString {
            appendLine()
            appendLine("KafkaErrorHandler.handleError() - kafkaMessage=[${message.payload}]")
            consumer.subscription().forEach {
                appendLine("kafka consumer error at: $it")
            }
            appendLine("\"${e.cause!!.stackTrace[0]} : ${e.cause!!.message}\"")
        }
        log.info { errorMessage }
        e.printStackTrace()
    }


}