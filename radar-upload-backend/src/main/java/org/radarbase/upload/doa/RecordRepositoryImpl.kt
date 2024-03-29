package org.radarbase.upload.doa

import org.hibernate.Hibernate
import org.hibernate.Session
import org.radarbase.upload.doa.entity.*
import org.radarbase.upload.inject.session
import org.radarbase.upload.inject.transact
import java.io.InputStream
import java.io.Reader
import java.time.Instant
import javax.persistence.EntityManager
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Context
import kotlin.streams.toList


class RecordRepositoryImpl(@Context private var em: EntityManager) : RecordRepository {
    override fun updateLogs(id: Long, reader: Reader, length: Long): Unit = em.transact {
        val logs = find(RecordLogs::class.java, id)
        val metadata = if (logs == null) {
            val metadata = find(RecordMetadata::class.java, id)  ?: throw NotFoundException("RecordMetadata with ID $id does not exist")
            metadata.logs = RecordLogs().apply {
                this.id = id
                this.metadata = metadata
                this.modifiedDate = Instant.now()
                this.size = length
                this.logs = Hibernate.getLobCreator(em.session).createClob(reader, length)
            }
            persist(metadata.logs!!)
            metadata
        } else {
            logs.apply {
                this.modifiedDate = Instant.now()
                this.logs = Hibernate.getLobCreator(em.session).createClob(reader, length)
            }
            merge(logs)
            logs.metadata
        }

        modifyNow(metadata)
    }

    override fun query(limit: Int, lastId: Long, projectId: String, userId: String?, status: String?): List<Record> {
        var queryString = "SELECT r FROM Record r WHERE r.projectId = :projectId AND r.id > :lastId"
        userId?.let {
            queryString += " AND r.userId = :userId"
        }
        status?.let {
            queryString += " AND r.metadata.status = :status"
        }
        queryString += " ORDER BY r.id"

        return em.transact {
            val query = createQuery(queryString, Record::class.java)
                    .setParameter("lastId", lastId)
                    .setParameter("projectId", projectId)
                    .setMaxResults(limit)
            userId?.let {
                query.setParameter("userId", it)
            }
            status?.let {
                query.setParameter("status", it)
            }
            query.resultList
        }
    }

    override fun readLogs(id: Long): RecordLogs? = em.transact {
        find(RecordLogs::class.java, id)
    }

    override fun updateContent(record: Record, fileName: String, contentType: String, stream: InputStream, length: Long): RecordContent = em.transact {
        val existingContent = record.contents?.find { it.fileName == fileName }

        val result = if (existingContent == null) {
            val newContent = RecordContent().apply {
                this.record = record
                this.fileName = fileName
                this.createdDate = Instant.now()
                this.contentType = contentType
                this.size = length
                this.content = Hibernate.getLobCreator(em.unwrap(Session::class.java)).createBlob(stream, length)
            }
            record.contents = (record.contents ?: mutableSetOf()).apply { add(newContent) }

            persist(newContent)
            merge(record)
            newContent
        } else {
            existingContent.apply {
                this.createdDate = Instant.now()
                this.contentType = contentType
                this.content = Hibernate.getLobCreator(em.unwrap(Session::class.java)).createBlob(stream, length)
            }
            merge(existingContent)
            existingContent
        }
        modifyNow(record.metadata)
        return@transact result
    }

    override fun poll(limit: Int): List<Record> = em.transact {
        createQuery("SELECT r FROM Record r WHERE r.metadata.status = :status ORDER BY r.metadata.modifiedDate", Record::class.java)
                .setParameter("status", "READY")
                .setMaxResults(limit)
                .resultStream
                .peek {
                    it.metadata.status = RecordStatus.QUEUED
                    modifyNow(it.metadata)
                }
                .toList()
    }

    override fun readContent(id: Long): RecordContent? = em.transact {
        find(RecordContent::class.java, id)
    }

    override fun create(record: Record): Record = em.transact {
        val tmpContent = record.contents?.also { record.contents = null }

        persist(record)

        record.contents = tmpContent?.mapTo(HashSet()) {
            it.record = record
            it.createdDate = Instant.now()
            persist(it)
            it
        }

        val metadata = RecordMetadata().apply {
            this.record = record
            status = if (tmpContent == null) RecordStatus.INCOMPLETE else RecordStatus.READY
            message = if (tmpContent == null) "No data uploaded yet" else "Data successfully uploaded, ready for processing."
            createdDate = Instant.now()
            modifiedDate = Instant.now()
            revision = 1
        }

        persist(metadata)

        record.apply {
            this.contents = contents
            this.metadata = metadata
        }
    }

    override fun read(id: Long): Record? = em.transact { find(Record::class.java, id) }

    override fun update(record: Record): Record = em.transact {
        modifyNow(record.metadata)
        merge<Record>(record)
    }

    private fun modifyNow(metadata: RecordMetadata): RecordMetadata {
        metadata.revision += 1
        metadata.modifiedDate = Instant.now()
        return em.merge<RecordMetadata>(metadata)
    }

    override fun update(metadata: RecordMetadata): RecordMetadata = em.transact { modifyNow(metadata) }

    override fun delete(record: Record) = em.transact { remove(record) }

    override fun readMetadata(id: Long): RecordMetadata? = em.transact { find(RecordMetadata::class.java, id) }
}
