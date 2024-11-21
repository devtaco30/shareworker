package com.devtaco.shareworker.model.kafka.payload

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotNull

@JsonIgnoreProperties(ignoreUnknown = true)
data class SummaryCompletePayload(
    @field:NotNull
    val identifier: String,

    @field:NotNull
    val summaryList: List<String>,
    ) : Payload()