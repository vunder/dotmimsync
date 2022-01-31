package com.mimetis.dotmim.sync

import com.mimetis.dotmim.sync.enumerations.ConflictType
import com.mimetis.dotmim.sync.set.SyncRow

/**
 * Represents a synchronization conflict at the row level.
 * Conflict rule resolution is set on the server side
 */
class SyncConflict() {
    constructor(type: ConflictType) : this() {
        this.type = type
    }

    /**
     * Gets or sets the error message that is returned when a conflict is set to ConflictType.ErrorsOccurred
     */
    var errorMessage: String = ""

    /**
     * Gets the row that contains the conflicting row from the local database.
     */
    lateinit var localRow: SyncRow

    /**
     * Gets the row that contains the conflicting row from the remote database.
     */
    lateinit var remoteRow: SyncRow

    /**
     * Initializes a new instance of the SyncConflict class by using conflict type and conflict stage parameters.
     */
    var type: ConflictType = ConflictType.ErrorsOccurred

    /**
     * add a local row
     */
    internal fun addLocalRow(row: SyncRow) {
        this.localRow = row
    }

    /**
     * add a remote row
     */
    internal fun addRemoteRow(row: SyncRow) {
        this.remoteRow = row
    }

    fun hasLocalRow() = ::localRow.isInitialized
    fun hasRemoteRow() = ::remoteRow.isInitialized
}
