package com.devtaco.shareworker.repository.mongo.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "shareTargetParams")
data class ShareTargetParams(
    @Id
    val id: ObjectId,
    val target: String,
    val apiToken: String,
    val execute: Boolean,
)