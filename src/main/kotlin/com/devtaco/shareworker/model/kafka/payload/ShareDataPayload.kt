package com.devtaco.shareworker.model.kafka.payload

import jakarta.validation.constraints.NotNull


data class ShareDataPayload(
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
    ) : Payload() {

        /** Kafka Stream 에서 Join 하는 경우 사용하는 값.
         * 이 시점을 기준으로 일정 시간이 경과되도록 처리되지 못하면 제거하기 위함. */
    val streamRegisterEpoch: Long = System.currentTimeMillis()
}