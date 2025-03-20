package com.devtaco.shareworker.config


import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

/**
 * Kafka Stream 을 위한 Topic 설정
 */
@Configuration
class KafkaTopicConfig {

    @Bean
    fun shareRepartitionTopic(): NewTopic {
        return TopicBuilder.name(TOPIC_A)
            .partitions(1)
            .replicas(1)
            .build()
    }

    @Bean
    fun summaryRepartitionTopic(): NewTopic {
        return TopicBuilder.name(TOPIC_B)
            .partitions(1)
            .replicas(1)
            .build()
    }

    companion object {
        const val TOPIC_A = "topic-a"
        const val TOPIC_B = "topic-b"
    }

}