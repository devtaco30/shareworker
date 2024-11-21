package com.devtaco.shareworker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.kafka.annotation.EnableKafka
import java.util.*

@EnableKafka
@EnableMongoRepositories(basePackages = ["com.devtaco.shareworker"])
@ConfigurationPropertiesScan
@SpringBootApplication
class ShareWorkerApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC")) // time zone utc 로...
    runApplication<ShareWorkerApplication>(*args)
}