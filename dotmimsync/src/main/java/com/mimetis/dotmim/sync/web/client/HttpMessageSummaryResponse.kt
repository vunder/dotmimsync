package com.mimetis.dotmim.sync.web.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.enumerations.ConflictResolutionPolicy
import com.mimetis.dotmim.sync.messages.DatabaseChangesApplied
import com.mimetis.dotmim.sync.messages.DatabaseChangesSelected
import com.mimetis.dotmim.sync.set.ContainerSet

@Serializable
class HttpMessageSummaryResponse(
        /**
         * Gets or Sets the SyncContext
         */
        @SerialName("sc")
        var syncContext: SyncContext,

        /**
         * Gets or Sets the conflict resolution policy from the server
         */
        @SerialName("bi")
        var batchInfo: BatchInfo? = null,

        /**
         * The remote client timestamp generated by the server database
         */
        @SerialName("rct")
        var remoteClientTimestamp: Long = 0,

        @SerialName("step")
        var step: HttpStep,

        /**
         * Gets or Sets the container changes when in memory requested by the client
         */
        @SerialName("changes")
        var changes: ContainerSet? = null,

        @SerialName("scs")
        var serverChangesSelected: DatabaseChangesSelected,

        @SerialName("cca")
        var clientChangesApplied: DatabaseChangesApplied,

        @SerialName("crp")
        var conflictResolutionPolicy: ConflictResolutionPolicy = ConflictResolutionPolicy.values()[0]
)
