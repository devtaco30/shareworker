package com.devtaco.shareworker.repository

import com.devtaco.shareworker.repository.mongo.MongoRepository
import com.devtaco.shareworker.repository.mongo.model.ShareLog
import com.devtaco.shareworker.repository.mongo.model.ShareTargetParams
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service

@Service
class DataManager(
    private val mongoRepository: MongoRepository
) {

    private val sharedMutex = Mutex()

    fun findTargetApiParameter(target: String): ShareTargetParams =
        mongoRepository.getShareTargetParams(target)

    fun findLatestShareLogs(shareDataId: Int, target: String) =
        mongoRepository.findLatestShareLogs(shareDataId, target)

    suspend fun saveLog(shareLog: ShareLog) {
        sharedMutex.withLock {
            mongoRepository.saveLog(shareLog)
        }
    }

}