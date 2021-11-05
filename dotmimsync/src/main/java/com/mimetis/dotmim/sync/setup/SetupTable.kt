package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.enumerations.SyncDirection
import com.mimetis.dotmim.sync.set.SyncNamedItem

@Serializable
class SetupTable(
        /**
         *  Gets or Sets the table name
         */
        @SerialName("tn")
        var tableName: String,

        /**
         *  Gets or Sets the schema name
         */
        @SerialName("sn")
        var schemaName: String = "",

        /**
         * Gets or Sets the table columns collection
         */
        @SerialName("cols")
        var columns: SetupColumns,

        /**
         * Gets or Sets the Sync direction (may be Bidirectional, DownloadOnly, UploadOnly)
         * Default is @see SyncDirection.Bidirectional
         */
        @SerialName("sd")
        var syncDirection: SyncDirection = SyncDirection.Bidirectional
) : SyncNamedItem<SetupFilter>() {
    override fun getAllNamesProperties(): List<String> =
            listOf(this.tableName, this.schemaName)
}
