package com.devtaco.shareworker.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.coroutines.EmptyCoroutineContext

@Configuration
class CoroutineScopeConfig {

    @Bean
    fun applicationCoroutineScope(): CoroutineScope {
        return CoroutineScope(EmptyCoroutineContext + SupervisorJob())
    }
}
