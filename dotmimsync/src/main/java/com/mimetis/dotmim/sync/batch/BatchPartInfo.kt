package com.mimetis.dotmim.sync.batch

import kotlinx.serialization.*

/**
 * Info about a BatchPart
 * Will be serialized in the BatchInfo file
 */
@Serializable
class BatchPartInfo {
    @SerialName("file")
    var fileName: String = ""

    @SerialName("index")
    var index: Int = 0

    @SerialName("last")
    var isLastBatch: Boolean = false

    /**
     * Tables contained in the SyncSet (serialiazed or not) (NEW v0.9.3 : Only One table per file)
     */
    @SerialName("tables")
    var tables: List<BatchPartTableInfo>? = null

    /**
     * Tables contained rows count
     */
    @SerialName("rc")
    var rowsCount: Int = 0

    override fun toString(): String {
        if (this.tables?.isEmpty() != false)
            return super.toString()

        val table = this.tables!![0]

        return if (table.schemaName.isNullOrEmpty())
            "${table.schemaName}.${table.tableName}"
        else
            table.tableName
    }
}
