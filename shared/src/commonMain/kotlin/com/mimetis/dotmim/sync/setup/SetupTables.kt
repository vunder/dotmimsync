package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

/**
 * Represents a list of tables to be added to the sync process
 */
@Serializable(with = SetupTablesSerializer::class)
class SetupTables : ArrayList<SetupTable>() {
    /**
     * Get a table by its name
     */
    operator fun get(tableName: String, schemaName: String): SetupTable? {
        if (tableName.isBlank())
            throw Exception("`tableName' parameter in empty ($tableName)")

        val scn = if (schemaName.isBlank()) "" else schemaName
        val table = this.firstOrNull { innerTable ->
            val innerTableSchemaName = if (innerTable.schemaName.isBlank()) "" else innerTable.schemaName
            return@firstOrNull innerTable.tableName.equals(tableName, true) && innerTableSchemaName.equals(scn, true)

        }
        return table
    }
}

object SetupTablesSerializer : ArrayListLikeSerializer<SetupTables, SetupTable>(SetupTable.serializer())
