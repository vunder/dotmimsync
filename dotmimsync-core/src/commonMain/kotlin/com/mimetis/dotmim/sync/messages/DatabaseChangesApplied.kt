package com.mimetis.dotmim.sync.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DatabaseChangesApplied(
        /**
         * Get the view to be applied
         */
        @SerialName("tca")
        var tableChangesApplied: MutableList<TableChangesApplied> = ArrayList()
) {
    /**
     * Gets the total number of conflicts that have been applied resolved during the synchronization session.
     */
    @Transient
    val totalResolvedConflicts: Int
        get() = this.tableChangesApplied.sumOf { it.resolvedConflicts }

    /**
     * Gets the total number of changes that have been applied during the synchronization session.
     */
    @Transient
    val totalAppliedChanges: Int
        get() = this.tableChangesApplied.sumOf { it.applied }

    /**
     * Gets the total number of changes that have failed to be applied during the synchronization session.
     */
    @Transient
    val totalAppliedChangesFailed: Int
        get() = this.tableChangesApplied.sumOf { it.failed }

    override fun toString(): String =
            "${this.totalAppliedChanges} changes applied for ${this.tableChangesApplied.size} tables"
}
