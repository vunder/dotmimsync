package com.mimetis.dotmim.sync

import com.mimetis.dotmim.sync.enumerations.SyncStage
import com.mimetis.dotmim.sync.enumerations.SyncType
import com.mimetis.dotmim.sync.enumerations.SyncWay
import com.mimetis.dotmim.sync.parameter.SyncParameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
class SyncContext(
    @SerialName("id")
    @Serializable(with = UuidSerializer::class)
    var sessionId: Uuid,

    @SerialName("sn")
    var scopeName: String,

    @SerialName("csid")
    @Serializable(with = UuidSerializer::class)
    var clientScopeId: Uuid? = null,

    @SerialName("typ")
    var syncType: SyncType = SyncType.values()[0],

    @SerialName("way")
    var syncWay: SyncWay = SyncWay.values()[0],

    @SerialName("stage")
    var syncStage: SyncStage = SyncStage.values()[0],

    @SerialName("ps")
    var parameters: SyncParameters? = null,

    @SerialName("ap")
    var additionalProperties: Map<String, String>? = null,

    @SerialName("pp")
    var progressPercentage: Double = 0.0
) {

    fun copyTo(otherSyncContext: SyncContext) {
        otherSyncContext.parameters = this.parameters
        otherSyncContext.scopeName = this.scopeName
        otherSyncContext.sessionId = this.sessionId
        otherSyncContext.syncStage = this.syncStage
        otherSyncContext.syncType = this.syncType
        otherSyncContext.syncWay = this.syncWay
        otherSyncContext.progressPercentage = this.progressPercentage

        if (this.additionalProperties != null) {
            otherSyncContext.additionalProperties = hashMapOf<String, String>().apply {
                this@SyncContext.additionalProperties!!.forEach { put(it.key, it.value) }
            }
        }
    }
}
