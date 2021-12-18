package com.mimetis.dotmim.sync.builders

import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.set.SyncTable

class ParserName private constructor() {
    private lateinit var key: String
    private var withDatabase: Boolean = false
    private var withSchema: Boolean = false
    private var withQuotes: Boolean = false
    private var withNormalized: Boolean = false

    val schemaName: String
        get() = GlobalParser.getParserString(this.key).schemaName
    val objectName: String
        get() = GlobalParser.getParserString(this.key).objectName
    val databaseName: String
        get() = GlobalParser.getParserString(this.key).databaseName

    /**
     * Add database name if available to the final string
     */
    fun database(): ParserName {
        withDatabase = true
        return this
    }

    /**
     * Add schema if available to the final string
     */
    fun schema(): ParserName {
        withSchema = true
        return this
    }

    /**
     * Add quotes ([] or ``) on all objects
     */
    fun quoted(): ParserName {
        withQuotes = true
        return this
    }

    fun unquoted(): ParserName {
        withQuotes = false
        return this
    }

    fun normalized(): ParserName {
        withNormalized = true
        return this
    }

    private constructor(
        table: SyncTable,
        leftQuote: String? = null,
        rightQuote: String? = null
    ) : this() {
        val input =
            if (table.schemaName.isBlank()) table.tableName else "${table.schemaName}.${table.tableName}"
        parseString(input, leftQuote, rightQuote)
    }

    private constructor(
        column: SyncColumn,
        leftQuote: String? = null,
        rightQuote: String? = null
    ) : this() {
        parseString(column.columnName, leftQuote, rightQuote)
    }

    private constructor(
        input: String,
        leftQuote: String? = null,
        rightQuote: String? = null
    ) : this() {
        parseString(input, leftQuote, rightQuote)
    }

    /**
     * Parse the input string and Get a non bracket object name :
     *      "[Client] ==> Client "
     *      "[dbo].[client] === > dbo client "
     *      "dbo.client === > dbo client "
     *      "Fabrikam.[dbo].[client] === > Fabrikam dbo client "
     */
    private fun parseString(input: String?, leftQuote: String? = null, rightQuote: String? = null) {
        val input1 = input?.trim() ?: ""
        this.key = input1

        if (!leftQuote.isNullOrBlank() && !rightQuote.isNullOrBlank())
            this.key = "${leftQuote}^${rightQuote}^${input}"
        else if (!leftQuote.isNullOrBlank())
            this.key = "${leftQuote}^${leftQuote}^${input}"

        GlobalParser.getParserString(this.key)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        val parsedName = GlobalParser.getParserString(key)

        if (withDatabase && databaseName.isNotBlank()) {
            builder.append(if (withQuotes) parsedName.quotedDatabaseName else databaseName)
            builder.append(if (withNormalized) "_" else ".")
        }
        if (withSchema && schemaName.isNotBlank()) {
            builder.append(if (withQuotes) parsedName.quotedSchemaName else schemaName)
            builder.append(if (withNormalized) "_" else ".")
        }

        var name = if (withQuotes) parsedName.quotedObjectName else objectName
        name = if (withNormalized)
            name.replace(" ", "_").replace(".", "_").replace("-", "_")
        else
            name
        builder.append(name)

        // now we have the correct string, reset options for the next time we call the same instance
        withDatabase = false
        withSchema = false
        withQuotes = false
        withNormalized = false

        return builder.toString()
    }

    companion object {
        fun parse(syncTable: SyncTable, leftQuote: String? = null, rightQuote: String? = null) =
            ParserName(syncTable, leftQuote, rightQuote)

        fun parse(syncColumn: SyncColumn, leftQuote: String? = null, rightQuote: String? = null) =
            ParserName(syncColumn, leftQuote, rightQuote)

        fun parse(input: String, leftQuote: String? = null, rightQuote: String? = null) =
            ParserName(input, leftQuote, rightQuote)
    }
}
