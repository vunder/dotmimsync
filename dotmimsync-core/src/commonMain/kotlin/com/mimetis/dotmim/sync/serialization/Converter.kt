package com.mimetis.dotmim.sync.serialization

import com.mimetis.dotmim.sync.set.SyncRow

interface Converter {
    /**
     * get the unique key for this converter
     */
    val key: String

    /**
     * Convert a row before being serialized
     */
    fun beforeSerialize(row: SyncRow)

    /**
     * Convert a row afeter being deserialized
     */
    fun afterDeserialized(row: SyncRow)
}
