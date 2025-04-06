package com.mimetis.dotmim.sync.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DatabaseChangesSelected(
        @SerialName("tcs")
        var tableChangesSelected: ArrayList<TableChangesSelected> = ArrayList()
) {
    /**
     * Gets the total number of changes that are to be applied during the synchronization session.
     */
    @Transient
    val totalChangesSelected: Int
        get() = this.tableChangesSelected.sumOf { it.totalChanges }

    /**
     * Gets the total number of deletes that are to be applied during the synchronization session.
     */
    @Transient
    val totalChangesSelectedDeletes: Int
        get() = this.tableChangesSelected.sumOf { it.deletes }

    /**
     * Gets the total number of updates OR inserts that are to be applied during the synchronization session.
     */
    @Transient
    val totalChangesSelectedUpdates
        get() = this.tableChangesSelected.sumOf { it.upserts }

    override fun toString(): String =
            "${this.totalChangesSelected} changes selected for ${this.tableChangesSelected.size} tables"
}
