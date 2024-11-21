package com.devtaco.shareworker.model.kafka

import com.devtaco.shareworker.model.worker.ShareWorker
import com.devtaco.shareworker.repository.mongo.model.ShareTarget
import com.devtaco.shareworker.repository.DataManager
import org.springframework.stereotype.Component


@Component
class WorkerInitializer(
    private val dataManager: DataManager,
) {
    fun initWorker(props: ShareTarget){
        when (props.name) {
            "condition_1" -> {
                ShareWorker.init(props, dataManager)
            }
        }
    }
}