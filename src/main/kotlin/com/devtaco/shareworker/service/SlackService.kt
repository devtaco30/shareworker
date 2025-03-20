package com.devtaco.shareworker.service

import org.springframework.stereotype.Service

@Service
class SlackService {

    fun sendMessage(title: String, message: String) {
        println("SlackService.sendMessage: $title: $message")
    }

}
