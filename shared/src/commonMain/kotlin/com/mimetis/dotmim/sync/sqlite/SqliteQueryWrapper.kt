package com.mimetis.dotmim.sync.sqlite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import com.benasher44.uuid.Uuid
import java.io.Closeable
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Wrapper to cache sql-query building and executing process
 *
 * Used for queries with NAMED PARAMETERS ONLY
 * E.g. "SELECT * FROM Table WHERE Age>@Age"
 *
 * Internally translate input query into query with unnamed parameters and bind those parameters
 * when executing sql statement
 *
 * E.g. "UPDATE Table SET Flag=0 WHERE Date>@Date AND Age>@Age AND OtherDate>@Date"
 *  1. Transform query: "UPDATE Table SET Flag=0 WHERE Date>? AND Age>? AND OtherDate>?"
 *  2. Form parameters order: @Date, @Age, @Date
 *  3. Using input parameters map (@Age=25, @Date=25.11.2010) making query:
 *      "UPDATE Table SET Flag=0 WHERE Date>? AND Age>? AND OtherDate>?", { 25.11.2010, 25, 25.11.2010 }
 *  4. Return number of updated rows, executing following query "SELECT changes()"
 *
 * @constructor SqliteQueryWrapper
 * @param database SQLiteDatabase to execute queries
 * @param dateFormat SimpleDateFormat instance to format Date values
 * @param queryBuilder Function to form sql query. Called only once on the first call
 */
class SqliteQueryWrapper(
    private val database: SQLiteDatabase,
    private val dateFormat: SimpleDateFormat,
    private val queryBuilder: () -> String
) : Closeable {
    private var sql: String = ""
    private var statement: SQLiteStatement? = null
    private var order: List<String>? = null

    /**
     * Execute statement with parameters
     * @return Number of inserted/updated/deleted rows
     */
    fun executeStatement(parameters: Map<String, Any?> = emptyMap()): Int {
        ensureSqlQuery()

        if (statement == null) {
            processSql(sql).apply {
                statement = database.compileStatement(first)
                order = second
            }
        } else {
            statement?.clearBindings()
        }

        bindParameters(parameters)

        statement?.execute()

        return getChanges()
    }

    /**
     * Execute query with parameters
     * @return Cursor instance to manually handle query results (e.g. within SELECT query)
     */
    fun executeCursor(parameters: Map<String, Any?> = emptyMap()): Cursor {
        ensureSqlQuery()

        val result = processSql(sql)

        return database.rawQuery(result.first, result.second.map { parameters[it]?.toString() }.toTypedArray())
    }

    private fun ensureSqlQuery() {
        if (sql.isBlank())
            sql = queryBuilder()
    }

    /**
     * Process sql-query with named parameters to a query without named parameters to run it over
     * SQLiteDatabase that does not support named parameters
     * @param query Input sql-query with named parameters
     * @return Transformed sql-query, parameters order to match values with unnamed parameters in the
     * query
     */
    private fun processSql(query: String): Pair<String, List<String>> {
        val bindOrder = mutableListOf<String>()
        var sql = query
        do {
            val startIndex = sql.indexOf("@")
            if (startIndex >= 0) {
                var index = startIndex + 1
                while (index < sql.length && (sql[index].isLetterOrDigit() || sql[index] == '_'))
                    ++index
                index = kotlin.math.min(index, sql.length)
                val name = sql.substring(startIndex, index)
                bindOrder.add(name)
                sql = "${sql.substring(0, startIndex)}?${sql.substring(index)}"
            }
        } while (startIndex >= 0)

        return Pair(sql, bindOrder)
    }

    private fun bindParameters(parameters: Map<String, Any?>) {
        var index = 1
        order?.forEach {
            when (val value = parameters[it]) {
                null, is Unit -> statement?.bindNull(index++)
                is String -> statement?.bindString(index++, value)
                is Byte -> statement?.bindLong(index++, value.toLong())
                is Int -> statement?.bindLong(index++, value.toLong())
                is Long -> statement?.bindLong(index++, value)
                is Boolean -> statement?.bindLong(index++, if (value) 1 else 0)
                is ByteArray -> statement?.bindBlob(index++, value)
                is Double -> statement?.bindDouble(index++, value)
                is Float -> statement?.bindDouble(index++, value.toDouble())
                is BigDecimal -> statement?.bindDouble(index++, value.toDouble())
                is Uuid -> statement?.bindString(index++, value.toString().uppercase())
                is Date -> statement?.bindString(index++, dateFormat.format(value))
                else -> statement?.bindString(index++, value.toString())
            }
        }
    }

    private fun getChanges() =
        database.rawQuery("SELECT changes()", null).use { cursor ->
            if (cursor.moveToNext())
                cursor.getInt(0)
            else
                0
        }

    override fun close() {
        statement?.close()
    }
}
