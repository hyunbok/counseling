package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.port.outbound.CounselNoteProjection
import com.counseling.api.port.outbound.FeedbackProjection
import com.counseling.api.port.outbound.HistoryProjection
import com.counseling.api.port.outbound.HistoryReadRepository
import com.counseling.api.port.outbound.RecordingProjection
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class HistoryMongoRepository(
    private val mongoTemplate: ReactiveMongoTemplate,
) : HistoryReadRepository {
    override fun upsert(projection: HistoryProjection): Mono<Void> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(projection.tenantId)
                .and("channelId")
                .`is`(projection.channelId.toString())
        val update =
            Update()
                .setOnInsert("channelId", projection.channelId.toString())
                .setOnInsert("tenantId", projection.tenantId)
                .set("agentId", projection.agentId?.toString())
                .set("agentName", projection.agentName)
                .set("groupId", projection.groupId?.toString())
                .set("groupName", projection.groupName)
                .set("customerName", projection.customerName)
                .set("customerContact", projection.customerContact)
                .set("status", projection.status)
                .set("startedAt", projection.startedAt)
                .set("endedAt", projection.endedAt)
                .set("durationSeconds", projection.durationSeconds)
        return mongoTemplate
            .findAndModify(
                Query.query(criteria),
                update,
                FindAndModifyOptions.options().upsert(true),
                ChannelHistoryDocument::class.java,
                COLLECTION_NAME,
            ).then()
    }

    override fun updateRecording(
        channelId: UUID,
        tenantId: String,
        recording: RecordingProjection,
    ): Mono<Void> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("channelId")
                .`is`(channelId.toString())
        val update =
            Update()
                .set("recording.recordingId", recording.recordingId.toString())
                .set("recording.status", recording.status)
                .set("recording.filePath", recording.filePath)
                .set("recording.startedAt", recording.startedAt)
                .set("recording.stoppedAt", recording.stoppedAt)
        return mongoTemplate
            .updateFirst(
                Query.query(criteria),
                update,
                ChannelHistoryDocument::class.java,
                COLLECTION_NAME,
            ).then()
    }

    override fun updateFeedback(
        channelId: UUID,
        tenantId: String,
        feedback: FeedbackProjection,
    ): Mono<Void> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("channelId")
                .`is`(channelId.toString())
        val update =
            Update()
                .set("feedback.rating", feedback.rating)
                .set("feedback.comment", feedback.comment)
                .set("feedback.createdAt", feedback.createdAt)
        return mongoTemplate
            .updateFirst(
                Query.query(criteria),
                update,
                ChannelHistoryDocument::class.java,
                COLLECTION_NAME,
            ).then()
    }

    override fun updateCounselNote(
        channelId: UUID,
        tenantId: String,
        counselNote: CounselNoteProjection,
    ): Mono<Void> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("channelId")
                .`is`(channelId.toString())
        val update =
            Update()
                .set("counselNote.noteId", counselNote.noteId.toString())
                .set("counselNote.content", counselNote.content)
                .set("counselNote.createdAt", counselNote.createdAt)
                .set("counselNote.updatedAt", counselNote.updatedAt)
        return mongoTemplate
            .updateFirst(
                Query.query(criteria),
                update,
                ChannelHistoryDocument::class.java,
                COLLECTION_NAME,
            ).then()
    }

    override fun updateStatus(
        channelId: UUID,
        tenantId: String,
        status: String,
        endedAt: Instant?,
        durationSeconds: Long?,
    ): Mono<Void> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("channelId")
                .`is`(channelId.toString())
        val update =
            Update()
                .set("status", status)
                .set("endedAt", endedAt)
                .set("durationSeconds", durationSeconds)
        return mongoTemplate
            .updateFirst(
                Query.query(criteria),
                update,
                ChannelHistoryDocument::class.java,
                COLLECTION_NAME,
            ).then()
    }

    override fun findByTenantId(
        tenantId: String,
        agentId: UUID?,
        groupId: UUID?,
        dateFrom: Instant?,
        dateTo: Instant?,
        before: Instant?,
        limit: Int,
    ): Mono<List<HistoryProjection>> {
        var criteria = Criteria.where("tenantId").`is`(tenantId)
        if (agentId != null) {
            criteria = criteria.and("agentId").`is`(agentId.toString())
        }
        if (groupId != null) {
            criteria = criteria.and("groupId").`is`(groupId.toString())
        }
        if (dateFrom != null && dateTo != null) {
            criteria = criteria.and("startedAt").gte(dateFrom).lte(dateTo)
        } else if (dateFrom != null) {
            criteria = criteria.and("startedAt").gte(dateFrom)
        } else if (dateTo != null) {
            criteria = criteria.and("startedAt").lte(dateTo)
        }
        if (before != null) {
            criteria = criteria.and("endedAt").lt(before)
        }
        val query =
            Query
                .query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "endedAt"))
                .limit(limit)
        return mongoTemplate
            .find(query, ChannelHistoryDocument::class.java, COLLECTION_NAME)
            .map { it.toProjection() }
            .collectList()
    }

    override fun findByChannelId(
        channelId: UUID,
        tenantId: String,
    ): Mono<HistoryProjection> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("channelId")
                .`is`(channelId.toString())
        return mongoTemplate
            .findOne(Query.query(criteria), ChannelHistoryDocument::class.java, COLLECTION_NAME)
            .map { it.toProjection() }
    }

    companion object {
        private const val COLLECTION_NAME = "channel_histories"
    }
}
