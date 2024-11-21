package com.devtaco.shareworker.model

import com.devtaco.shareworker.model.kafka.WorkerInitializer
import com.devtaco.shareworker.repository.DataManager
import com.devtaco.shareworker.repository.mongo.MongoRepository
import com.devtaco.shareworker.repository.mongo.model.ShareTarget
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class WorkerRegistration(
    private val mongoRepository: MongoRepository,
    private val dataManager: DataManager,
    val workerInitializer: WorkerInitializer,
) {

    @PostConstruct
    fun registerWorker() {
        val targets: List<ShareTarget> = mongoRepository.findAllTargets()
        for (target: ShareTarget in targets) {
            if (!target.execute) { // 상황에 따라 execute가 false인 경우도 있을 수 있다.
                continue
            }
            dataManager.findTargetApiParameter(target.name).let {
                workerInitializer.initWorker(target)
            }
        }
    }
}