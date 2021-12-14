package com.mimetis.dotmim.sync.sqlite

import android.database.sqlite.SQLiteDatabase
import com.mimetis.dotmim.sync.builders.DbTableBuilder
import com.mimetis.dotmim.sync.builders.DbTriggerType
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.manager.DbRelationColumnDefinition
import com.mimetis.dotmim.sync.manager.DbRelationDefinition
import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.DbType
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.util.*
import kotlin.collections.ArrayList

class SqliteTableBuilder(
    tableDescription: SyncTable,
    tableName: ParserName,
    trackingTableName: ParserName,
    setup: SyncSetup,
    private val database: SQLiteDatabase
) : DbTableBuilder(tableDescription, tableName, trackingTableName, setup) {
    override fun existsTable(): Boolean {
        val tbl = this.tableName.unquoted().toString()
        database.rawQuery(
            "select count(name) from sqlite_master where name = ? AND type='table'",
            arrayOf(tbl)
        ).use { cursor ->
            return cursor.moveToNext() && cursor.getInt(0) > 0
        }
    }

    override fun getColumns(): List<SyncColumn> {
        val columns = ArrayList<SyncColumn>()
        val columnsList = getColumnsForTable(this.tableName.unquoted().toString())

        val sqlDbMetadata = SqliteDbMetadata()
        for (c in columnsList.rows.sortedBy { r -> r["cid"] as Int }) {
            val typeName = c["type"].toString()
            val name = c["name"].toString()

//            // Gets the datastore owner dbType
//            val datastoreDbType = sqlDbMetadata.validateOwnerDbType(
//                typeName,
//                isUnsigned = false,
//                isUnicode = false,
//                maxLength = 0
//            ) as SqliteType
//
//            // once we have the datastore type, we can have the managed type
//            val columnType = sqlDbMetadata.validateType(datastoreDbType)

            val sColumn = SyncColumn(name).apply {
                originalDbType = typeName
                ordinal = c["cid"] as Int
                originalTypeName = c["type"].toString()
                allowDBNull = c["notnull"] == 0
                defaultValue = c["dflt_value"].toString()

                // No unsigned type in SQLite
                isUnsigned = false
            }

            columns.add(sColumn)
        }

        return columns
    }

    override fun getPrimaryKeys(): List<SyncColumn> {
        val unquotedTableName = this.tableName.unquoted().toString()
        val keys = SyncTable(unquotedTableName, "")
        database.rawQuery(
            // may not work on old Android
            //"SELECT * FROM pragma_table_info('${unquotedTableName}') where pk = 1;",
            "pragma table_info('${unquotedTableName}');",
            null
        ).use { cursor ->
            val pkIndex = cursor.getColumnIndex("pk")
            keys.load(cursor) { c -> c.getInt(pkIndex) == 1 }
        }

        val lstKeys = ArrayList<SyncColumn>()

        for (key in keys.rows) {
            val keyColumn = SyncColumn.create<String>(key["name"] as String)
            keyColumn.ordinal = key["cid"] as Int
            lstKeys.add(keyColumn)
        }

        return lstKeys
    }

    override fun getRelations(): List<DbRelationDefinition> {
        val relations = ArrayList<DbRelationDefinition>()

        val unquotedTableName = this.tableName.unquoted().toString()
        val relationsTable =
        // may not work on old Android
//            database.rawQuery("SELECT * FROM pragma_foreign_key_list('$unquotedTableName')", null)
            database.rawQuery("pragma foreign_key_list('$unquotedTableName')", null)
                .use { cursor ->
                    SyncTable(unquotedTableName).apply {
                        load(cursor)
                    }
                }

        if (relationsTable.rows.isNotEmpty()) {
            for (fk in relationsTable.rows.groupBy { row ->
                GroupKey(
                    name = row["id"].toString(),
                    tableName = this.tableName.quoted().toString(),
                    referenceTableName = row["table"] as String
                )
            }) {
                val relationDefinition = DbRelationDefinition().apply {
                    foreignKey = fk.key.name
                    tableName = fk.key.tableName
                    referenceTableName = fk.key.referenceTableName
                }
                relationDefinition.columns.addAll(fk.value.map { dmRow ->
                    DbRelationColumnDefinition().apply {
                        keyColumnName = dmRow["from"].toString()
                        referenceColumnName = dmRow["to"].toString()
                        order = dmRow["seq"] as Int
                    }
                })

                relations.add(relationDefinition)
            }
        }

        return relations.sortedBy { t -> t.foreignKey }
    }

    override fun createTable() {
        val stringBuilder = StringBuilder("CREATE TABLE IF NOT EXISTS ${this.tableName.quoted()} (")
        var empty = ""
        stringBuilder.appendLine()
        for (column in this.tableDescription.columns!!) {
            val columnName = ParserName.parse(column).quoted().toString()

            val columnTypeString = this.tryGetOwnerDbTypeString(
                column.originalDbType,
                column.getDbType(),
                isUnsigned = false,
                isUnicode = false,
                maxLength = column.maxLength,
                fromProviderType = this.tableDescription.originalProvider!!,
                ownerProviderType = SqliteSyncProvider.ProviderType
            )
            val columnPrecisionString = this.tryGetOwnerDbTypePrecision(
                column.originalDbType,
                column.getDbType(),
                isUnsigned = false,
                isUnicode = false,
                maxLength = column.maxLength,
                precision = column.precision,
                scale = column.scale,
                fromProviderType = this.tableDescription.originalProvider!!,
                ownerProviderType = SqliteSyncProvider.ProviderType
            )
            val columnType = "$columnTypeString $columnPrecisionString"

            // check case
            var casesensitive = ""
            if (this.isTextType(column.getDbType())) {
                casesensitive = ""//SyncGlobalization.IsCaseSensitive() ? "" : "COLLATE NOCASE";

                //check if it's a primary key, then, even if it's case sensitive, we turn on case insensitive
//                if (SyncGlobalization.IsCaseSensitive())
//                {
//                    if (this.TableDescription.PrimaryKeys.Contains(column.ColumnName))
//                        casesensitive = "COLLATE NOCASE";
//                }
            }

            var identity = ""

            if (column.isAutoIncrement) {
                val (step, seed) = column.getAutoIncrementSeedAndStep()
                if (seed > 1 || step > 1)
                    throw Exception("NotSupportedException: can't establish a seed / step in Sqlite autoinc value")

                //identity = $"AUTOINCREMENT"
                // Actually no need to set AutoIncrement, if we insert a null value
                identity = ""
            }
            var nullString = if (column.allowDBNull) "NULL" else "NOT NULL"

            // if auto inc, don't specify NOT NULL option, since we need to insert a null value to make it auto inc.
            if (column.isAutoIncrement)
                nullString = ""
            // if it's a readonly column, it could be a computed column, so we need to allow null
            else if (column.isReadOnly)
                nullString = "NULL"

            stringBuilder.appendLine("\t${empty}${columnName} $columnType $identity $nullString $casesensitive")
            empty = ","
        }
        stringBuilder.append("\t,PRIMARY KEY (")
        for (i in 0 until this.tableDescription.primaryKeys.size) {
            val pkColumn = this.tableDescription.primaryKeys[i]
            val quotedColumnName = ParserName.parse(pkColumn).quoted().toString()

            stringBuilder.append(quotedColumnName)

            if (i < this.tableDescription.primaryKeys.size - 1)
                stringBuilder.append(", ")
        }
        stringBuilder.append(")")

        // Constraints
        for (constraint in this.tableDescription.getRelations()) {
            // Don't want foreign key on same table since it could be a problem on first
            // sync. We are not sure that parent row will be inserted in first position
            if (constraint.getParentTable()!!.equalsByName(constraint.getTable()))
                continue

            val parentTable = constraint.getParentTable()!!
            val parentTableName = ParserName.parse(parentTable.tableName).quoted().toString()

            stringBuilder.appendLine()
            stringBuilder.append("\tFOREIGN KEY (")
            empty = ""
            for (column in constraint.keys) {
                val columnName = ParserName.parse(column.columnName).quoted().toString()
                stringBuilder.append("$empty $columnName")
                empty = ", "
            }
            stringBuilder.append(") ")
            stringBuilder.append("REFERENCES $parentTableName(")
            empty = ""
            for (column in constraint.parentKeys) {
                val columnName = ParserName.parse(column.columnName).quoted().toString()
                stringBuilder.append("$empty $columnName")
                empty = ", "
            }
            stringBuilder.appendLine(" )")
        }
        stringBuilder.append(")")
        val createTableCommandString = stringBuilder.toString()

        database.execSQL(createTableCommandString)
    }

    override fun existsTrackingTable(): Boolean {
        val tbl = this.trackingTableName.unquoted().toString()
        database.rawQuery(
            "select count(*) from sqlite_master where name = ? AND type='table'",
            arrayOf(tbl)
        ).use { cursor ->
            return cursor.moveToNext() && cursor.getInt(0) > 0
        }
    }

    override fun createTrackingTable(): Boolean {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("CREATE TABLE IF NOT EXISTS ${this.trackingTableName.quoted()} (")

        // Adding the primary key
        for (pkColumn in this.tableDescription.getPrimaryKeysColumns()) {
            val quotedColumnName = ParserName.parse(pkColumn).quoted().toString()

            val columnTypeString = this.tryGetOwnerDbTypeString(
                pkColumn.originalDbType,
                pkColumn.getDbType(),
                isUnsigned = false,
                isUnicode = false,
                maxLength = pkColumn.maxLength,
                fromProviderType = this.tableDescription.originalProvider!!,
                ownerProviderType = SqliteSyncProvider.ProviderType
            )
            val columnPrecisionString = this.tryGetOwnerDbTypePrecision(
                pkColumn.originalDbType,
                pkColumn.getDbType(),
                isUnsigned = false,
                isUnicode = false,
                maxLength = pkColumn.maxLength,
                precision = pkColumn.precision,
                scale = pkColumn.scale,
                fromProviderType = this.tableDescription.originalProvider!!,
                ownerProviderType = SqliteSyncProvider.ProviderType
            )
            var quotedColumnType = ParserName.parse(columnTypeString).quoted().toString()
            quotedColumnType += columnPrecisionString

            stringBuilder.appendLine("$quotedColumnName $quotedColumnType NOT NULL COLLATE NOCASE, ")
        }

        // adding the tracking columns
        stringBuilder.appendLine("[update_scope_id] [text] NULL COLLATE NOCASE, ")
        stringBuilder.appendLine("[timestamp] [integer] NULL, ")
        stringBuilder.appendLine("[sync_row_is_tombstone] [integer] NOT NULL default(0), ")
        stringBuilder.appendLine("[last_change_datetime] [datetime] NULL, ")

        stringBuilder.append(" PRIMARY KEY (")
        for (i in 0 until this.tableDescription.primaryKeys.size) {
            val pkColumn = this.tableDescription.primaryKeys[i]
            val quotedColumnName = ParserName.parse(pkColumn).quoted().toString()

            stringBuilder.append(quotedColumnName)

            if (i < this.tableDescription.primaryKeys.size - 1)
                stringBuilder.append(", ")
        }
        stringBuilder.append(")")


        stringBuilder.append(");")

        stringBuilder.appendLine(
            "CREATE INDEX IF NOT EXISTS [${
                this.trackingTableName.schema().unquoted().normalized()
            }_timestamp_index] ON ${this.trackingTableName.schema().quoted()} ("
        )
        stringBuilder.appendLine("\t [timestamp] ASC")
        stringBuilder.appendLine("\t,[update_scope_id] ASC")
        stringBuilder.appendLine("\t,[sync_row_is_tombstone] ASC")
        for (pkColumn in this.tableDescription.getPrimaryKeysColumns()) {
            val columnName = ParserName.parse(pkColumn).quoted().toString()
            stringBuilder.appendLine("\t,$columnName ASC")
        }
        stringBuilder.append(");")

        database.execSQL(stringBuilder.toString())

        return true
    }

    override fun existsTrigger(triggerType: DbTriggerType): Boolean {
        val commandTriggerName = this.getTriggerCommandName(triggerType)
        val triggerName = ParserName.parse(commandTriggerName).toString()

        database.rawQuery(
            "select count(*) from sqlite_master where name = ? AND type='trigger'",
            arrayOf(triggerName)
        ).use { cursor ->
            return cursor.moveToNext() && cursor.getInt(0) > 0
        }
    }

    override fun dropTrigger(triggerType: DbTriggerType) {
        val commandTriggerName = this.getTriggerCommandName(triggerType)
        val triggerName = ParserName.parse(commandTriggerName).toString()

        database.execSQL("drop trigger if exists $triggerName")
    }

    override fun createTrigger(triggerType: DbTriggerType) =
        when (triggerType) {
            DbTriggerType.Insert -> createInsertTrigger()
            DbTriggerType.Update -> createUpdateTrigger()
            DbTriggerType.Delete -> createDeleteTrigger()
        }

    override fun existsTriggerCommand(triggerType: DbTriggerType): Boolean {
        val commandTriggerName =
            String.format(getTriggerCommandName(triggerType), tableName.unquoted().toString())
        val triggerName = ParserName.parse(commandTriggerName).toString()

        return database.rawQuery(
            "select count(*) from sqlite_master where name = @triggerName AND type='trigger'",
            arrayOf(triggerName)
        ).use { cursor ->
            cursor.moveToNext() && cursor.getInt(0) > 0
        }
    }

    override fun dropTrackingTable() {
        database.execSQL("drop table if exists ${this.trackingTableName.quoted()}")
    }

    override fun dropTable() {
        database.execSQL("drop table if exists ${this.tableName.quoted()}")
    }

    private lateinit var triggersNames: Map<DbTriggerType, String>

    private fun initTriggerNames() {
        val tpref = if (this.setup.triggersPrefix.isNotBlank()) this.setup.triggersPrefix else ""
        val tsuf = if (this.setup.triggersSuffix.isNotBlank()) this.setup.triggersSuffix else ""

        triggersNames = hashMapOf(
            DbTriggerType.Insert to String.format(
                "[%s_insert_trigger]",
                "${tpref}${tableName.unquoted().normalized()}${tsuf}"
            ),
            DbTriggerType.Update to String.format(
                "[%s_update_trigger]",
                "${tpref}${tableName.unquoted().normalized()}${tsuf}"
            ),
            DbTriggerType.Delete to String.format(
                "[%s_delete_trigger]",
                "${tpref}${tableName.unquoted().normalized()}${tsuf}"
            )
        )
    }

    private fun getTriggerCommandName(objectType: DbTriggerType): String {
        if (!triggersNames.containsKey(objectType))
            throw Exception("Yous should provide a value for all DbCommandName")

        val commandName = triggersNames[objectType]!!

        //// concat filter name
        //if (filter != null)
        //    commandName = string.Format(commandName, filter.GetFilterName());

        return commandName
    }

    private class GroupKey(
        val name: String,
        val tableName: String,
        val referenceTableName: String
    )

    private fun getColumnsForTable(unquotedTableName: String): SyncTable {
        //may not work on old Android
//        val commandColumn = "SELECT * FROM pragma_table_info('$unquotedTableName');"
        val commandColumn = "pragma table_info('$unquotedTableName');"
        val syncTable = SyncTable(unquotedTableName, "")
        database.rawQuery(commandColumn, null).use { cursor -> syncTable.load(cursor) }
        return syncTable
    }

    private fun isTextType(dbType: DbType): Boolean =
        dbType == DbType.AnsiString || dbType == DbType.AnsiStringFixedLength ||
                dbType == DbType.String || dbType == DbType.StringFixedLength ||
                dbType == DbType.Xml

    private fun tryGetOwnerDbTypeString(
        originalDbType: String, fallbackDbType: DbType, isUnsigned: Boolean,
        isUnicode: Boolean, maxLength: Int, fromProviderType: String, ownerProviderType: String
    ): String {
        // We MUST check if we are from the same provider (if it's mysql or oracle, we fallback on dbtype
        if (originalDbType.isNotBlank() && fromProviderType == ownerProviderType) {
            val ownedDbType = validateOwnerDbType(originalDbType, isUnsigned, isUnicode, maxLength)
            return getStringFromOwnerDbType(DbType.values()[ownedDbType.ordinal])
        }

        // if it's not the same provider, fallback on DbType instead.
        return getStringFromDbType(fallbackDbType)
    }

    private fun tryGetOwnerDbTypePrecision(
        originalDbType: String,
        fallbackDbType: DbType,
        isUnsigned: Boolean,
        isUnicode: Boolean,
        maxLength: Int,
        precision: Byte,
        scale: Byte,
        fromProviderType: String,
        ownerProviderType: String
    ): String {
        // We MUST check if we are from the same provider (if it's mysql or oracle, we fallback on dbtype
        if (originalDbType.isNotBlank() && fromProviderType == ownerProviderType) {
            val ownedDbType = validateOwnerDbType(originalDbType, isUnsigned, isUnicode, maxLength)
            return getPrecisionStringFromOwnerDbType(ownedDbType, maxLength, precision, scale)
        }

        // if it's not the same provider, fallback on DbType instead.
        return getPrecisionStringFromDbType(fallbackDbType, maxLength, precision, scale)
    }

    private fun getStringFromDbType(dbType: DbType): String =
        when (dbType) {
            DbType.AnsiString,
            DbType.AnsiStringFixedLength,
            DbType.String,
            DbType.StringFixedLength,
            DbType.Xml,
            DbType.Time,
            DbType.DateTimeOffset ->
                "text"
            DbType.Guid,
            DbType.Binary,
            DbType.Object ->
                "blob"
            DbType.Boolean,
            DbType.Byte,
            DbType.Int16,
            DbType.Int32,
            DbType.UInt16,
            DbType.Int64,
            DbType.UInt32,
            DbType.UInt64,
            DbType.SByte ->
                "integer"
            DbType.Date,
            DbType.DateTime,
            DbType.DateTime2 ->
                "datetime"
            DbType.Decimal,
            DbType.Double,
            DbType.Single,
            DbType.Currency,
            DbType.VarNumeric ->
                "real"
            else ->
                throw  Exception("this DbType ${dbType} is not supported")
        }

    private fun getStringFromOwnerDbType(ownerType: Any): String {
        val dbType = validateDbType(ownerType.toString(), true, true, 0)
        return this.getStringFromDbType(dbType)
    }

    private fun validateDbType(
        typeName: String,
        isUnsigned: Boolean,
        isUnicode: Boolean,
        maxLength: Long
    ): DbType {
        var typeName = typeName
        if (typeName.contains("("))
            typeName = typeName.substring(0, typeName.indexOf("("));

        when (typeName.lowercase()) {
            "bit" ->
                return DbType.Boolean
            "integer",
            "bigint" ->
                return DbType.Int64
            "numeric",
            "real",
            "float" ->
                return DbType.Double
            "decimal" ->
                return DbType.Decimal
            "blob",
            "image" ->
                return DbType.Binary
            "datetime" ->
                return DbType.DateTime
            "time" ->
                return DbType.Time
            "text",
            "varchar" ->
                return DbType.String

        }
        throw Exception("this type name $typeName is not supported")
    }

    private fun validateOwnerDbType(
        typeName: String,
        isUnsigned: Boolean,
        isUnicode: Boolean,
        maxLength: Int
    ): SqliteType {
        var tn = typeName
        if (tn.contains("("))
            tn = tn.substring(0, tn.indexOf("("));

        when (tn.lowercase(Locale.getDefault())) {
            "bit",
            "integer",
            "bigint" ->
                return SqliteType.Integer
            "numeric",
            "decimal",
            "real" ->
                return SqliteType.Real
            "blob",
            "image" ->
                return SqliteType.Blob
            "datetime",
            "text" ->
                return SqliteType.Text

        }
        throw Exception("this type name $typeName is not supported")
    }

    private fun getPrecisionStringFromDbType(
        dbType: Any,
        maxLength: Int,
        precision: Byte,
        scale: Byte
    ): String =
        ""

    private fun getPrecisionStringFromOwnerDbType(
        dbType: Any,
        maxLength: Int,
        precision: Byte,
        scale: Byte
    ): String = getPrecisionStringFromDbType(dbType as DbType, maxLength, precision, scale)

    private fun createInsertTrigger() {
        val insTriggerName = getTriggerCommandName(DbTriggerType.Insert)
        val createTrigger =
            StringBuilder("CREATE TRIGGER IF NOT EXISTS $insTriggerName AFTER INSERT ON ${tableName.quoted()} ")
        createTrigger.appendLine()

        val stringBuilderArguments = StringBuilder()
        val stringBuilderArguments2 = StringBuilder()
        val stringPkAreNull = StringBuilder()
        var argComma = ""
        var argAnd = ""

        createTrigger.appendLine()
        createTrigger.appendLine("BEGIN")
        createTrigger.appendLine("-- If row was deleted before, it already exists, so just make an update")

        createTrigger.appendLine("\tINSERT OR REPLACE INTO ${this.trackingTableName.quoted()} (")
        for (mutableColumn in this.tableDescription.getPrimaryKeysColumns()
            .filter { c -> !c.isReadOnly }) {
            val columnName = ParserName.parse(mutableColumn).quoted().toString()

            stringBuilderArguments.appendLine("\t\t${argComma}${columnName}")
            stringBuilderArguments2.appendLine("\t\t${argComma}new.${columnName}")
            stringPkAreNull.append("${argAnd}${trackingTableName.quoted()}.$columnName IS NULL")
            argComma = ","
            argAnd = " AND "
        }

        createTrigger.append(stringBuilderArguments.toString())
        createTrigger.appendLine("\t\t,[update_scope_id]")
        createTrigger.appendLine("\t\t,[timestamp]")
        createTrigger.appendLine("\t\t,[sync_row_is_tombstone]")
        createTrigger.appendLine("\t\t,[last_change_datetime]")

        createTrigger.appendLine("\t) ")
        createTrigger.appendLine("\tVALUES (")
        createTrigger.append(stringBuilderArguments2.toString())
        createTrigger.appendLine("\t\t,NULL")
        createTrigger.appendLine("\t\t,$TimestampValue")
        createTrigger.appendLine("\t\t,0")
        createTrigger.appendLine("\t\t,datetime('now')")
        createTrigger.appendLine("\t);")
        createTrigger.appendLine("END;")

        database.execSQL(createTrigger.toString())
    }

    private fun createUpdateTrigger() {
        val updTriggerName = getTriggerCommandName(DbTriggerType.Update)

        val createTrigger =
            StringBuilder("CREATE TRIGGER IF NOT EXISTS $updTriggerName AFTER UPDATE ON ${tableName.quoted()} ")
        createTrigger.appendLine()


        createTrigger.appendLine()
        createTrigger.appendLine("Begin ")

        createTrigger.appendLine("\tUPDATE ${trackingTableName.quoted()} ")
        createTrigger.appendLine("\tSET [update_scope_id] = NULL -- scope id is always NULL when update is made locally")
        createTrigger.appendLine("\t\t,[timestamp] = $TimestampValue")
        createTrigger.appendLine("\t\t,[last_change_datetime] = datetime('now')")

        createTrigger.append("\tWhere ")
        createTrigger.append(
            SqliteManagementUtils.joinTwoTablesOnClause(
                this.tableDescription.primaryKeys,
                trackingTableName.quoted().toString(),
                "new"
            )
        )

        if (this.tableDescription.getMutableColumns().isNotEmpty()) {
            createTrigger.appendLine()
            createTrigger.appendLine("\t AND (")
            var or = "    "
            for (column in this.tableDescription.getMutableColumns()) {
                val quotedColumn = ParserName.parse(column).quoted().toString()

                createTrigger.append("\t")
                createTrigger.append(or)
                createTrigger.append("IFNULL(")
                createTrigger.append("NULLIF(")
                createTrigger.append("[old].")
                createTrigger.append(quotedColumn)
                createTrigger.append(", ")
                createTrigger.append("[new].")
                createTrigger.append(quotedColumn)
                createTrigger.append(")")
                createTrigger.append(", ")
                createTrigger.append("NULLIF(")
                createTrigger.append("[new].")
                createTrigger.append(quotedColumn)
                createTrigger.append(", ")
                createTrigger.append("[old].")
                createTrigger.append(quotedColumn)
                createTrigger.append(")")
                createTrigger.appendLine(") IS NOT NULL")

                or = " OR "
            }
            createTrigger.appendLine("\t ) ")
        }

        createTrigger.appendLine("; ")


        val stringBuilderArguments = StringBuilder()
        val stringBuilderArguments2 = StringBuilder()
        val stringPkAreNull = StringBuilder()
        var argComma = ""
        var argAnd = ""

        createTrigger.appendLine("\tINSERT OR IGNORE INTO ${trackingTableName.quoted()} (")
        for (mutableColumn in this.tableDescription.getPrimaryKeysColumns()
            .filter { c -> !c.isReadOnly }) {
            val columnName = ParserName.parse(mutableColumn).quoted().toString()

            stringBuilderArguments.appendLine("\t\t${argComma}${columnName}")
            stringBuilderArguments2.appendLine("\t\t${argComma}new.${columnName}")
            stringPkAreNull.append("${argAnd}${trackingTableName.quoted()}.${columnName} IS NULL")
            argComma = ","
            argAnd = " AND "
        }

        createTrigger.append(stringBuilderArguments.toString())
        createTrigger.appendLine("\t\t,[update_scope_id]")
        createTrigger.appendLine("\t\t,[timestamp]")
        createTrigger.appendLine("\t\t,[sync_row_is_tombstone]")
        createTrigger.appendLine("\t\t,[last_change_datetime]")

        createTrigger.appendLine("\t) ")
        createTrigger.appendLine("\tSELECT ")
        createTrigger.append(stringBuilderArguments2.toString())
        createTrigger.appendLine("\t\t,NULL")
        createTrigger.appendLine("\t\t,${TimestampValue}")
        createTrigger.appendLine("\t\t,0")
        createTrigger.appendLine("\t\t,datetime('now')")

        createTrigger.append("\tWHERE (SELECT COUNT(*) FROM ${trackingTableName.quoted()} WHERE ")
        val pkeys = this.tableDescription.getPrimaryKeysColumns()
        var str1 = ""
        for (pkey in pkeys) {
            val quotedColumn = ParserName.parse(pkey).quoted().toString()
            createTrigger.append("${str1}${quotedColumn}=new.${quotedColumn}")
            str1 = " AND "
        }
        createTrigger.appendLine(")=0")
        if (this.tableDescription.getMutableColumns().isNotEmpty()) {
            createTrigger.appendLine("\t AND (")
            var or = "    "
            for (column in this.tableDescription.getMutableColumns()) {
                val quotedColumn = ParserName.parse(column).quoted().toString()

                createTrigger.append("\t")
                createTrigger.append(or)
                createTrigger.append("IFNULL(")
                createTrigger.append("NULLIF(")
                createTrigger.append("[old].")
                createTrigger.append(quotedColumn)
                createTrigger.append(", ")
                createTrigger.append("[new].")
                createTrigger.append(quotedColumn)
                createTrigger.append(")")
                createTrigger.append(", ")
                createTrigger.append("NULLIF(")
                createTrigger.append("[new].")
                createTrigger.append(quotedColumn)
                createTrigger.append(", ")
                createTrigger.append("[old].")
                createTrigger.append(quotedColumn)
                createTrigger.append(")")
                createTrigger.appendLine(") IS NOT NULL")

                or = " OR "
            }
            createTrigger.appendLine("\t ) ")
        }

        createTrigger.appendLine("; ")

        createTrigger.appendLine("End; ")

        database.execSQL(createTrigger.toString())
    }

    private fun createDeleteTrigger() {
        val delTriggerName = getTriggerCommandName(DbTriggerType.Delete)

        val createTrigger =
            StringBuilder("CREATE TRIGGER IF NOT EXISTS $delTriggerName AFTER DELETE ON ${tableName.quoted()} ")
        createTrigger.appendLine()

        val stringBuilderArguments = StringBuilder()
        val stringBuilderArguments2 = StringBuilder()
        val stringPkAreNull = StringBuilder()
        var argComma = ""
        var argAnd = ""

        createTrigger.appendLine()
        createTrigger.appendLine("BEGIN")

        createTrigger.appendLine("\tINSERT OR REPLACE INTO ${trackingTableName.quoted()} (")
        for (mutableColumn in this.tableDescription.getPrimaryKeysColumns()
            .filter { c -> !c.isReadOnly }) {
            val columnName = ParserName.parse(mutableColumn).quoted().toString()

            stringBuilderArguments.appendLine("\t\t${argComma}${columnName}")
            stringBuilderArguments2.appendLine("\t\t${argComma}old.${columnName}")
            stringPkAreNull.append("${argAnd}${trackingTableName.quoted()}.${columnName} IS NULL")
            argComma = ","
            argAnd = " AND "
        }

        createTrigger.append(stringBuilderArguments.toString())
        createTrigger.appendLine("\t\t,[update_scope_id]")
        createTrigger.appendLine("\t\t,[timestamp]")
        createTrigger.appendLine("\t\t,[sync_row_is_tombstone]")
        createTrigger.appendLine("\t\t,[last_change_datetime]")

        createTrigger.appendLine("\t) ")
        createTrigger.appendLine("\tVALUES (")
        createTrigger.append(stringBuilderArguments2.toString())
        createTrigger.appendLine("\t\t,NULL")
        createTrigger.appendLine("\t\t,${TimestampValue}")
        createTrigger.appendLine("\t\t,1")
        createTrigger.appendLine("\t\t,datetime('now')")
        createTrigger.appendLine("\t);")
        createTrigger.appendLine("END;")

        database.execSQL(createTrigger.toString())
    }

    override fun createSchema() {}

    override fun existsSchema(): Boolean = false

    override fun addColumn(columnName: String) {
        val stringBuilder = StringBuilder("ALTER TABLE ${this.tableName.quoted()} ADD COLUMN")

        val column = this.tableDescription.columns!![columnName]!!
        val columnNameString = ParserName.parse(column).quoted().toString()

        val sqliteDbMetadata = SqliteDbMetadata()
        val columnType = sqliteDbMetadata.getCompatibleColumnTypeDeclarationString(column, tableDescription.originalProvider ?: "")

        // check case
        var casesensitive = ""
        if (this.isTextType(column.getDbType()))
        {
            casesensitive = "COLLATE NOCASE"//SyncGlobalization.IsCaseSensitive() ? "" : "COLLATE NOCASE";

            //check if it's a primary key, then, even if it's case sensitive, we turn on case insensitive
//            if (SyncGlobalization.IsCaseSensitive())
//            {
//                if (this.TableDescription.PrimaryKeys.Contains(column.ColumnName))
//                    casesensitive = "COLLATE NOCASE";
//            }
        }

        var identity = ""

        if (column.isAutoIncrement) {
            val step_seed = column.getAutoIncrementSeedAndStep()
            if (step_seed.first > 1 || step_seed.second > 1)
                throw UnsupportedOperationException("can't establish a seed / step in Sqlite autoinc value")

            //identity = $"AUTOINCREMENT";
            // Actually no need to set AutoIncrement, if we insert a null value
            identity = ""
        }
        var nullString = if (column.allowDBNull) "NULL" else "NOT NULL"

        // if auto inc, don't specify NOT NULL option, since we need to insert a null value to make it auto inc.
        if (column.isAutoIncrement)
            nullString = ""
        // if it's a readonly column, it could be a computed column, so we need to allow null
        else if (column.isReadOnly)
            nullString = "NULL"

        stringBuilder.appendLine(" $columnNameString $columnType $identity $nullString ${casesensitive};")

        database.execSQL(stringBuilder.toString())
    }

    override fun existsColumn(columnName: String): Boolean {
        database.rawQuery(
            // may not work on old Android
            //"SELECT * FROM pragma_table_info('${unquotedTableName}') where pk = 1;",
            "pragma table_info('${tableName.unquoted()}');",
            null
        ).use { cursor ->
            val index = cursor.getColumnIndex("name")
            if (index < 0)
                return false
            while (cursor.moveToNext())
                if (cursor.getString(index) == columnName)
                    return true
        }
        return false
    }

    init {
        initTriggerNames()
    }

    companion object {
        val TimestampValue = "replace(strftime('%Y%m%d%H%M%f', 'now'), '.', '')"
    }
}
