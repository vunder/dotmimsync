package com.mimetis.dotmim.sync.sqlite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.os.Build
import android.util.Base64
import com.mimetis.dotmim.sync.DbSyncAdapter
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.set.SyncRow
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.DbType
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class SqliteSyncAdapter(
    tableDescription: SyncTable,
    private val tableName: ParserName,
    private val trackingName: ParserName,
    setup: SyncSetup,
    private val database: SQLiteDatabase
) : DbSyncAdapter(tableDescription, setup) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var getSelectChangesSql = ""
    private var getSelectRowSql = ""
    private var deleteRowSql = ""
    private var deleteRowChangesSql = ""
    private var initializeRowSql = ""
    private var initializeRowStatement: SQLiteStatement? = null
    private var initializeRowOrder: List<String>? = null
    private var initializeRowChangesSql = ""
    private var updateRowSql = ""
    private var updateRowStatement: SQLiteStatement? = null
    private var updateRowOrder: List<String>? = null
    private var updateRowChangesSql = ""
    private var updateMetadataSql = ""
    private var updateUntrackedRowsSql = ""

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

    override fun getSelectRow(primaryKeyRow: SyncRow): Cursor {
        if (getSelectRowSql.isEmpty()) {
            val stringBuilder = StringBuilder("SELECT ")
            stringBuilder.appendLine()
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

            getSelectRowSql = stringBuilder.toString()
        }

        val parameters = mutableMapOf<String, Any?>()
        fillParametersFromColumns(
            parameters,
            this.tableDescription.getPrimaryKeysColumns().filter { c -> !c.isReadOnly },
            primaryKeyRow,
            stringOnly = true
        )

        val f = processSqlWithArgs(getSelectRowSql, parameters)
        return database.rawQuery(f.first, f.second.map { it.toString() }.toTypedArray())
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

    override fun deleteRow(
        scopeId: UUID?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int {
        if (deleteRowSql.isEmpty()) {
            val stringBuilder = StringBuilder()
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

            deleteRowSql = stringBuilder.toString()
        }

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

        val op1 = database.executeNonQuery(deleteRowSql, parameters)

        if (deleteRowChangesSql.isEmpty()) {
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

            deleteRowChangesSql = stringBuilder.toString()
        }

        return op1 + database.executeNonQuery(deleteRowChangesSql, parameters)
    }

    override fun initializeRow(
        scopeId: UUID?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int {
        if (initializeRowSql.isEmpty()) {
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

            initializeRowSql = stringBuilder.toString()
        }

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

        if (initializeRowStatement == null) {
            val f = processSqlWithArgs(initializeRowSql, parameters)
            initializeRowStatement = database.compileStatement(f.first)
            initializeRowOrder = f.third
        }
        initializeRowStatement?.clearBindings()

        var index = 1
        initializeRowOrder?.forEach {
            when (val value = parameters[it]) {
                null, is Unit -> initializeRowStatement?.bindNull(index++)
                is String -> initializeRowStatement?.bindString(index++, value)
                is Byte -> initializeRowStatement?.bindLong(index++, value.toLong())
                is Int -> initializeRowStatement?.bindLong(index++, value.toLong())
                is Long -> initializeRowStatement?.bindLong(index++, value)
                is Boolean -> initializeRowStatement?.bindLong(index++, if (value) 1 else 0)
                is ByteArray -> initializeRowStatement?.bindBlob(index++, value)
                is Double -> initializeRowStatement?.bindDouble(index++, value)
                is Float -> initializeRowStatement?.bindDouble(index++, value.toDouble())
                is BigDecimal -> initializeRowStatement?.bindDouble(index++, value.toDouble())
                is UUID -> initializeRowStatement?.bindString(index++, value.toString().uppercase())
                is Date -> initializeRowStatement?.bindString(index++, dateFormat.format(value))
                else -> initializeRowStatement?.bindString(index++, value.toString())
            }
        }

        initializeRowStatement?.execute()

        val op1 = database.rawQuery("SELECT changes()", null).use { cursor ->
            if (cursor.moveToNext())
                cursor.getInt(0)
            else
                0
        }


//        val op1 = database.executeNonQuery(initializeRowSql, parameters)

        //stringBuilder.AppendLine($"INSERT OR REPLACE INTO {tableName.Quoted().ToString()}");
        //stringBuilder.AppendLine($"({stringBuilderArguments.ToString()})");
        //stringBuilder.AppendLine($"SELECT {stringBuilderParameters.ToString()} ");
        //stringBuilder.AppendLine($"FROM (SELECT {stringBuilderParametersValues.ToString()}) as [c]");
        //stringBuilder.AppendLine($"LEFT JOIN {trackingName.Quoted().ToString()} AS [side] ON {str1}");
        //stringBuilder.AppendLine($"LEFT JOIN {tableName.Quoted().ToString()} AS [base] ON {str2}");
        //stringBuilder.Append($"WHERE ({SqliteManagementUtils.WhereColumnAndParameters(this.TableDescription.PrimaryKeys, "[base]")} ");
        //stringBuilder.AppendLine($"AND ([side].[timestamp] < @sync_min_timestamp OR [side].[update_scope_id] = @sync_scope_id)) ");
        //stringBuilder.Append($"OR ({SqliteManagementUtils.WhereColumnIsNull(this.TableDescription.PrimaryKeys, "[base]")} ");
        //stringBuilder.AppendLine($"AND ([side].[timestamp] < @sync_min_timestamp OR [side].[timestamp] IS NULL)) ");
        //stringBuilder.Append($"OR @sync_force_write = 1");
        //stringBuilder.AppendLine($";");


        //stringBuilder.AppendLine($"INSERT OR REPLACE INTO {tableName.Quoted().ToString()}");
        //stringBuilder.AppendLine($"({stringBuilderArguments.ToString()})");
        //stringBuilder.AppendLine($"SELECT {stringBuilderParameters.ToString()} ");
        //stringBuilder.AppendLine($"FROM (SELECT {stringBuilderParametersValues.ToString()}) as [c]");
        //stringBuilder.AppendLine($"LEFT JOIN {trackingName.Quoted().ToString()} AS [side] ON {str1}");

        if (initializeRowChangesSql.isEmpty()) {
            val stringBuilder = StringBuilder(500)
            stringBuilder.clear()
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

            initializeRowChangesSql = stringBuilder.toString()
        }

        return op1 + database.executeNonQuery(initializeRowChangesSql, parameters)
    }

    override fun updateRow(
        scopeId: UUID?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int {
        if (updateRowSql.isEmpty()) {
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

            updateRowSql = stringBuilder.toString()
        }

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

        if (updateRowStatement == null) {
            val f = processSqlWithArgs(updateRowSql, parameters)
            updateRowStatement = database.compileStatement(f.first)
            updateRowOrder = f.third
        }
        updateRowStatement?.clearBindings()

        var index = 1
        updateRowOrder?.forEach {
            when (val value = parameters[it]) {
                null, is Unit -> updateRowStatement?.bindNull(index++)
                is String -> updateRowStatement?.bindString(index++, value)
                is Byte -> updateRowStatement?.bindLong(index++, value.toLong())
                is Int -> updateRowStatement?.bindLong(index++, value.toLong())
                is Long -> updateRowStatement?.bindLong(index++, value)
                is Boolean -> updateRowStatement?.bindLong(index++, if (value) 1 else 0)
                is ByteArray -> updateRowStatement?.bindBlob(index++, value)
                is Double -> updateRowStatement?.bindDouble(index++, value)
                is Float -> updateRowStatement?.bindDouble(index++, value.toDouble())
                is BigDecimal -> updateRowStatement?.bindDouble(index++, value.toDouble())
                is UUID -> updateRowStatement?.bindString(index++, value.toString().uppercase())
                is Date -> updateRowStatement?.bindString(index++, dateFormat.format(value))
                else -> updateRowStatement?.bindString(index++, value.toString())
            }
        }

        updateRowStatement?.execute()

        val op1 = database.rawQuery("SELECT changes()", null).use { cursor ->
            if (cursor.moveToNext())
                cursor.getInt(0)
            else
                0
        }


//        val op1 = database.executeNonQuery(updateRowSql, parameters)

        if (updateRowChangesSql.isEmpty()) {
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

            updateRowChangesSql = stringBuilder.toString()
        }

        return op1 + database.executeNonQuery(updateRowChangesSql, parameters)
    }

    override fun updateMetadata(
        scopeId: UUID?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int {
        if (updateMetadataSql.isEmpty()) {
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
                pkeyAliasSelectForInsert.append("{comma}@${parameterName} as $columnName")
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

            updateMetadataSql = stringBuilder.toString()
        }

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

        return database.executeNonQuery(updateMetadataSql, parameters)
    }

    override fun deleteMetadata(timestamp: Long): Int =
        database.executeNonQuery("DELETE FROM ${trackingName.quoted()} WHERE [timestamp] < $timestamp;")

    override fun updateUntrackedRows(): Int {
        if (updateUntrackedRowsSql.isEmpty()) {
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

            updateUntrackedRowsSql = stringBuilder.toString()
        }

        return database.executeNonQuery(updateUntrackedRowsSql)
    }

    override fun close() {
        initializeRowStatement?.close()
    }

    override fun getSelectInitializedChangesWithFilters(): Cursor =
        getSelectInitializedChanges()

    override fun getSelectChangesWithFilters(lastTimestamp: Long?): Cursor =
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
                Base64.decode(value.toString(), Base64.NO_WRAP)
            DbType.DateTime ->
                dateFormat.parse(value.toString().replace("T", " "))
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
                "X'" + Base64.decode(value.toString(), Base64.NO_WRAP)
                    .joinToString("") {
                        val x = it.toString(16)
                        if (x.length < 2)
                            "0$x"
                        else
                            x
                    }
                    .uppercase(Locale.getDefault()) + "'"
            DbType.DateTime ->
                value.toString().replace("T", " ")
            DbType.Guid ->
                value.toString().uppercase()
            else -> value
        }
    }

    private fun processSqlWithArgs(
        query: String,
        parameters: Map<String, Any?>
    ): Triple<String, List<Any?>, List<String>> {
        val bindArgs = mutableListOf<Any?>()
        val bindOrder = mutableListOf<String>()
        var sql = query
        do {
            val startIndex = sql.indexOf("@")
            if (startIndex >= 0) {
                var index = startIndex + 1
                while (index < sql.length && (sql[index].isLetterOrDigit() || sql[index] == '_'))
                    ++index
                index = min(index, sql.length)
                val name = sql.substring(startIndex, index)
                val value = parameters[name]
                bindArgs.add(value)
                bindOrder.add(name)
//                    sql =
//                        "${
//                            sql.substring(
//                                0,
//                                startIndex
//                            )
//                        }${value.asSqlValue()}${sql.substring(index)}"
                sql = "${sql.substring(0, startIndex)}?${sql.substring(index)}"
            }
        } while (startIndex >= 0)

        return Triple(sql, bindArgs, bindOrder)
    }

    private fun SQLiteDatabase.executeNonQuery(
        query: String,
        parameters: Map<String, Any?>? = null
    ): Int {
        if (parameters?.isNotEmpty() == true) {
            val f = processSqlWithArgs(query, parameters)
            this.compileStatement(f.first).apply {
                var index = 1
                f.second.forEach {
                    when (it) {
                        null, is Unit -> this.bindNull(index++)
                        is String -> this.bindString(index++, it)
                        is Byte -> this.bindLong(index++, it.toLong())
                        is Int -> this.bindLong(index++, it.toLong())
                        is Long -> this.bindLong(index++, it)
                        is Boolean -> this.bindLong(index++, if (it) 1 else 0)
                        is ByteArray -> this.bindBlob(index++, it)
                        is Double -> this.bindDouble(index++, it)
                        is Float -> this.bindDouble(index++, it.toDouble())
                        is BigDecimal -> this.bindDouble(index++, it.toDouble())
                        is UUID -> this.bindString(index++, it.toString().uppercase())
                        is Date -> this.bindString(index++, dateFormat.format(it))
                        else -> this.bindString(index++, it.toString())
                    }
                }
                this.execute()
            }
        } else {
            this.execSQL(query)
        }

        return this.rawQuery("SELECT changes()", null).use { cursor ->
            if (cursor.moveToNext())
                cursor.getInt(0)
            else
                0
        }
    }
}

//fun Any?.asSqlValue(): String {
//    if (this == null)
//        return "NULL"
//
//    return when (this) {
//        is Boolean -> if (this) "1" else "0"
//        is ByteArray -> "0x" + this
//            .joinToString("") {
//                val x = it.toString(16)
//                if (x.length < 2)
//                    "0$x"
//                else
//                    x
//            }
//            .uppercase(Locale.getDefault())
//        is String -> "'${this}'"
//        is UUID -> "'${this}'"
//        else -> this.toString()
//    }
//}
