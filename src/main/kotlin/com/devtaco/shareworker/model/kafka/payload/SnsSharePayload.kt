package com.devtaco.shareworker.model.kafka.payload

import com.devtaco.shareworker.model.kafka.stream.JoinStreamHandler
import jakarta.validation.constraints.NotNull

/**
 * [ShareDataPayload] 와 [SummaryCompletePayload] 를 join 으로 생성한다.
 *
 * join 은 [JoinStreamHandler] 에서 처리한다.
 *
 */
data class SnsSharePayload(
    @field:NotNull
    val identifier: String,
    @field:NotNull
    val seq: Int,
    @field:NotNull
    val title: String,
    @field:NotNull
    val content: String,
    @field:NotNull
    val actionOperator: String,
    @field:NotNull
    val summaryList: List<String>
) : Payload()
