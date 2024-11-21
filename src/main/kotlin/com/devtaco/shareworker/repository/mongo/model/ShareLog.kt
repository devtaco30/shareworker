package com.devtaco.shareworker.repository.mongo.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "shareLogs")
data class ShareLog(
    @Id
    val id: ObjectId,
    val seq: Int,
    val action: String, // Insert, Update, Delete
    val isSuccess: Boolean, // action 공유의 성공 여부
    val createEpoch: Long, // action 공유가 행해진 시간
)