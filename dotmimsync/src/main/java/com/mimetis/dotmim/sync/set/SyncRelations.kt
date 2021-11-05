package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

@Serializable(with = SyncRelationsSerializer::class)
class SyncRelations() : ArrayList<SyncRelation>() {
    /**
     * Relation's schema
     */
    @Transient
    var schema: SyncSet? = null

    constructor(schema: SyncSet) : this() {
        this.schema = schema
    }

    /**
     * Since we don't serializer the reference to the schema, this method will reaffect the correct schema
     */
    fun ensureRelations(schema: SyncSet) {
        this.schema = schema
        this.forEach { it.ensureRelation(schema) }
    }

    override fun clear() {
        this.forEach { it.clear() }
        super.clear()
    }
}

object SyncRelationsSerializer : ArrayListLikeSerializer<SyncRelations, SyncRelation>(SyncRelation.serializer())
