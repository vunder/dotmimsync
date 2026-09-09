package com.mimetis.dotmim.sync.set

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class SyncRelation(
        /**
         * Gets or Sets the relation name
         */
        @SerialName("n")
        var relationName: String,

        /**
         * Gets or Sets a list of columns that represent the parent key.
         */
        @SerialName("pks")
        var parentKeys: ArrayList<SyncColumnIdentifier> = ArrayList(),

        /**
         * Gets or Sets a list of columns that represent the parent key.
         */
        @SerialName("cks")
        var keys: ArrayList<SyncColumnIdentifier> = ArrayList()
) : SyncNamedItem<SyncRelation>() {
    /**
     * Gets the ShemaFilter's SyncSchema
     */
    @Transient
    var schema: SyncSet? = null

    /**
     * Ensure this relation has correct Schema reference
     */
    fun ensureRelation(schema: SyncSet) {
        this.schema = schema
    }

    /// <summary>
    /// Get child table
    /// </summary>
    fun getTable(): SyncTable? {
        if (this.schema == null || this.keys.isEmpty())
            return null

        val id = this.keys.first()

        return this.schema!!.tables[id.tableName, id.schemaName]
    }

    /**
     * Get parent table
     */
    fun getParentTable(): SyncTable? {
        if (this.schema == null || this.parentKeys.isEmpty())
            return null

        val id = this.parentKeys.first()

        return this.schema!!.tables[id.tableName, id.schemaName]
    }

    fun clone(): SyncRelation =
            SyncRelation(
                    this.relationName,
                    ArrayList(this.parentKeys.map { pk -> pk.clone() }),
                    ArrayList(this.keys.map { ck -> ck.clone() }),
            )

    fun clear() {
        this.keys.clear()
        this.parentKeys.clear()
        this.schema = null
    }

    override fun getAllNamesProperties(): List<String> =
            listOf(this.relationName)
}
