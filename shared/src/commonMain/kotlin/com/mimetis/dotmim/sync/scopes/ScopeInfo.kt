package com.mimetis.dotmim.sync.scopes

import com.mimetis.dotmim.sync.UUIDSerializer
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Mapping sur la table ScopeInfo
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
class ScopeInfo(
        /**
         * Scope name. Shared by all clients and the server
         */
        @SerialName("n")
        var name: String,

        /**
         * Id of the scope owner
         */
        @SerialName("id")
        @Serializable(with = UUIDSerializer::class)
        var id: Uuid,

        /**
         * Gets or Sets if the current provider is newly created one in database.
         * If new, we will override timestamp for first synchronisation to be sure to get all datas from server
         */
        @SerialName("in")
        var isNewScope: Boolean,

        /**
         * Gets or Sets the schema version
         */
        @SerialName("v")
        var version: String,

        /**
         * Gets or Sets the last timestamp a sync has occured. This timestamp is set just 'before' sync start.
         */
        @SerialName("lst")
        var lastSyncTimestamp: Long?,

        /**
         * Gets or Sets the last server timestamp a sync has occured for this scope client.
         */
        @SerialName("lsst")
        var lastServerSyncTimestamp: Long?,

        /**
         * Gets or Sets the last datetime when a sync has successfully ended.
         */
        @Transient
        var lastSync: Long? = null,

        /**
         * Scope schema. stored locally on the client
         */
        @Transient
        var schema: SyncSet? = null,

        /**
         * Setup. stored locally on the client
         */
        @Transient
        var setup: SyncSetup? = null,

        /**
         * Gets or Sets the last duration a sync has occured.
         */
        @Transient
        var lastSyncDuration: Long = 0
)
