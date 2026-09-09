package com.mimetis.dotmim.sync.web.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.ConflictResolutionPolicy
import com.mimetis.dotmim.sync.messages.DatabaseChangesApplied
import com.mimetis.dotmim.sync.messages.DatabaseChangesSelected
import com.mimetis.dotmim.sync.set.ContainerSet

@Serializable
class HttpMessageSendChangesResponse(
        @SerialName("ss")
        var serverStep: HttpStep,

        @SerialName("sc")
        var syncContext: SyncContext,

        @SerialName("bi")
        var batchIndex: Int,

        @SerialName("bc")
        var batchCount: Int?,

        @SerialName("islb")
        var isLastBatch: Boolean,

        @SerialName("rct")
        var remoteClientTimestamp: Long,

        @SerialName("changes")
        var changes: ContainerSet?,

        @SerialName("scs")
        var serverChangesSelected: DatabaseChangesSelected?,

        @SerialName("cca")
        var clientChangesApplied: DatabaseChangesApplied?,

        @SerialName("policy")
        var conflictResolutionPolicy: ConflictResolutionPolicy
)
