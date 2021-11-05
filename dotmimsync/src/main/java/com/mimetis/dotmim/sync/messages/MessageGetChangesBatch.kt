package com.mimetis.dotmim.sync.messages

import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.util.*

/**
 * Message exchanged during the Get Changes Batch sync stage
 */
class MessageGetChangesBatch(
        /**
         * Gets or Sets the Scope Id that should be excluded when we get lines from the local store
         * Usable only from Server side
         */
        var excludingScopeId: UUID? = null,

        /**
         * Gets or Sets the local Scope Id that will replace <NULL> values when creating the row
         */
        var localScopeId: UUID,

        /**
         * Gets or Sets if the sync is a first sync. In this case, the last sync timestamp is ignored
         */
        var isNew: Boolean,

        /**
         * Gets or Sets the last date timestamp from where we want rows
         */
        var lastTimestamp: Long?,

        /**
         * Gets or Sets the schema used for this sync
         */
        var schema: SyncSet,

        /**
         * Gets or Sets the Setup used for this sync
         */
        var setup: SyncSetup,

        /**
         * Gets or Sets the download batch size, if needed
         */
        var batchSize: Int,

        /**
         * Gets or Sets the batch directory used to serialize the datas
         */
        var batchDirectory: String
)
