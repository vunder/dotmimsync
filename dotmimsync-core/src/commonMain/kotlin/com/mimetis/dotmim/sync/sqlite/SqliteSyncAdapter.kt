package com.mimetis.dotmim.sync.sqlite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.sqlite.SQLiteStatement
import com.mimetis.dotmim.sync.DbSyncAdapter
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.set.SyncRow
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.DbType
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalEncodingApi::class)
class SqliteSyncAdapter(
    tableDescription: SyncTable,
    private val tableName: ParserName,
    private val trackingName: ParserName,
    setup: SyncSetup,
    private val database: SQLiteDatabase
) : DbSyncAdapter(tableDescription, setup) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var getSelectChangesSql = ""

    override fun getSelectInitializedChanges(): Cursor {
        val stringBuilder = StringBuilder("SELECT ")
        for (pkColumn in this.tableDescription.getPrimaryKeysColumns()) {
            val columnName = ParserName.parse(pkColumn).quoted().toString()
            stringBuilder.appendLine("\t[base].$columnName, ")
        }
        val columns = this.tableDescription.getMutableColumns()

        for (i in columns.indices) {
            val mutableColumn = columns[i]
            val columnName = ParserName.parse(mutableColumn).quoted().toString()
            stringBuilder.append("\t[base].$columnName")

            if (i < columns.size - 1)
                stringBuilder.appendLine(", ")
        }
        stringBuilder.appendLine("FROM ${tableName.quoted()} [base]")

        return database.rawQuery(stringBuilder.toString(), null)
    }

    override fun getSelectChanges(lastTimestamp: Long?): Cursor {
        if (getSelectChangesSql.isEmpty()) {
            val stringBuilder = StringBuilder("SELECT ")
            for (pkColumn in this.tableDescription.getPrimaryKeysColumns()) {
                val columnName = ParserName.parse(pkColumn).quoted().toString()
                stringBuilder.appendLine("\t[side].$columnName, ")
            }
            for (mutableColumn in this.tableDescription.getMutableColumns()) {
                val columnName = ParserName.parse(mutableColumn).quoted().toString()
                stringBuilder.appendLine("\t[base].$columnName, ")
            }
            stringBuilder.appendLine("\t[side].[sync_row_is_tombstone], ")
            stringBuilder.appendLine("\t[side].[update_scope_id] as [sync_update_scope_id] ")
            stringBuilder.appendLine("FROM ${trackingName.quoted()} [side]")
            stringBuilder.appendLine("LEFT JOIN ${tableName.quoted()} [base]")
            stringBuilder.append("ON ")

            var empty = ""
            for (pkColumn in this.tableDescription.getPrimaryKeysColumns()) {
                val columnName = ParserName.parse(pkColumn).quoted().toString()

                stringBuilder.append("$empty[base].$columnName = [side].$columnName")
                empty = " AND "
            }
            stringBuilder.appendLine()
            //stringBuilder.appendLine("WHERE (");
            //stringBuilder.appendLine("\t[side].[timestamp] > @sync_min_timestamp");
            //stringBuilder.appendLine("\tAND ([side].[update_scope_id] <> @sync_scope_id OR [side].[update_scope_id] IS NULL)");
            //stringBuilder.appendLine(")");

            // Looking at discussion https://github.com/Mimetis/Dotmim.Sync/discussions/453, trying to remove ([side].[update_scope_id] <> @sync_scope_id)
            // since we are sure that sqlite will never be a server side database

            stringBuilder.appendLine("WHERE ([side].[timestamp] > @sync_min_timestamp AND [side].[update_scope_id] IS NULL)")

            getSelectChangesSql = stringBuilder.toString()
        }

        return database.rawQuery(getSelectChangesSql, arrayOf((lastTimestamp ?: 0).toString()))
    }

    private val getSelectRowQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,
        dateFormat
    ) {
        val stringBuilder = StringBuilder(1000)
            .appendLine("SELECT ")
        val stringBuilder1 = StringBuilder()
        var empty = ""
        for (pkColumn in this.tableDescription.getPrimaryKeysColumns()) {
            val columnName = ParserName.parse(pkColumn).quoted().toString()
            val unquotedColumnName =
                ParserName.parse(pkColumn).unquoted().normalized().toString()
            stringBuilder.appendLine("\t[side].${columnName}, ")
            stringBuilder1.append("${empty}[side].${columnName} = @${unquotedColumnName}")
            empty = " AND "
        }
        for (mutableColumn in this.tableDescription.getMutableColumns()) {
            val nonPkColumnName = ParserName.parse(mutableColumn).quoted().toString()
            stringBuilder.appendLine("\t[base].${nonPkColumnName}, ")
        }
        stringBuilder.appendLine("\t[side].[sync_row_is_tombstone], ")
        stringBuilder.appendLine("\t[side].[update_scope_id] as [sync_update_scope_id]")

        stringBuilder.appendLine("FROM ${trackingName.quoted()} [side] ")
        stringBuilder.appendLine("LEFT JOIN ${tableName.quoted()} [base] ON ")

        var str = ""
        for (pkColumn in this.tableDescription.getPrimaryKeysColumns()) {
            val columnName = ParserName.parse(pkColumn).quoted().toString()
            stringBuilder.append("${str}[base].${columnName} = [side].${columnName}")
            str = " AND "
        }
        stringBuilder.appendLine()
        stringBuilder.append("WHERE $stringBuilder1")
        stringBuilder.append(";")

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    override fun getSelectRow(primaryKeyRow: SyncRow): Cursor {
        val parameters = mutableMapOf<String, Any?>()
        fillParametersFromColumns(
            parameters,
            this.tableDescription.getPrimaryKeysColumns().filter { c -> !c.isReadOnly },
            primaryKeyRow,
            stringOnly = true
        )

        return getSelectRowQuery.executeCursor(parameters)
    }

    override fun enableConstraints() =
        database.execSQL("Select 0")

    override fun disableConstraints() =
        database.execSQL("Select 0")

    override fun reset() {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine()
        stringBuilder.appendLine("DELETE FROM ${tableName.quoted()};")
        stringBuilder.appendLine("DELETE FROM ${trackingName.quoted()};")
        database.execSQL(stringBuilder.toString())
    }

    private val deleteRowQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,
        dateFormat
    ) {
        val stringBuilder = StringBuilder(1000)
        val str1 = SqliteManagementUtils.joinTwoTablesOnClause(
            this.tableDescription.primaryKeys,
            "[c]",
            "[base]"
        )
        val str7 = SqliteManagementUtils.joinTwoTablesOnClause(
            this.tableDescription.primaryKeys,
            "[p]",
            "[side]"
        )

        stringBuilder.appendLine(";WITH [c] AS (")
        stringBuilder.append("\tSELECT ")
        for (c in this.tableDescription.getPrimaryKeysColumns()) {
            val columnName = ParserName.parse(c).quoted().toString()
            stringBuilder.append("[p].${columnName}, ")
        }
        stringBuilder.appendLine("[side].[update_scope_id] as [sync_update_scope_id], [side].[timestamp] as [sync_timestamp], [side].[sync_row_is_tombstone]")
        stringBuilder.append("\tFROM (SELECT ")
        var comma = ""
        for (c in this.tableDescription.getPrimaryKeysColumns()) {
            val columnName = ParserName.parse(c).quoted().toString()
            val columnParameterName = ParserName.parse(c).unquoted().normalized().toString()

            stringBuilder.append("${comma}@${columnParameterName} as $columnName")
            comma = ", "
        }
        stringBuilder.appendLine(") AS [p]")
        stringBuilder.append("\tLEFT JOIN ${trackingName.quoted()} [side] ON ")
        stringBuilder.appendLine("\t${str7}")
        stringBuilder.appendLine("\t)")

        stringBuilder.appendLine("DELETE FROM ${tableName.quoted()} ")
        stringBuilder.appendLine(
            "WHERE ${
                SqliteManagementUtils.whereColumnAndParameters(
                    this.tableDescription.primaryKeys,
                    ""
                )
            }"
        )
        stringBuilder.appendLine("AND (EXISTS (")
        stringBuilder.appendLine("     SELECT * FROM [c] ")
        stringBuilder.appendLine(
            "     WHERE ${
                SqliteManagementUtils.whereColumnAndParameters(
                    this.tableDescription.primaryKeys,
                    "[c]"
                )
            }"
        )
        stringBuilder.appendLine("     AND ([sync_timestamp] < @sync_min_timestamp OR [sync_timestamp] IS NULL OR [sync_update_scope_id] = @sync_scope_id))")
        stringBuilder.appendLine("  OR @sync_force_write = 1")
        stringBuilder.appendLine(" );")

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    private val deleteRowTrackingQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,
        dateFormat
    ) {
        val stringBuilder = StringBuilder(500)
        stringBuilder.appendLine("UPDATE OR IGNORE ${trackingName.quoted()} SET ")
        stringBuilder.appendLine("[update_scope_id] = @sync_scope_id,")
        stringBuilder.appendLine("[sync_row_is_tombstone] = 1,")
        stringBuilder.appendLine("[last_change_datetime] = datetime('now')")
        stringBuilder.appendLine(
            "WHERE ${
                SqliteManagementUtils.whereColumnAndParameters(
                    this.tableDescription.primaryKeys,
                    ""
                )
            }"
        )
        stringBuilder.appendLine(" AND (select changes()) > 0")

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    override fun deleteRow(
        scopeId: Uuid?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int {
        val parameters = mutableMapOf<String, Any?>(
            "@sync_force_write" to forceWrite,
            "@sync_min_timestamp" to syncTimeStamp,
            "@sync_scope_id" to scopeId
        )
        fillParametersFromColumns(
            parameters,
            this.tableDescription.getPrimaryKeysColumns().filter { c -> !c.isReadOnly },
            row
        )

        val op1 = deleteRowQuery.executeStatement(parameters)

        val op2 = deleteRowTrackingQuery.executeStatement(parameters)

        return op1 + op2
    }

    private val initializeRowQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,
        dateFormat
    ) {
        val stringBuilderArguments = StringBuilder()
        val stringBuilderParameters = StringBuilder()
        val stringBuilderParametersValues = StringBuilder()
        val stringBuilderParametersValues2 = StringBuilder()
        var empty = ""

        val str1 = SqliteManagementUtils.joinOneTablesOnParametersValues(
            this.tableDescription.primaryKeys,
            "[side]"
        )
        val str2 = SqliteManagementUtils.joinOneTablesOnParametersValues(
            this.tableDescription.primaryKeys,
            "[base]"
        )
        val str7 = SqliteManagementUtils.joinTwoTablesOnClause(
            this.tableDescription.primaryKeys,
            "[p]",
            "[side]"
        )

        // Generate Update command
        val stringBuilder = StringBuilder(1000)

        for (mutableColumn in this.tableDescription.getMutableColumns(
            includeAutoIncrement = false,
            includePrimaryKeys = true
        )) {
            val columnName = ParserName.parse(mutableColumn).quoted().toString()
            val columnParameterName =
                ParserName.parse(mutableColumn).unquoted().normalized().toString()
            stringBuilderParametersValues.append("${empty}@${columnParameterName} as $columnName")
            stringBuilderParametersValues2.append("${empty}@${columnParameterName}")
            stringBuilderArguments.append("${empty}${columnName}")
            stringBuilderParameters.append("${empty}[c].${columnName}")
            empty = ", "
        }

        stringBuilder.appendLine("INSERT OR IGNORE INTO ${tableName.quoted()}")
        stringBuilder.appendLine("(${stringBuilderArguments})")
        stringBuilder.append("VALUES (${stringBuilderParametersValues2}) ")
        stringBuilder.appendLine(";")

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    private val initializeRowTrackingQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,
        dateFormat
    ) {
        val stringBuilder = StringBuilder(500)
        stringBuilder.appendLine("UPDATE OR IGNORE ${trackingName.quoted()} SET ")
        stringBuilder.appendLine("[update_scope_id] = @sync_scope_id,")
        stringBuilder.appendLine("[sync_row_is_tombstone] = 0,")
        stringBuilder.appendLine("[last_change_datetime] = datetime('now')")
        stringBuilder.appendLine(
            "WHERE ${
                SqliteManagementUtils.whereColumnAndParameters(
                    this.tableDescription.primaryKeys,
                    ""
                )
            }"
        )
        stringBuilder.append(" AND (select changes()) > 0")
        stringBuilder.appendLine(";")

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    override fun initializeRow(
        scopeId: Uuid?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int {
        val parameters = mutableMapOf<String, Any?>(
            "@sync_force_write" to forceWrite,
            "@sync_min_timestamp" to syncTimeStamp,
            "@sync_scope_id" to scopeId
        )
        fillParametersFromColumns(
            parameters,
            this.tableDescription.columns!!.filter { c -> !c.isReadOnly },
            row
        )

        val op1 = initializeRowQuery.executeStatement(parameters)

        val op2 = initializeRowTrackingQuery.executeStatement(parameters)

        return op1 + op2
    }

    private val sqliteRowQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,
        dateFormat
    ) {
        val stringBuilderArguments = StringBuilder()
        val stringBuilderParameters = StringBuilder()
        val stringBuilderParametersValues = StringBuilder()
        var empty = ""

        val str1 = SqliteManagementUtils.joinOneTablesOnParametersValues(
            this.tableDescription.primaryKeys,
            "[side]"
        )
//        val str2 = SqliteManagementUtils.joinTwoTablesOnClause(this.tableDescription.primaryKeys, "[c]", "[base]")
        val str2 = SqliteManagementUtils.joinOneTablesOnParametersValues(
            this.tableDescription.primaryKeys,
            "[base]"
        )

        // Generate Update command
        val stringBuilder = StringBuilder(1000)

        for (mutableColumn in this.tableDescription.getMutableColumns(
            includeAutoIncrement = false,
            includePrimaryKeys = true
        )) {
            val columnName = ParserName.parse(mutableColumn).quoted().toString()
            val columnParameterName =
                ParserName.parse(mutableColumn).unquoted().normalized().toString()

            stringBuilderParametersValues.append("${empty}@${columnParameterName} as $columnName")
            stringBuilderArguments.append("${empty}${columnName}")
            stringBuilderParameters.append("${empty}[c].${columnName}")
            empty = "\n, "
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // No support for "ON CONFLICT" in Android < 11 because this is supported in Sqlite after 3.24
            stringBuilder.appendLine("INSERT OR REPLACE INTO ${tableName.quoted()}")
            stringBuilder.appendLine("(${stringBuilderArguments})")
            stringBuilder.appendLine("SELECT $stringBuilderParameters ")
            stringBuilder.appendLine("FROM (SELECT ${stringBuilderParametersValues}) as [c]")
            stringBuilder.appendLine("LEFT JOIN ${trackingName.quoted()} AS [side] ON $str1")
            stringBuilder.appendLine("LEFT JOIN ${tableName.quoted()} AS [base] ON $str2")

            stringBuilder.appendLine(
                "WHERE (${
                    SqliteManagementUtils.whereColumnAndParameters(
                        this.tableDescription.primaryKeys,
                        "[base]"
                    )
                } "
            )
            stringBuilder.appendLine("AND ([side].[timestamp] < @sync_min_timestamp OR [side].[update_scope_id] = @sync_scope_id)) ")
            stringBuilder.appendLine(
                "OR (${
                    SqliteManagementUtils.whereColumnIsNull(
                        this.tableDescription.primaryKeys,
                        "[base]"
                    )
                } "
            )
            stringBuilder.appendLine("AND ([side].[timestamp] < @sync_min_timestamp OR [side].[timestamp] IS NULL)) ")
            stringBuilder.appendLine("OR @sync_force_write = 1;")
        } else {
            // create update statement without PK
            var emptyUpdate = ""
            var columnsToUpdate = false
            val stringBuilderUpdateSet = StringBuilder()
            for (mutableColumn in this.tableDescription.getMutableColumns(
                includeAutoIncrement = false,
                includePrimaryKeys = false
            )) {
                val columnName = ParserName.parse(mutableColumn).quoted().toString()
                stringBuilderUpdateSet.append("${emptyUpdate}${columnName}=excluded.${columnName}")
                emptyUpdate = "\n, "

                columnsToUpdate = true
            }

            val primaryKeys = this.tableDescription.primaryKeys.joinToString(",") { name ->
                ParserName.parse(name).quoted().toString()
            }

            // add CTE
            stringBuilder.appendLine("WITH CHANGESET as (SELECT $stringBuilderParameters ")
            stringBuilder.appendLine("FROM (SELECT ${stringBuilderParametersValues}) as [c]")
            stringBuilder.appendLine("LEFT JOIN ${trackingName.quoted()} AS [side] ON $str1")
            stringBuilder.appendLine("LEFT JOIN ${tableName.quoted()} AS [base] ON $str2")
//        stringBuilder.appendLine("WHERE (${SqliteManagementUtils.whereColumnAndParameters(this.tableDescription.primaryKeys, "[base]")} ")
            stringBuilder.appendLine("WHERE ([side].[timestamp] < @sync_min_timestamp OR [side].[update_scope_id] = @sync_scope_id) ")
            stringBuilder.append(
                "OR (${
                    SqliteManagementUtils.whereColumnIsNull(
                        this.tableDescription.primaryKeys,
                        "[base]"
                    )
                } "
            )
            stringBuilder.appendLine("AND ([side].[timestamp] < @sync_min_timestamp OR [side].[timestamp] IS NULL)) ")
            stringBuilder.append("OR @sync_force_write = 1")
            stringBuilder.appendLine(")")

            stringBuilder.appendLine("INSERT INTO ${tableName.quoted()}")
            stringBuilder.appendLine("(${stringBuilderArguments})")
            // use CTE here. The CTE is required in order to make the "ON CONFLICT" statement work. Otherwise SQLite cannot parse it
            // Note, that we have to add the pseudo WHERE TRUE clause here, as otherwise the SQLite parser may confuse the following ON
            // with a join clause, thus, throwing a parsing error
            // See a detailed explanation here at the official SQLite documentation: "Parsing Ambiguity" on page https://www.sqlite.org/lang_UPSERT.html
            stringBuilder.appendLine(" SELECT * from CHANGESET WHERE TRUE")
            if (columnsToUpdate) {
                stringBuilder.appendLine(" ON CONFLICT (${primaryKeys}) DO UPDATE SET ")
                stringBuilder.append(stringBuilderUpdateSet).appendLine(";")
            } else
                stringBuilder.appendLine(" ON CONFLICT (${primaryKeys}) DO NOTHING; ")
        }

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    private val sqliteRowTrackingQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,
        dateFormat
    ) {
        val stringBuilder = StringBuilder(500)
        stringBuilder.appendLine("UPDATE OR IGNORE ${trackingName.quoted()} SET ")
        stringBuilder.appendLine("[update_scope_id] = @sync_scope_id,")
        stringBuilder.appendLine("[sync_row_is_tombstone] = 0,")
        stringBuilder.appendLine("[timestamp] = ${SqliteTableBuilder.TimestampValue},")
        stringBuilder.appendLine("[last_change_datetime] = datetime('now')")
        stringBuilder.appendLine(
            "WHERE ${
                SqliteManagementUtils.whereColumnAndParameters(
                    this.tableDescription.primaryKeys,
                    ""
                )
            }"
        )
        stringBuilder.appendLine(" AND (select changes()) > 0;")

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    override fun updateRow(
        scopeId: Uuid?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int {
        val parameters = mutableMapOf<String, Any?>(
            "@sync_force_write" to forceWrite,
            "@sync_min_timestamp" to syncTimeStamp,
            "@sync_scope_id" to scopeId
        )
        fillParametersFromColumns(
            parameters,
            this.tableDescription.columns!!.filter { c -> !c.isReadOnly },
            row
        )

        val op1 = sqliteRowQuery.executeStatement(parameters)

        val op2 = sqliteRowTrackingQuery.executeStatement(parameters)

        return op1 + op2
    }

    private var sqliteMetadataQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,
        dateFormat
    ) {
        val stringBuilder = StringBuilder(1000)

        val pkeySelectForInsert = StringBuilder()
        val pkeyISelectForInsert = StringBuilder()
        val pkeyAliasSelectForInsert = StringBuilder()
        val pkeysLeftJoinForInsert = StringBuilder()
        val pkeysIsNullForInsert = StringBuilder()

        var and = ""
        var comma = ""
        for (pkColumn in tableDescription.getPrimaryKeysColumns()) {
            val columnName = ParserName.parse(pkColumn).quoted().toString()
            val parameterName = ParserName.parse(pkColumn).unquoted().normalized().toString()

            pkeySelectForInsert.append("${comma}${columnName}")
            pkeyISelectForInsert.append("${comma}[i].${columnName}")
            pkeyAliasSelectForInsert.append("${comma}@${parameterName} as $columnName")
            pkeysLeftJoinForInsert.append("${and}[side].${columnName} = [i].${columnName}")
            pkeysIsNullForInsert.append("${and}[side].${columnName} IS NULL")
            and = " AND "
            comma = ", "
        }

        stringBuilder.appendLine("INSERT OR REPLACE INTO ${trackingName.schema().quoted()} (")
        stringBuilder.appendLine(pkeySelectForInsert)
        stringBuilder.appendLine(",[update_scope_id], [sync_row_is_tombstone], [timestamp], [last_change_datetime] )")
        stringBuilder.appendLine("SELECT $pkeyISelectForInsert ")
        stringBuilder.appendLine("   , i.sync_scope_id, i.sync_row_is_tombstone, i.sync_timestamp, i.UtcDate")
        stringBuilder.appendLine("FROM (")
        stringBuilder.appendLine("  SELECT $pkeyAliasSelectForInsert")
        stringBuilder.appendLine("          ,@sync_scope_id as sync_scope_id, @sync_row_is_tombstone as sync_row_is_tombstone, ${SqliteTableBuilder.TimestampValue} as sync_timestamp, datetime('now') as UtcDate) as i;")

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    override fun updateMetadata(
        scopeId: Uuid?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int {
        val parameters = mutableMapOf<String, Any?>(
            "@sync_force_write" to forceWrite,
            "@sync_scope_id" to scopeId,
            "@sync_row_is_tombstone" to isDeleted
        )
        fillParametersFromColumns(
            parameters,
            this.tableDescription.getPrimaryKeysColumns().filter { c -> !c.isReadOnly },
            row
        )

        return sqliteMetadataQuery.executeStatement(parameters)
    }

    override fun deleteMetadata(timestamp: Long): Int {
        database.execSQL("DELETE FROM ${trackingName.quoted()} WHERE [timestamp] < $timestamp;")

        return database.rawQuery("SELECT changes()", null).use { cursor ->
            if (cursor.moveToNext())
                cursor.getInt(0)
            else
                0
        }
    }

    private val sqliteUntrackedRowsQuery: SqliteQueryWrapper = SqliteQueryWrapper(
        database,dateFormat
    ) {
        val stringBuilder = StringBuilder(1000)
        val str1 = StringBuilder()
        val str2 = StringBuilder()
        val str3 = StringBuilder()
        val str4 = SqliteManagementUtils.joinTwoTablesOnClause(
            this.tableDescription.primaryKeys,
            "[side]",
            "[base]"
        )

        stringBuilder.appendLine("INSERT INTO ${trackingName.schema().quoted()} (")


        var comma = ""
        for (pkeyColumn in tableDescription.getPrimaryKeysColumns()) {
            val pkeyColumnName = ParserName.parse(pkeyColumn).quoted().toString()

            str1.append("${comma}${pkeyColumnName}")
            str2.append("${comma}[base].${pkeyColumnName}")
            str3.append("${comma}[side].${pkeyColumnName}")

            comma = ", "
        }
        stringBuilder.append(str1)
        stringBuilder.appendLine(", [update_scope_id], [sync_row_is_tombstone], [timestamp], [last_change_datetime]")
        stringBuilder.appendLine(")")
        stringBuilder.append("SELECT ")
        stringBuilder.append(str2)
        stringBuilder.appendLine(", NULL, 0, ${SqliteTableBuilder.TimestampValue}, datetime('now')")
        stringBuilder.appendLine(
            "FROM ${
                tableName.schema().quoted()
            } as [base] WHERE NOT EXISTS"
        )
        stringBuilder.append("(SELECT ")
        stringBuilder.append(str3)
        stringBuilder.appendLine(" FROM ${trackingName.schema().quoted()} as [side] ")
        stringBuilder.appendLine("WHERE ${str4})")

        return@SqliteQueryWrapper stringBuilder.toString()
    }

    override fun updateUntrackedRows(): Int =  sqliteUntrackedRowsQuery.executeStatement()

    override fun close() {
        initializeRowQuery.close()
        initializeRowTrackingQuery.close()
        sqliteRowQuery.close()
        sqliteRowTrackingQuery.close()
        sqliteMetadataQuery.close()
    }

    override fun getSelectInitializedChangesWithFilters(): SQLiteStatement =
        getSelectInitializedChanges()

    override fun getSelectChangesWithFilters(lastTimestamp: Long?): SQLiteStatement =
        getSelectChanges(lastTimestamp)

    private fun fillParametersFromColumns(
        parameters: MutableMap<String, Any?>,
        columns: List<SyncColumn>,
        row: SyncRow,
        stringOnly: Boolean = false
    ) {
        val schemaTable = row.table
        for (column in columns) {
            val col = schemaTable.columns?.get(column.columnName)
            if (col != null) {
                val unquotedColumn = ParserName.parse(column).normalized().unquoted().toString()
                parameters["@$unquotedColumn"] =
                    if (stringOnly)
                        convertToString(col, row[col])
                    else
                        convertToParameter(col, row[col])
            }
        }
    }

    private fun convertToParameter(column: SyncColumn, value: Any?): Any? {
        if (value == null)
            return null

        return when (column.getDbType()) {
            DbType.Binary ->
                Base64.decode(value.toString())
            DbType.DateTime ->
//                dateFormat.parse(value.toString().replace("T", " "))
                value.toString().replace("T", " ")
            DbType.Guid ->
                value.toString().uppercase()
            else -> value
        }
    }

    private fun convertToString(column: SyncColumn, value: Any?): Any? {
        if (value == null)
            return null

        return when (column.getDbType()) {
            DbType.Binary ->
                "X'" + Base64.decode(value.toString())
                    .joinToString("") {
                        val x = it.toString(16)
                        if (x.length < 2)
                            "0$x"
                        else
                            x
                    }
                    .uppercase() + "'"
            DbType.DateTime ->
                value.toString().replace("T", " ")
            DbType.Guid ->
                value.toString().uppercase()
            else -> value
        }
    }
}
