package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncConflict
import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.ConflictResolution
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncRow
import java.util.*

/**
 * Raised as an argument when an apply is failing. Waiting from user for the conflict resolution
 */
class ApplyChangesFailedArgs(
    context: SyncContext,

    /**
     * Gets the object that contains data and metadata for the row being applied and for the existing row in the database that caused the failure.
     */
    val conflict: SyncConflict,

    resolution: ConflictResolution,

    /**
     * Gets or Sets the scope id who will be marked as winner
     */
    val senderScopeId: UUID?
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Information

    private var _resolution: ConflictResolution = resolution

    /**
     * Gets or Sets the action to be taken when resolving the conflict.
     * If you choose MergeRow, you have to fill the FinalRow property
     */
    var resolution: ConflictResolution
        get() = _resolution
        set(value) {
            if (_resolution != value) {
                _resolution = value

                if (this._resolution == ConflictResolution.MergeRow) {
                    val finalRowArray = this.conflict.remoteRow.toArray()
                    val finalTable = this.conflict.remoteRow.schemaTable.clone()
                    val finalSet = this.conflict.remoteRow.schemaTable.schema?.clone(false)
                    finalSet?.tables?.add(finalTable)
                    this.finalRow = SyncRow(this.conflict.remoteRow.schemaTable, finalRowArray)
                    finalTable.rows.add(this.finalRow)
                } else if (::finalRow.isInitialized) {
                    val finalSet = this.finalRow.schemaTable.schema
                    this.finalRow.clear()
                    finalSet?.clear()
                    finalSet?.close()
                }
            }
        }

    /**
     * If we have a merge action, the final row represents the merged row
     */
    lateinit var finalRow: SyncRow

    override val message: String =
        "Conflict ${conflict.type}"

    override val eventId: Int =
        300
}

/**
 * Intercept the provider when an apply change is failing
 */
fun BaseOrchestrator.onApplyChangesFailed(action: (ApplyChangesFailedArgs) -> Unit) =
    this.setInterceptor(action)
