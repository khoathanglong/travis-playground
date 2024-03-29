package org.radarbase.upload.doa.entity

import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "record_metadata")
class RecordMetadata {
    @Id
    @Column(name = "record_id")
    var id: Long? = null

    @JoinColumn(name = "record_id")
    @OneToOne(cascade = [CascadeType.ALL])
    @MapsId
    lateinit var record: Record

    var revision: Int = 0

    @Enumerated(EnumType.STRING)
    lateinit var status: RecordStatus
    var message: String? = null

    @Column(name = "created_date")
    lateinit var createdDate: Instant

    @Column(name = "modified_date")
    lateinit var modifiedDate: Instant

    @Column(name = "committed_date")
    var committedDate: Instant? = null

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "metadata", cascade = [CascadeType.ALL])
    var logs: RecordLogs? = null


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordMetadata

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
