package com.devtaco.shareworker.repository.mongo

import com.devtaco.shareworker.repository.mongo.model.ShareLog
import com.devtaco.shareworker.repository.mongo.model.ShareTarget
import com.devtaco.shareworker.repository.mongo.model.ShareTargetParams
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class MongoRepository(
    private val mongoTemplate: MongoTemplate,
) {

    companion object {
        private const val TARGETS = "shareTargets" // 기사 제공을 계약한 계약사 정보 컬렉션
        private const val SHARE_LOG = "shareLogs"
        private const val SNS_API_TOKEN = "tokens"
    }

    fun findAllTargets(): List<ShareTarget> {
        return mongoTemplate.findAll(ShareTarget::class.java, TARGETS)
    }

    fun saveLog(log: ShareLog) {
        mongoTemplate.save(log, SHARE_LOG)
    }

    fun findLatestShareLogs(seq: Int, contractor: String): ShareLog? {
        val query = Query()
        query.addCriteria(ShareLog::seq isEqualTo seq)
        query.with(Sort.by(Sort.Direction.DESC, "seq"))
        query.limit(1)
        return mongoTemplate.findOne(query, ShareLog::class.java, SHARE_LOG)
    }

    fun getShareTargetParams(target: String): ShareTargetParams {
        val query = Query()
        query.addCriteria(Criteria.where("target").`is`(target))
        val result = mongoTemplate.findOne(query, ShareTargetParams::class.java, SNS_API_TOKEN)
        return result
            ?: throw NoSuchElementException("No document found for providerId: $target")
    }
}