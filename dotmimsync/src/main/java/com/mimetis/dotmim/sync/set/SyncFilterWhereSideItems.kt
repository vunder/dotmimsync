package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

@Serializable(with = SyncFilterWhereSideItemsSerializer::class)
class SyncFilterWhereSideItems : ArrayList<SyncFilterWhereSideItem>() {
    /**
     * Filter's schema
     */
    @Transient
    var schema: SyncSet? = null

    /**
     * Since we don't serializer the reference to the schema, this method will reaffect the correct schema
     */
    fun ensureFilters(schema: SyncSet) {
        this.schema = schema

        this.forEach { it.ensureFilterWhereSideItem(schema)}
    }
}

object SyncFilterWhereSideItemsSerializer : ArrayListLikeSerializer<SyncFilterWhereSideItems, SyncFilterWhereSideItem>(SyncFilterWhereSideItem.serializer())
