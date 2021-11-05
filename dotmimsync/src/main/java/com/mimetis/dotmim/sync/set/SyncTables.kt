package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.builders.ParserName

@Serializable(with = SyncTablesSerializer::class)
class SyncTables() : ArrayList<SyncTable>() {
    /**
     * Table's schema
     */
    @Transient
    var schema: SyncSet? = null

    constructor(schema: SyncSet) : this() {
        this.schema = schema
    }

    /**
     * Since we don't serializer the reference to the schema, this method will reaffect the correct schema
     */
    fun ensureTables(schema: SyncSet) {
        this.schema = schema
        this.forEach { it.ensureTable(schema) }
    }

    override fun clear() {
        this.forEach { it.clear() }
        super.clear()
    }

    override fun add(element: SyncTable): Boolean {
        element.schema = schema
        return super.add(element)
    }

    /**
     * Get a table by its name
     */
    operator fun get(tableName: String, schemaName: String): SyncTable? {
        if (tableName.isBlank())
            throw Exception("`tableName' parameter in empty ($tableName)")

        val parser = ParserName.parse(tableName)
        val tblName = parser.objectName

        val scn = if (schemaName.isBlank()) "" else schemaName
        val table = this.firstOrNull { innerTable ->
            val innerTableSchemaName = if (innerTable.schemaName.isBlank()) "" else innerTable.schemaName
            return@firstOrNull innerTable.tableName.equals(tblName, true) && innerTableSchemaName.equals(scn, true)

        }
        return table
    }
}

object SyncTablesSerializer : ArrayListLikeSerializer<SyncTables, SyncTable>(SyncTable.serializer())
