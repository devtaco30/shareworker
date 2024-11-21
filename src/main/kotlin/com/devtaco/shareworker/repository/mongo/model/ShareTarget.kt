package com.devtaco.shareworker.repository.mongo.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "shareTargets")
data class ShareTarget(
    @Id
    val id: ObjectId,
    val seq: Long,
    val name: String,
    val execute: Boolean,
)

