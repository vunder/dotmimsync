package com.mimetis.dotmim.sync.scopes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup

/**
 * Mapping sur la table ScopeInfo
 */
@Serializable
class ServerScopeInfo(
        /**
         * Scope name. Shared by all clients and the server
         */
        @SerialName("n")
        var name: String,

        /**
         * Setup. stored locally
         */
        @SerialName("s")
        var setup: SyncSetup,

        /**
         * Gets or Sets the schema version
         */
        @SerialName("v")
        var version: String? = null,

        /**
         * Gets or Sets the last timestamp a sync has occured. This timestamp is set just 'before' sync start.
         */
        @SerialName("lst")
        var lastCleanupTimestamp: Long = 0,

        /**
         * Scope schema. stored locally
         */
        @Transient
        var schema: SyncSet? = null
)
