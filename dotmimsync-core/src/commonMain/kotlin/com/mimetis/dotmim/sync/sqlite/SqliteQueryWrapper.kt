package com.mimetis.dotmim.sync.sqlite

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeFormat
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
@OptIn(ExperimentalUuidApi::class)
class SqliteQueryWrapper(
    private val database: SQLiteConnection,
    private val dateFormat: DateTimeFormat<LocalDateTime>,
    private val queryBuilder: () -> String
) : AutoCloseable {
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
                statement = database.prepare(first)
                order = second
            }
        } else {
            statement?.clearBindings()
        }

        if (statement != null && order != null)
            bindParameters(statement!!, order!!, parameters)

        statement?.step()

        return getChanges()
    }

    /**
     * Execute query with parameters
     * @return Cursor instance to manually handle query results (e.g. within SELECT query)
     */
    fun executeCursor(parameters: Map<String, Any?> = emptyMap()): SQLiteStatement {
        ensureSqlQuery()

        val result = processSql(sql)

        return database.prepare(result.first).apply {
            bindParameters(this, result.second, parameters)
        }
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

    private fun bindParameters(
        sqlStatement: SQLiteStatement,
        parametersOrder: List<String>,
        parameters: Map<String, Any?>
    ) {
        var index = 1
        parametersOrder.forEach {
            when (val value = parameters[it]) {
                null, is Unit -> sqlStatement.bindNull(index++)
                is String -> sqlStatement.bindText(index++, value)
                is Byte -> sqlStatement.bindLong(index++, value.toLong())
                is Int -> sqlStatement.bindLong(index++, value.toLong())
                is Long -> sqlStatement.bindLong(index++, value)
                is Boolean -> sqlStatement.bindLong(index++, if (value) 1 else 0)
                is ByteArray -> sqlStatement.bindBlob(index++, value)
                is Double -> sqlStatement.bindDouble(index++, value)
                is Float -> sqlStatement.bindDouble(index++, value.toDouble())
                is BigDecimal -> sqlStatement.bindDouble(index++, value.doubleValue(false))
                is Uuid -> sqlStatement.bindText(index++, value.toString().uppercase())
                is LocalDateTime -> sqlStatement.bindText(index++, dateFormat.format(value))
                else -> sqlStatement.bindText(index++, value.toString())
            }
        }
    }

    private fun getChanges() =
        database.prepare("SELECT changes()").use { cursor ->
            if (cursor.step())
                cursor.getInt(0)
            else
                0
        }

    override fun close() {
        statement?.close()
    }
}
