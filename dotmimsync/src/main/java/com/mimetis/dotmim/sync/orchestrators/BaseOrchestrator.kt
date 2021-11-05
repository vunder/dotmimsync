package com.mimetis.dotmim.sync.orchestrators

import android.database.Cursor
import com.mimetis.dotmim.sync.*
import com.mimetis.dotmim.sync.args.*
import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.builders.*
import com.mimetis.dotmim.sync.enumerations.*
import com.mimetis.dotmim.sync.interceptors.Interceptors
import com.mimetis.dotmim.sync.manager.DbRelationDefinition
import com.mimetis.dotmim.sync.messages.*
import com.mimetis.dotmim.sync.parameter.SyncParameters
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo
import com.mimetis.dotmim.sync.set.*
import com.mimetis.dotmim.sync.setup.SetupTable
import com.mimetis.dotmim.sync.setup.SyncSetup
import com.mimetis.dotmim.sync.sqlite.CursorHelper.getValue
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

abstract class BaseOrchestrator(
    /**
     * Gets or Sets the provider used by this local orchestrator
     */
    protected var provider: CoreProvider,
    /**
     * Gets the options used by this local orchestrator
     */
    var options: SyncOptions,
    /**
     * Gets the Setup used by this local orchestrator
     */
    var setup: SyncSetup,
    /**
     * Gets the scope name used by this local orchestrator
     */
    var scopeName: String = SyncOptions.DefaultScopeName
) {
    // Collection of Interceptors
    private val interceptors: Interceptors = Interceptors()

    var syncContext: SyncContext? = null

    /**
     * Gets or Sets the start time for this orchestrator
     */
    var startTime: Long? = null

    /**
     * Gets or Sets the end time for this orchestrator
     */
    var completeTime: Long? = null

    /**
     * Set an interceptor to get info on the current sync process
     */
    @Suppress("EXPERIMENTAL_API_USAGE_ERROR")
    internal inline fun <reified T : ProgressArgs> on(noinline action: (T) -> Unit) =
        this.interceptors.getInterceptor<T>().set(action)

    /**
     * Returns the Task associated with given type of BaseArgs
     * Because we are not doing anything else than just returning a task, no need to use async / await. Just return the Task itself
     */
    @Suppress("EXPERIMENTAL_API_USAGE_ERROR")
    internal inline fun <reified T : ProgressArgs> intercept(args: T) {
        val interceptor = this.interceptors.getInterceptor<T>()
        interceptor.run(args)
    }

    /**
     * Affect an interceptor
     */
    @Suppress("EXPERIMENTAL_API_USAGE_ERROR")
    internal inline fun <reified T : ProgressArgs> setInterceptor(noinline action: (T) -> Unit) =
        this.on(action)

    open fun getContext(): SyncContext {
        return syncContext ?: SyncContext(
            sessionId = UUID.randomUUID(),
            scopeName = this.scopeName
        )
    }

    /**
     * Read the schema stored from the orchestrator database, through the provider.
     * @return Schema containing tables, columns, relations, primary keys
     */
    fun getSchema(progress: Progress<ProgressArgs>? = null): SyncSet =
        this.internalGetSchema(this.getContext(), this.setup, progress)

    fun getScopeBuilder(scopeInfoTableName: String): DbScopeBuilder =
        this.provider.getScopeBuilder(scopeInfoTableName)

    fun getTableBuilder(tableDescription: SyncTable, setup: SyncSetup) =
        this.provider.getTableBuilder(tableDescription, setup)

    fun internalProvision(
        ctx: SyncContext,
        overwrite: Boolean,
        schema: SyncSet,
        setup: SyncSetup,
        provision: EnumSet<SyncProvision>,
        scope: Any?,
        progress: Progress<ProgressArgs>?
    ): SyncSet {
        // If schema does not have any table, raise an exception
        if (schema?.tables == null || !schema.hasTables)
            throw MissingTablesException()

        this.intercept(ProvisioningArgs(ctx, provision, schema))

        val builder = this.provider.getDatabaseBuilder()

        // Initialize database if needed
        builder.ensureDatabase()

        var schema = schema
        // Check if we have tables AND columns
        // If we don't have any columns it's most probably because user called method with the Setup only
        // So far we have only tables names, it's enough to get the schema
        if (schema.hasTables && !schema.hasColumns)
            schema = internalGetSchema(ctx, setup, progress)

        val scopeBuilder = this.getScopeBuilder(options.scopeInfoTableName)

        // Shoudl we create scope
        var scope = scope
        if (provision.contains(SyncProvision.ClientScope) && scope == null) {
            val exists = scopeBuilder.existsScopeInfoTable()

            if (!exists)
                scopeBuilder.createScopeInfoTable()

            scope = this.internalGetClientScope(ctx, this.scopeName, scopeBuilder, progress)
        }

        // Sorting tables based on dependencies between them
        val schemaTables = schema.tables
            .sortByDependencies({ tab: SyncTable ->
                tab.getRelations().map { r -> r.getParentTable() }.filterNotNull()
            })

        for (schemaTable in schemaTables) {
            val tableBuilder = this.getTableBuilder(schemaTable, this.setup)

            if (provision.contains(SyncProvision.Table)) {
                val tableExistst = this.internalExistsTable(ctx, tableBuilder, progress)

                if (!tableExistst)
                    this.internalCreateTable(ctx, tableBuilder, progress)
            }

            if (provision.contains(SyncProvision.TrackingTable)) {
                val trackingTableExistst =
                    this.internalExistsTrackingTable(ctx, tableBuilder, progress)

                if (!trackingTableExistst)
                    this.internalCreateTrackingTable(ctx, tableBuilder, progress)
            }

            if (provision.contains(SyncProvision.Triggers))
                this.internalCreateTriggers(ctx, overwrite, tableBuilder, progress)
        }

        // save scope
        if (this is LocalOrchestrator && scope != null) {
            val clientScopeInfo = scope as ScopeInfo
            clientScopeInfo.schema = schema
            clientScopeInfo.setup = setup

            this.internalSaveScope(clientScopeInfo, scopeBuilder, progress)
        }

        val args = ProvisionedArgs(ctx, provision, schema)
        this.intercept(args)
        this.reportProgress(ctx, progress, args)

        return schema
    }

    /**
     * Check if the orchestrator database is outdated
     */
    fun isOutDated(clientScopeInfo: ScopeInfo, serverScopeInfo: ServerScopeInfo): Boolean {
        if (this.startTime == null)
            this.startTime = utcNow()

        var isOutdated = false

        // Get context or create a new one
        val ctx = this.getContext()

        // if we have a new client, obviously the last server sync is < to server stored last clean up (means OutDated !)
        // so far we return directly false
        if (clientScopeInfo.isNewScope)
            return false

        // Check if the provider is not outdated
        // We can have negative value where we want to compare anyway
        if (clientScopeInfo.lastServerSyncTimestamp != 0L || serverScopeInfo.lastCleanupTimestamp != 0L)
            isOutdated =
                clientScopeInfo.lastServerSyncTimestamp!! < serverScopeInfo.lastCleanupTimestamp

        // Get a chance to make the sync even if it's outdated
        if (isOutdated) {
            val outdatedArgs = OutdatedArgs(ctx, clientScopeInfo, serverScopeInfo)

            // Interceptor
            this.intercept(outdatedArgs)

            if (outdatedArgs.action != OutdatedAction.Rollback)
                ctx.syncType =
                    if (outdatedArgs.action == OutdatedAction.Reinitialize)
                        SyncType.Reinitialize
                    else
                        SyncType.ReinitializeWithUpload

            if (outdatedArgs.action == OutdatedAction.Rollback)
                throw OutOfDateException(
                    clientScopeInfo.lastServerSyncTimestamp,
                    serverScopeInfo.lastCleanupTimestamp
                )
        }

        return isOutdated
    }

    /**
     * Internal create scope info table routine
     */
    internal fun internalCreateScopeInfoTable(
        ctx: SyncContext,
        scopeType: DbScopeType,
        scopeBuilder: DbScopeBuilder,
        progress: Progress<ProgressArgs>? = null
    ): Boolean {
        val action = ScopeTableCreatingArgs(
            ctx,
            scopeBuilder.scopeInfoTableName.toString(),
            scopeType
        )
        this.intercept(action)
        scopeBuilder.createScopeInfoTable()
        this.intercept(
            ScopeTableCreatedArgs(
                ctx,
                scopeBuilder.scopeInfoTableName.toString(),
                scopeType
            )
        )
        return true
    }

    /**
     * update configuration object with tables desc from server database
     */
    internal fun internalGetSchema(
        context: SyncContext,
        setup: SyncSetup?,
        progress: Progress<ProgressArgs>?
    ): SyncSet {
        if (setup == null || setup.tables.size <= 0)
            throw MissingTablesException()

        this.intercept(SchemaLoadingArgs(context, setup))

        // Create the schema
        val schema = SyncSet()

        // copy filters from setup
        this.setup.filters.forEach { schema.filters.add(it) }

        val relations = ArrayList<DbRelationDefinition>(20)

        this.setup.tables.forEach { setupTable ->
            val (syncTable, tableRelations) = internalGetTableSchema(context, setupTable, progress)
            // Add this table to schema
            schema.tables.add(syncTable)
            // Since we are not sure of the order of reading tables
            // create a tmp relations list
            relations.addAll(tableRelations)
        }

        // Parse and affect relations to schema
        setRelations(relations, schema)

        // Ensure all objects have correct relations to schema
        schema.ensureSchema()

        val schemaArgs = SchemaLoadedArgs(context, schema)
        this.intercept(schemaArgs)
        this.reportProgress(context, progress, schemaArgs)

        return schema
    }

    internal fun internalMigration(
        context: SyncContext,
        schema: SyncSet,
        oldSetup: SyncSetup,
        newSetup: SyncSetup
    ) {
        // TODO: Migration not implemented
//        throw NotImplementedError("Migration not implemented")
    }

    internal fun internalGetClientScope(
        ctx: SyncContext,
        scopeName: String,
        scopeBuilder: DbScopeBuilder,
        progress: Progress<ProgressArgs>?
    ): ScopeInfo {
        val scopes = scopeBuilder.getAllScopes(scopeName)
        if (scopes.isEmpty()) {
            var scope = ScopeInfo(
                id = UUID.randomUUID(),
                name = scopeName,
                isNewScope = true,
                lastSync = null,
                lastServerSyncTimestamp = null,
                lastSyncTimestamp = null,
                version = SyncVersion.current
            )
            scope = internalSaveScope(scope, scopeBuilder, progress)
            scopes.add(scope)
        }

        val localScope = scopes.first()
        scopes.forEach { scope ->
            scope.isNewScope = scope.lastSync == null
        }
        localScope.schema?.ensureSchema()

        return localScope
    }

    internal fun internalSaveScope(
        scope: ScopeInfo,
        scopeBuilder: DbScopeBuilder,
        progress: Progress<ProgressArgs>?
    ): ScopeInfo {
        val scopeExists = scopeBuilder.existsScopeInfo(scope.id)
        return if (scopeExists)
            scopeBuilder.updateScope(scope)
        else
            scopeBuilder.insertScope(scope)
    }

    private fun internalGetTableSchema(
        context: SyncContext,
        setupTable: SetupTable,
        progress: Progress<ProgressArgs>?
    ): Pair<SyncTable, List<DbRelationDefinition>> {
        val syncTable = this.provider.getDatabaseBuilder()
            .ensureTable(setupTable.tableName, setupTable.schemaName)

        val tableBuilder = getTableBuilder(syncTable, this.setup)

        val exists = internalExistsTable(context, tableBuilder, progress)

        if (!exists)
            throw MissingTableException(if (setupTable.schemaName.isBlank()) setupTable.tableName else "${setupTable.schemaName}.${setupTable.tableName}")

        // get columns list
        val lstColumns = tableBuilder.getColumns()

        // Validate the column list and get the dmTable configuration object.
        fillSyncTableWithColumns(setupTable, syncTable, lstColumns)

        // Check primary Keys
        setPrimaryKeys(syncTable, tableBuilder)

        // get all relations
        val tableRelations = tableBuilder.getRelations()

        return Pair(syncTable, tableRelations)
    }

    private fun internalExistsTable(
        ctx: SyncContext,
        tableBuilder: DbTableBuilder,
        progress: Progress<ProgressArgs>?
    ): Boolean =
        tableBuilder.existsTable()

    private fun fillSyncTableWithColumns(
        setupTable: SetupTable,
        schemaTable: SyncTable,
        columns: List<SyncColumn>
    ) {
        schemaTable.originalProvider = this.provider.getProviderTypeName()
        schemaTable.syncDirection = setupTable.syncDirection

        var ordinal = 0

        // Eventually, do not raise exception here, just we don't have any columns
        if (columns.isEmpty())
            return

        // Delete all existing columns
        if (schemaTable.primaryKeys.isNotEmpty())
            schemaTable.primaryKeys.clear()

        if (schemaTable.columns!!.isNotEmpty())
            schemaTable.columns!!.clear()


        val lstColumns: List<SyncColumn>

        // Validate columns list from setup table if any
        if (setupTable.columns.isNotEmpty()) {
            lstColumns = ArrayList<SyncColumn>()

            for (setupColumn in setupTable.columns) {
                // Check if the columns list contains the column name we specified in the setup
                val column = columns.firstOrNull { c -> c.columnName.equals(setupColumn, true) }

                if (column == null)
                    throw MissingColumnException(setupColumn, schemaTable.tableName)
                else
                    lstColumns.add(column)
            }
        } else {
            lstColumns = columns
        }


        for (column in lstColumns.sortedBy { c -> c.ordinal }) {
            // First of all validate if the column is currently supported
            if (!this.provider.getMetadata().isValid(column))
                throw UnsupportedColumnTypeException(
                    column.columnName,
                    column.originalTypeName,
                    this.provider.getProviderTypeName()
                )

            val columnNameLower = column.columnName.lowercase(Locale.getDefault())
            if (columnNameLower == "sync_scope_id"
                || columnNameLower == "changeTable"
                || columnNameLower == "sync_scope_name"
                || columnNameLower == "sync_min_timestamp"
                || columnNameLower == "sync_row_count"
                || columnNameLower == "sync_force_write"
                || columnNameLower == "sync_update_scope_id"
                || columnNameLower == "sync_timestamp"
                || columnNameLower == "sync_row_is_tombstone"
            )
                throw UnsupportedColumnTypeException(
                    column.columnName,
                    column.originalTypeName,
                    this.provider.getProviderTypeName()
                )

            // Validate max length
            column.maxLength = this.provider.getMetadata().validateMaxLength(
                column.originalTypeName,
                column.isUnsigned,
                column.isUnicode,
                column.maxLength
            )

            // Gets the datastore owner dbType (could be SqlDbtype, MySqlDbType, SqliteDbType, NpgsqlDbType & so on ...)
            val datastoreDbType = this.provider.getMetadata().validateOwnerDbType(
                column.originalTypeName,
                column.isUnsigned,
                column.isUnicode,
                column.maxLength
            )

            // once we have the datastore type, we can have the managed type
            val columnType = this.provider.getMetadata().validateType(datastoreDbType)

            // Set the correct type
            column.setType(columnType)

            // and the DbType
            column.dbType = this.provider.getMetadata().validateDbType(
                column.originalTypeName,
                column.isUnsigned,
                column.isUnicode,
                column.maxLength
            ).value

            // Gets the owner dbtype (SqlDbType, OracleDbType, MySqlDbType, NpsqlDbType & so on ...)
            // Sqlite does not have it's own type, so it's DbType too
            column.originalDbType = datastoreDbType.toString()

            // Validate if column should be readonly
            column.isReadOnly = this.provider.getMetadata().validateIsReadonly(column)

            // set position ordinal
            column.ordinal = ordinal
            ordinal++

            // Validate the precision and scale properties
            if (this.provider.getMetadata().isNumericType(column.originalTypeName)) {
                if (this.provider.getMetadata().supportScale(column.originalTypeName)) {
                    val (p, s) = this.provider.getMetadata().validatePrecisionAndScale(column)
                    column.precision = p
                    column.precisionSpecified = true
                    column.scale = s
                    column.scaleSpecified = true
                } else {
                    column.precision = this.provider.getMetadata().validatePrecision(column)
                    column.precisionSpecified = true
                    column.scaleSpecified = false
                }

            }

            // if setup table has no columns, we add all columns from db
            // otherwise check if columns exist in the data source
            if (setupTable.columns.isEmpty() || setupTable.columns.contains(column.columnName))
                schemaTable.columns?.add(column)
            // If column does not allow null value and is not compute
            // We will not be able to insert a row, so raise an error
            else if (!column.allowDBNull && !column.isCompute && !column.isReadOnly && column.defaultValue.isNullOrBlank())
                throw Exception("Column ${column.columnName} is not part of your setup. But it seems this columns is mandatory in your data source.")
        }
    }

    /**
     * Check then add primary keys to schema table
     */
    private fun setPrimaryKeys(schemaTable: SyncTable, tableBuilder: DbTableBuilder) {
        val schemaPrimaryKeys = tableBuilder.getPrimaryKeys()
        if (schemaPrimaryKeys.isEmpty())
            throw MissingPrimaryKeyException(schemaTable.tableName)

        // Set the primary Key
        for (rowColumn in schemaPrimaryKeys.sortedBy { r -> r.ordinal }) {
            // Find the column in the schema columns
            val columnKey = schemaTable.columns!!.firstOrNull { sc ->
                sc.columnName.equals(
                    rowColumn.columnName,
                    true
                )
            }

            if (columnKey == null)
                throw MissingPrimaryKeyColumnException(rowColumn.columnName, schemaTable.tableName)

            val columnNameLower = columnKey.columnName.lowercase(Locale.getDefault())
            if (columnNameLower == "update_scope_id" ||
                columnNameLower == "timestamp" ||
                columnNameLower == "timestamp_bigint" ||
                columnNameLower == "sync_row_is_tombstone" ||
                columnNameLower == "last_change_datetime"
            )
                throw UnsupportedPrimaryKeyColumnNameException(
                    columnKey.columnName,
                    columnKey.originalTypeName,
                    this.provider.getProviderTypeName()
                )

            schemaTable.primaryKeys.add(columnKey.columnName)
        }
    }

    /**
     * For all relations founded, create the SyncRelation and add it to schema
     */
    private fun setRelations(relations: List<DbRelationDefinition>, schema: SyncSet) {
        if (relations.isEmpty())
            return

        for (r in relations) {
            // Get table from the relation where we need to work on
            val schemaTable = schema.tables[r.tableName, r.schemaName]!!

            // get SchemaColumn from SchemaTable, based on the columns from relations
            val schemaColumns = r.columns
                .sortedBy { kc -> kc.order }
                .map { kc ->
                    val schemaColumn = schemaTable.columns!![kc.keyColumnName]

                    if (schemaColumn == null)
                        return@map null

                    return@map SyncColumnIdentifier(
                        schemaColumn.columnName,
                        schemaTable.tableName,
                        schemaTable.schemaName
                    )
                }
                .filterNotNull()
            //.toList()

            // if we don't find the column, maybe we just dont have this column in our setup def
            if (schemaColumns.isEmpty())
                continue

            // then Get the foreign table as well
            val foreignTable =
                schemaTable.schema!!.tables[r.referenceTableName, r.referenceSchemaName]

            // Since we can have a table with a foreign key but not the parent table
            // It's not a problem, just forget it
            if (foreignTable?.columns.isNullOrEmpty())
                continue

            val foreignColumns = r.columns
                .sortedBy { kc -> kc.order }
                .map { fc ->
                    val schemaColumn = foreignTable!!.columns!![fc.referenceColumnName]
                    if (schemaColumn == null)
                        return@map null
                    return@map SyncColumnIdentifier(
                        schemaColumn.columnName,
                        foreignTable.tableName,
                        foreignTable.schemaName
                    )
                }
                .filterNotNull()
            //.toList()

            if (foreignColumns.isEmpty())
                continue

            val schemaRelation =
                SyncRelation(r.foreignKey, ArrayList(schemaColumns), ArrayList(foreignColumns))

            schema.relations.add(schemaRelation)
        }
    }

    /**
     * Internal create table routine
     */
    private fun internalCreateTable(
        ctx: SyncContext,
        tableBuilder: DbTableBuilder,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        if (tableBuilder.tableDescription.columns!!.isEmpty())
            throw MissingsColumnException(tableBuilder.tableDescription.getFullName())

        if (tableBuilder.tableDescription.primaryKeys.isEmpty())
            throw MissingPrimaryKeyException(tableBuilder.tableDescription.getFullName())

        tableBuilder.createTable()

        return true
    }

    private fun internalExistsTrackingTable(
        ctx: SyncContext,
        tableBuilder: DbTableBuilder,
        progress: Progress<ProgressArgs>?
    ): Boolean =
        tableBuilder.existsTrackingTable()

    private fun internalCreateTrackingTable(
        ctx: SyncContext,
        tableBuilder: DbTableBuilder,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        if (tableBuilder.tableDescription.columns!!.isEmpty())
            throw  MissingsColumnException(tableBuilder.tableDescription.getFullName())

        if (tableBuilder.tableDescription.primaryKeys.isEmpty())
            throw  MissingPrimaryKeyException(tableBuilder.tableDescription.getFullName())

        return tableBuilder.createTrackingTable()
    }

    private fun internalCreateTriggers(
        ctx: SyncContext,
        overwrite: Boolean,
        tableBuilder: DbTableBuilder,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        var hasCreatedAtLeastOneTrigger = false

        val listTriggerType = DbTriggerType.values()

        for (triggerType in listTriggerType) {
            val exists = tableBuilder.existsTrigger(triggerType)

            // Drop trigger if already exists
            if (exists && overwrite)
                tableBuilder.dropTrigger(triggerType)

            val shouldCreate = !exists || overwrite

            if (!shouldCreate)
                continue

            val hasBeenCreated = internalCreateTrigger(ctx, tableBuilder, triggerType)

            if (hasBeenCreated)
                hasCreatedAtLeastOneTrigger = true
        }

        return hasCreatedAtLeastOneTrigger
    }

    private fun internalCreateTrigger(
        ctx: SyncContext,
        tableBuilder: DbTableBuilder,
        triggerType: DbTriggerType
    ): Boolean {
        if (tableBuilder.tableDescription.columns!!.isEmpty())
            throw  MissingsColumnException(tableBuilder.tableDescription.getFullName())

        if (tableBuilder.tableDescription.primaryKeys.isEmpty())
            throw  MissingPrimaryKeyException(tableBuilder.tableDescription.getFullName())

        tableBuilder.createTrigger(triggerType)

        return true
    }

    internal fun internalGetLocalTimestamp(): Long {
        val scopeBuilder = getScopeBuilder(this.options.scopeInfoTableName)
        return scopeBuilder.getLocalTimestamp()
    }

    /**
     * Get the correct Select changes command
     * Can be either
     * - SelectInitializedChanges              : All changes for first sync
     * - SelectChanges                         : All changes filtered by timestamp
     * - SelectInitializedChangesWithFilters   : All changes for first sync with filters
     * - SelectChangesWithFilters              : All changes filtered by timestamp with filters
     */
    protected fun getSelectChangesCursor(
        context: SyncContext,
        syncTable: SyncTable,
        setup: SyncSetup,
        isNew: Boolean,
        lastTimestamp: Long?
    ): Cursor {
        val tableFilter: SyncFilter?

        val syncAdapter = this.getSyncAdapter(syncTable, setup)

        // Check if we have parameters specified

        val hasFilters = false//tableFilter != null

        // Sqlite does not have any filter, since he can't be server side
//        if (this.provider.canBeServerProvider)
//            tableFilter = syncTable.getFilter()

        // Determing the correct DbCommandType
        return when {
            isNew && hasFilters ->
                syncAdapter.getSelectInitializedChangesWithFilters()
            isNew && !hasFilters ->
                syncAdapter.getSelectInitializedChanges()
            !isNew && hasFilters ->
                syncAdapter.getSelectChangesWithFilters(lastTimestamp)
            else ->
                syncAdapter.getSelectChanges(lastTimestamp)
        }
    }

    /**
     * Create a new SyncRow from a dataReader.
     */
    internal fun createSyncRowFromReader(cursor: Cursor, table: SyncTable): SyncRow {
        // Create a new row, based on table structure
        val row = table.newRow()

        var isTombstone = false

        for (i in 0 until cursor.columnCount) {
            val columnName = cursor.getColumnName(i)

            // if we have the tombstone value, do not add it to the table
            if (columnName == "sync_row_is_tombstone") {
                isTombstone = cursor.getInt(i) > 0 // Convert.ToInt64(dataReader.GetValue(i)) > 0
                continue
            }
            if (columnName == "sync_update_scope_id") {
                continue
            }

            val columnValue = cursor.getValue(i)

            row[columnName] = columnValue
        }

        row.rowState = if (isTombstone) DataRowState.Deleted else DataRowState.Modified

        return row
    }

    /**
     * Get the provider sync adapter
     */
    fun getSyncAdapter(tableDescription: SyncTable, setup: SyncSetup): DbSyncAdapter {
        return this.provider.getSyncAdapter(tableDescription, setup)
    }

    /**
     * Apply changes : Delete / Insert / Update
     * the fromScope is local client scope when this method is called from server
     * the fromScope is server scope when this method is called from client
     */
    internal fun internalApplyChanges(
        context: SyncContext,
        message: MessageApplyChanges,
        progress: Progress<ProgressArgs>?
    ): Pair<SyncContext, DatabaseChangesApplied> {
        val changesApplied = DatabaseChangesApplied()

        // Check if we have some data available
        val hasChanges = message.changes.hasData()

        // if we have changes or if we are in re init mode
        if (hasChanges || context.syncType != SyncType.Normal) {

            val schemaTables = message.schema.tables.sortByDependencies({ tab ->
                tab.getRelations().mapNotNull { r -> r.getParentTable() }
            })

            // Disable check constraints
            // Because Sqlite does not support "PRAGMA foreign_keys=OFF" Inside a transaction
            // Report this disabling constraints brefore opening a transaction
            if (message.disableConstraintsOnApplyChanges)
                schemaTables.forEach { table ->
                    this.internalDisableConstraints(
                        context,
                        this.getSyncAdapter(table, message.setup)
                    )
                }

            // -----------------------------------------------------
            // 0) Check if we are in a reinit mode (Check also SyncWay to be sure we don't reset tables on server, then check if we don't have already applied a snapshot)
            // -----------------------------------------------------
            if (context.syncWay == SyncWay.Download && context.syncType != SyncType.Normal && !message.snapshoteApplied)
                schemaTables.reversed().forEach { table ->
                    this.internalResetTable(
                        context,
                        this.getSyncAdapter(table, message.setup)
                    )
                }

            // Trying to change order (from deletes-upserts to upserts-deletes)
            // see https://github.com/Mimetis/Dotmim.Sync/discussions/453#discussioncomment-380530

            // -----------------------------------------------------
            // 1) Applying Inserts and Updates. Apply in table order
            // -----------------------------------------------------
            if (hasChanges)
                schemaTables.forEach { table ->
                    this.internalApplyTableChanges(
                        context,
                        table,
                        message,
                        DataRowState.Modified,
                        changesApplied,
                        progress
                    )
                }

            // -----------------------------------------------------
            // 2) Applying Deletes. Do not apply deletes if we are in a new database
            // -----------------------------------------------------
            if (!message.isNew && hasChanges)
                schemaTables.reversed().forEach { table ->
                    this.internalApplyTableChanges(
                        context,
                        table,
                        message,
                        DataRowState.Deleted,
                        changesApplied,
                        progress
                    )
                }

            // Re enable check constraints
            if (message.disableConstraintsOnApplyChanges)
                schemaTables.forEach { table ->
                    this.internalEnableConstraints(
                        context,
                        this.getSyncAdapter(table, message.setup)
                    )
                }

            // Dispose data
            message.changes.clear(false)
        }


        // Before cleaning, check if we are not applying changes from a snapshotdirectory
        var cleanFolder = message.cleanFolder

        if (cleanFolder)
            cleanFolder = this.internalCanCleanFolder(context, message.changes, progress)

        // clear the changes because we don't need them anymore
        message.changes.clear(cleanFolder)

        val databaseChangesAppliedArgs = DatabaseChangesAppliedArgs(context, changesApplied)
        this.intercept(databaseChangesAppliedArgs)
        this.reportProgress(context, progress, databaseChangesAppliedArgs)

        return Pair(context, changesApplied)
    }

    /**
     * Disabling all constraints on synced tables
     */
    internal fun internalDisableConstraints(context: SyncContext, syncAdapter: DbSyncAdapter) =
        syncAdapter.disableConstraints()

    /**
     * Enabling all constraints on synced tables
     */
    internal fun internalEnableConstraints(context: SyncContext, syncAdapter: DbSyncAdapter) =
        syncAdapter.enableConstraints()

    /**
     * Reset a table, deleting rows from table and tracking_table
     */
    internal fun internalResetTable(context: SyncContext, syncAdapter: DbSyncAdapter) =
        syncAdapter.reset()

    /**
     * Internal routine to clean tmp folders. MUST be compare also with Options.CleanFolder
     */
    internal fun internalCanCleanFolder(
        context: SyncContext,
        batchInfo: BatchInfo,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        // if in memory, the batch file is not actually serialized on disk
        if (batchInfo.inMemory)
            return false

        val batchInfoDirectoryFullPath = File(batchInfo.getDirectoryFullPath())

        val (snapshotRootDirectory, snapshotNameDirectory) = this.getSnapshotDirectory()

        // if we don't have any snapshot configuration, we are sure that the current batchinfo is actually stored into a temp folder
        if (snapshotRootDirectory.isBlank())
            return true

        val snapInfo = File(snapshotRootDirectory, snapshotNameDirectory).absolutePath
        val snapshotDirectoryFullPath = File(snapInfo)

        // check if the batch dir IS NOT the snapshot directory
        val canCleanFolder =
            batchInfoDirectoryFullPath.absolutePath != snapshotDirectoryFullPath.absolutePath

        return canCleanFolder
    }

    /**
     *  Get a snapshot root directory name and folder directory name
     */
    fun getSnapshotDirectory(syncParameters: SyncParameters? = null): Pair<String, String> {
        // Get context or create a new one
        val ctx = this.getContext()

        // check parameters
        // If context has no parameters specified, and user specifies a parameter collection we switch them
        if ((ctx.parameters == null || ctx.parameters!!.size <= 0) && syncParameters != null && syncParameters.size > 0)
            ctx.parameters = syncParameters

        return this.internalGetSnapshotDirectory(ctx)
    }

    /**
     * Internal routine to get the snapshot root directory and batch directory name
     */
    internal fun internalGetSnapshotDirectory(context: SyncContext): Pair<String, String> {
        if (this.options.snapshotsDirectory.isBlank())
            return Pair("", "")

        // cleansing scope name
        val directoryScopeName = context.scopeName.filter { it.isLetterOrDigit() }

        val directoryFullPath =
            File(this.options.snapshotsDirectory, directoryScopeName).absolutePath

        val sb = StringBuilder()
        var underscore = ""

        if (context.parameters != null) {
            for (p in context.parameters!!.sortedBy { p -> p.name }) {
                val cleanValue = p.value.toString().filter { it.isLetterOrDigit() }
                val cleanName = p.name.filter { it.isLetterOrDigit() }

                sb.append("${underscore}${cleanName}_${cleanValue}")
                underscore = "_"
            }
        }

        var directoryName = sb.toString()
        directoryName = if (directoryName.isBlank()) "ALL" else directoryName

        return Pair(directoryFullPath, directoryName)
    }

    /**
     * Apply changes internal method for one type of query: Insert, Update or Delete for every batch from a table
     */
    private fun internalApplyTableChanges(
        context: SyncContext,
        schemaTable: SyncTable,
        message: MessageApplyChanges,
        applyType: DataRowState,
        changesApplied: DatabaseChangesApplied,
        progress: Progress<ProgressArgs>?
    ) {
        // Only table schema is replicated, no datas are applied
        if (schemaTable.syncDirection == SyncDirection.None)
            return

        // if we are in upload stage, so check if table is not download only
        if (context.syncWay == SyncWay.Upload && schemaTable.syncDirection == SyncDirection.DownloadOnly)
            return

        // if we are in download stage, so check if table is not download only
        if (context.syncWay == SyncWay.Download && schemaTable.syncDirection == SyncDirection.UploadOnly)
            return

        val hasChanges = message.changes.hasData(schemaTable.tableName, schemaTable.schemaName)

        // Each table in the messages contains scope columns. Don't forget it
        if (hasChanges) {
            // launch interceptor if any
            val args = TableChangesApplyingArgs(context, schemaTable, applyType)
            this.intercept(args)

            if (args.cancel)
                return

            var tableChangesApplied: TableChangesApplied? = null

            val enumerableOfTables =
                message.changes.getTable(schemaTable.tableName, schemaTable.schemaName, this)

            // getting the table to be applied
            // we may have multiple batch files, so we can have multipe sync tables with the same name
            // We can say that dmTable may be contained in several files
            for (syncTable in enumerableOfTables) {

                if (syncTable?.rows == null || syncTable.rows.isEmpty())
                    continue

                // Creating a filtered view of my rows with the correct applyType
                val filteredRows = syncTable.rows.filter { r -> r.rowState == applyType }

                // no filtered rows, go next container table
                if (filteredRows.isEmpty())
                    continue

                // Create an empty Set that wil contains filtered rows to apply
                // Need Schema for culture & case sensitive properties
                val changesSet = syncTable.schema?.clone(false)
                val schemaChangesTable = syncTable.clone()
                changesSet?.tables?.add(schemaChangesTable)
                schemaChangesTable.rows.addAll(filteredRows)

                // Should we use bulk operations ?
                val usBulk = message.useBulkOperations && false//this.provider.supportBulkOperations

                // Apply the changes batch
                val (rowsApplied, conflictsResolvedCount) = this.internalApplyChangesBatch(
                    context,
                    usBulk,
                    schemaChangesTable,
                    message,
                    applyType
                )

                // Any failure ?
                val changedFailed = filteredRows.size - conflictsResolvedCount - rowsApplied

                // We may have multiple batch files, so we can have multipe sync tables with the same name
                // We can say that a syncTable may be contained in several files
                // That's why we should get an applied changes instance if already exists from a previous batch file
                tableChangesApplied = changesApplied.tableChangesApplied.firstOrNull { tca ->
                    val sn = tca.schemaName
                    val otherSn = schemaTable.schemaName

                    return@firstOrNull tca.tableName.equals(schemaTable.tableName, true) &&
                            sn.equals(otherSn, true) &&
                            tca.state == applyType
                }

                if (tableChangesApplied == null) {
                    tableChangesApplied = TableChangesApplied(
                        tableName = schemaTable.tableName,
                        schemaName = schemaTable.schemaName,
                        applied = rowsApplied,
                        resolvedConflicts = conflictsResolvedCount,
                        failed = changedFailed,
                        state = applyType,
                        totalRowsCount = message.changes.rowsCount,
                        totalAppliedCount = changesApplied.totalAppliedChanges + rowsApplied
                    )
                    changesApplied.tableChangesApplied.add(tableChangesApplied)
                } else {
                    tableChangesApplied.applied += rowsApplied
                    tableChangesApplied.totalAppliedCount = changesApplied.totalAppliedChanges
                    tableChangesApplied.resolvedConflicts += conflictsResolvedCount
                    tableChangesApplied.failed += changedFailed
                }

                // we've got 0.25% to fill here
                val progresspct = rowsApplied * 0.25 / tableChangesApplied.totalRowsCount
                context.progressPercentage += progresspct

                val tableChangesBatchAppliedArgs =
                    TableChangesBatchAppliedArgs(context, tableChangesApplied)

                // Report the batch changes applied
                // We don't report progress if we do not have applied any changes on the table, to limit verbosity of Progress
                if (tableChangesBatchAppliedArgs.tableChangesApplied.applied > 0 || tableChangesBatchAppliedArgs.tableChangesApplied.failed > 0 || tableChangesBatchAppliedArgs.tableChangesApplied.resolvedConflicts > 0) {
                    this.intercept(tableChangesBatchAppliedArgs)
                    this.reportProgress(context, progress, tableChangesBatchAppliedArgs)
                }
            }

            // Report the overall changes applied for the current table
            if (tableChangesApplied != null) {
                val tableChangesAppliedArgs = TableChangesAppliedArgs(context, tableChangesApplied)

                // We don't report progress if we do not have applied any changes on the table, to limit verbosity of Progress
                if (tableChangesAppliedArgs.tableChangesApplied.applied > 0 || tableChangesAppliedArgs.tableChangesApplied.failed > 0 || tableChangesAppliedArgs.tableChangesApplied.resolvedConflicts > 0)
                    this.intercept(tableChangesAppliedArgs)
            }
        }
    }

    /**
     * Internally apply a batch changes from a table
     */
    private fun internalApplyChangesBatch(
        context: SyncContext,
        useBulkOperation: Boolean,
        changesTable: SyncTable,
        message: MessageApplyChanges,
        applyType: DataRowState
    ): Pair<Int, Int> {
        // Conflicts occured when trying to apply rows
        val conflictRows = ArrayList<SyncRow>()

        // get executioning adapter
        val syncAdapter = this.getSyncAdapter(changesTable, message.setup)
        syncAdapter.applyType = applyType

        // Get correct command type
//        var dbCommandType = when(applyType)                {
//                    DataRowState.Deleted -> if (useBulkOperation) DbCommandType.BulkDeleteRows : DbCommandType.DeleteRow,
//                    DataRowState.Modified -> if (useBulkOperation) DbCommandType.BulkUpdateRows : DbCommandType.UpdateRow,
//                    _ => throw new UnknownException("RowState not valid during ApplyBulkChanges operation"),
//                }
//
//        // Get command
//        var command = await syncAdapter.GetCommandAsync(dbCommandType, connection, transaction)

        // Launch any interceptor if available
        val args = TableChangesBatchApplyingArgs(context, changesTable, applyType)
        this.intercept(args)

        if (args.cancel)// || args.Command == null)
            return Pair(0, 0)
//
//        // get the correct pointer to the command from the interceptor in case user change the whole instance
//        command = args.Command

        // get the items count
        val itemsArrayCount = changesTable.rows.size

        // Make some parts of BATCH_SIZE
        var appliedRowsTmp = 0

        for (step in 0 until itemsArrayCount step DbSyncAdapter.BATCH_SIZE) {
            // get upper bound max value
            val taken =
                step + (if (DbSyncAdapter.BATCH_SIZE >= itemsArrayCount) itemsArrayCount - step else DbSyncAdapter.BATCH_SIZE)

            val arrayStepChanges = changesTable.rows.drop(step).take(taken)

            if (useBulkOperation) {
//                    val failedPrimaryKeysTable = changesTable.schema.clone().tables[changesTable.tableName, changesTable.schemaName]
//
//                    // execute the batch, through the provider
//                    syncAdapter.ExecuteBatchCommandAsync(command, message.senderScopeId, arrayStepChanges, changesTable, failedPrimaryKeysTable, message.lastTimestamp)
//
//                    // Get local and remote row and create the conflict object
//                    for (failedRow in failedPrimaryKeysTable!!.rows) {
//                        // Get the row that caused the problem, from the opposite side (usually client)
//                        val remoteConflictRow = changesTable.rows.getRowByPrimaryKeys(failedRow)
//                        conflictRows.add(remoteConflictRow)
//                    }
//
//                    //rows minus failed rows
//                    appliedRowsTmp += taken - failedPrimaryKeysTable.rows.size
            } else {
                val init = message.isNew || context.syncType != SyncType.Normal
                for (row in arrayStepChanges) {
                    val rowAppliedCount = when (applyType) {
                        DataRowState.Deleted -> syncAdapter.deleteRow(
                            message.senderScopeId,
                            message.lastTimestamp,
                            applyType == DataRowState.Deleted,
                            false,
                            row
                        )
                        DataRowState.Modified -> if (init)
                            syncAdapter.initializeRow(
                                message.senderScopeId,
                                message.lastTimestamp,
                                applyType == DataRowState.Deleted,
                                false,
                                row
                            )
                        else
                            syncAdapter.updateRow(
                                message.senderScopeId,
                                message.lastTimestamp,
                                applyType == DataRowState.Deleted,
                                false,
                                row
                            )
                        else -> throw UnknownException("RowState not valid during ApplyBulkChanges operation")
                    }

                    if (rowAppliedCount > 0)
                        appliedRowsTmp++
                    else
                        conflictRows.add(row)
                }
            }

        }

        var appliedRows = appliedRowsTmp

        // If conflicts occured
        if (conflictRows.isEmpty())
            return Pair(appliedRows, 0)

        // conflict rows applied
        var rowsAppliedCount = 0
        // conflict resolved count
        var conflictsResolvedCount = 0

        for (conflictRow in conflictRows) {
            val fromScopeLocalTimeStamp = message.lastTimestamp

            val (conflictResolvedCount, resolvedRow, rowAppliedCount) =
                this.handleConflict(
                    message.localScopeId,
                    message.senderScopeId,
                    syncAdapter,
                    context,
                    conflictRow,
                    changesTable,
                    message.policy,
                    fromScopeLocalTimeStamp
                )

            conflictsResolvedCount += conflictResolvedCount
            rowsAppliedCount += rowAppliedCount

        }

        appliedRows += rowsAppliedCount

        return Pair(appliedRows, conflictsResolvedCount)
    }

    /**
     * Handle a conflict
     * The int returned is the conflict count I need
     */
    private fun handleConflict(
        localScopeId: UUID, senderScopeId: UUID?, syncAdapter: DbSyncAdapter,
        context: SyncContext, conflictRow: SyncRow, schemaChangesTable: SyncTable,
        policy: ConflictResolutionPolicy, lastTimestamp: Long?
    ): Triple<Int, SyncRow?, Int> {
        var rowAppliedCount = 0

        var (conflictApplyAction, conflictType, localRow, finalRow, nullableSenderScopeId) = this.getConflictAction(
            context, localScopeId, syncAdapter, conflictRow, schemaChangesTable,
            policy, senderScopeId
        )

        // Conflict rollbacked by user
        if (conflictApplyAction == ApplyAction.Rollback)
            throw RollbackException("Rollback action taken on conflict")

        // Local provider wins, update metadata
        if (conflictApplyAction == ApplyAction.Continue) {
            val isMergeAction = finalRow != null
            val row = if (isMergeAction) finalRow else localRow

            // Conflict on a line that is not present on the datasource
            if (row == null)
                return Triple(1, finalRow, 0)

            // if we have a merge action, we apply the row on the server
            if (isMergeAction) {
                // if merge, we update locally the row and let the update_scope_id set to null
                val isUpdated = this.internalApplyConflictUpdate(
                    context,
                    syncAdapter,
                    row,
                    lastTimestamp,
                    null,
                    true
                )
                // We don't update metadatas so the row is updated (on server side)
                // and is mark as updated locally.
                // and will be returned back to sender, since it's a merge, and we need it on the client

                if (!isUpdated)
                    throw Exception("Can't update the merge row.")
            }

            finalRow = if (isMergeAction) row else localRow

            // We don't do anything, since we let the original row. so we resolved one conflict but applied no rows
            return Triple(1, finalRow, 0)

        }

        // We gonna apply with force the line
        if (conflictApplyAction == ApplyAction.RetryWithForceWrite) {
            // TODO : Should Raise an error ?
            if (conflictRow == null)
                return Triple(0, finalRow, 0)

            var operationComplete = false

            when (conflictType) {
                // Remote source has row, Local don't have the row, so insert it
                ConflictType.RemoteExistsLocalExists -> {
                    operationComplete = this.internalApplyConflictUpdate(
                        context,
                        syncAdapter,
                        conflictRow,
                        lastTimestamp,
                        nullableSenderScopeId,
                        true
                    )
                    rowAppliedCount = 1
                }

                ConflictType.RemoteExistsLocalNotExists,
                ConflictType.RemoteExistsLocalIsDeleted,
                ConflictType.UniqueKeyConstraint -> {
                    operationComplete = this.internalApplyConflictUpdate(
                        context,
                        syncAdapter,
                        conflictRow,
                        lastTimestamp,
                        nullableSenderScopeId,
                        true
                    )
                    rowAppliedCount = 1
                }

                // Conflict, but both have delete the row, so just update the metadata to the right winner
                ConflictType.RemoteIsDeletedLocalIsDeleted -> {
                    operationComplete = this.internalUpdateMetadatas(
                        context,
                        syncAdapter,
                        conflictRow,
                        nullableSenderScopeId,
                        true
                    )
                    rowAppliedCount = 0
                }

                // The row does not exists locally, and since it's coming from a deleted state, we can forget it
                ConflictType.RemoteIsDeletedLocalNotExists -> {
                    operationComplete = true
                    rowAppliedCount = 0
                }

                // The remote has delete the row, and local has insert or update it
                // So delete the local row
                ConflictType.RemoteIsDeletedLocalExists -> {
                    operationComplete = this.internalApplyConflictDelete(
                        context,
                        syncAdapter,
                        conflictRow,
                        lastTimestamp,
                        nullableSenderScopeId,
                        true
                    )

                    // Conflict, but both have delete the row, so just update the metadata to the right winner
                    if (!operationComplete) {
                        operationComplete = this.internalUpdateMetadatas(
                            context,
                            syncAdapter,
                            conflictRow,
                            nullableSenderScopeId,
                            true
                        )
                        rowAppliedCount = 0

                    } else {
                        rowAppliedCount = 1
                    }

                }

                ConflictType.ErrorsOccurred ->
                    return Triple(0, finalRow, 0)

                else -> {
                }
            }

            finalRow = conflictRow

            //After a force update, there is a problem, so raise exception
            if (!operationComplete)
                throw UnknownException("Force update should always work.. contact the author :)")

            return Triple(1, finalRow, rowAppliedCount)
        }

        return Triple(0, finalRow, 0)
    }

    /**
     * A conflict has occured, we try to ask for the solution to the user
     */
    private fun getConflictAction(
        context: SyncContext, localScopeId: UUID?, syncAdapter: DbSyncAdapter,
        conflictRow: SyncRow, schemaChangesTable: SyncTable, policy: ConflictResolutionPolicy,
        senderScopeId: UUID?
    ): Tuple<ApplyAction, ConflictType, SyncRow?, SyncRow?, UUID?> {
        // default action
        var resolution =
            if (policy == ConflictResolutionPolicy.ClientWins) ConflictResolution.ClientWins else ConflictResolution.ServerWins

        // if ConflictAction is ServerWins or MergeRow it's Ok to set to Continue
        var action = ApplyAction.Continue

        // check the interceptor
        val interceptor = this.interceptors.getInterceptor<ApplyChangesFailedArgs>()

        var finalRow: SyncRow? = null
        var localRow: SyncRow? = null
        var finalSenderScopeId: UUID? = senderScopeId

        // default conflict type
        var conflictType =
            if (conflictRow.rowState == DataRowState.Deleted)
                ConflictType.RemoteIsDeletedLocalExists
            else
                ConflictType.RemoteExistsLocalExists

        // if is not empty, get the conflict and intercept
        if (!interceptor.isEmpty) {
            // Get the localRow
            localRow = this.internalGetConflictRow(
                context,
                syncAdapter,
                localScopeId!!,
                conflictRow,
                schemaChangesTable
            )
            // Get the conflict
            val conflict = this.getConflict(conflictRow, localRow)

            // Interceptor
            val arg = ApplyChangesFailedArgs(context, conflict, resolution, senderScopeId)
            this.intercept(arg)

            resolution = arg.resolution
            finalRow = if (arg.resolution == ConflictResolution.MergeRow) arg.finalRow else null
            finalSenderScopeId = arg.senderScopeId
            conflictType = arg.conflict.type
//        } else {
//            // Check logger, because we make some reflection here
//            if (this.Logger.IsEnabled(LogLevel.Debug)) {
//                var args = new {
//                    Row =
//                        conflictRow, Resolution = resolution, Connection = connection, Transaction = transaction
//                }
//                this.Logger.LogDebug(
//                    new EventId (SyncEventsId.ApplyChangesFailed.Id,
//                    "ApplyChangesFailed"
//                ), args)
//            }
        }

        // Change action only if we choose ClientWins or Rollback.
        // for ServerWins or MergeRow, action is Continue
        if (resolution == ConflictResolution.ClientWins)
            action = ApplyAction.RetryWithForceWrite
        else if (resolution == ConflictResolution.Rollback)
            action = ApplyAction.Rollback

        // returning the action to take, and actually the finalRow if action is set to Merge
        return Tuple(action, conflictType, localRow, finalRow, finalSenderScopeId)
    }

    /**
     * We have a conflict, try to get the source row and generate a conflict
     */
    private fun getConflict(remoteConflictRow: SyncRow?, localConflictRow: SyncRow?): SyncConflict {
        var dbConflictType = ConflictType.ErrorsOccurred

        if (remoteConflictRow == null)
            throw UnknownException("THAT can't happen...")


        // local row is null
        if (localConflictRow == null && remoteConflictRow.rowState == DataRowState.Modified)
            dbConflictType = ConflictType.RemoteExistsLocalNotExists
        else if (localConflictRow == null && remoteConflictRow.rowState == DataRowState.Deleted)
            dbConflictType = ConflictType.RemoteIsDeletedLocalNotExists

        //// remote row is null. Can't happen
        //else if (remoteConflictRow == null && localConflictRow.RowState == DataRowState.Modified)
        //    dbConflictType = ConflictType.RemoteNotExistsLocalExists;
        //else if (remoteConflictRow == null && localConflictRow.RowState == DataRowState.Deleted)
        //    dbConflictType = ConflictType.RemoteNotExistsLocalIsDeleted;

        else if (remoteConflictRow.rowState == DataRowState.Deleted && localConflictRow?.rowState == DataRowState.Deleted)
            dbConflictType = ConflictType.RemoteIsDeletedLocalIsDeleted
        else if (remoteConflictRow.rowState == DataRowState.Modified && localConflictRow?.rowState == DataRowState.Deleted)
            dbConflictType = ConflictType.RemoteExistsLocalIsDeleted
        else if (remoteConflictRow.rowState == DataRowState.Deleted && localConflictRow?.rowState == DataRowState.Modified)
            dbConflictType = ConflictType.RemoteIsDeletedLocalExists
        else if (remoteConflictRow.rowState == DataRowState.Modified && localConflictRow?.rowState == DataRowState.Modified)
            dbConflictType = ConflictType.RemoteExistsLocalExists

        // Generate the conflict
        val conflict = SyncConflict(dbConflictType)
        conflict.addRemoteRow(remoteConflictRow)

        if (localConflictRow != null)
            conflict.addLocalRow(localConflictRow)

        return conflict
    }

    private fun internalGetConflictRow(
        context: SyncContext,
        syncAdapter: DbSyncAdapter,
        localScopeId: UUID,
        primaryKeyRow: SyncRow,
        schema: SyncTable
    ): SyncRow? {
        // Get the row in the local repository
        syncAdapter.getSelectRow(primaryKeyRow).use { dataReader ->

            // Create a select table based on the schema in parameter + scope columns
            val changesSet = schema.schema!!.clone(false)
            val selectTable = DbSyncAdapter.createChangesTable(schema, changesSet)

            if (!dataReader.moveToNext()) {
                dataReader.close()
                return null
            }

            // Create a new empty row
            val syncRow = selectTable.newRow()
            for (i in 0 until dataReader.columnCount) {
                val columnName = dataReader.getColumnName(i)

                // if we have the tombstone value, do not add it to the table
                if (columnName == "sync_row_is_tombstone") {
                    val isTombstone =
                        dataReader.getInt(i) > 0 // Convert.ToInt64(dataReader.getValue(i)) > 0
                    syncRow.rowState = if (isTombstone)
                        DataRowState.Deleted
                    else
                        DataRowState.Modified
                    continue
                }
                if (columnName == "sync_update_scope_id")
                    continue

//            val columnValueObject = dataReader.getValue(i)
//            val columnValue = if (columnValueObject == null) null else columnValueObject //DBNull.Value
                syncRow[columnName] = dataReader.getValue(i)
            }


            // if syncRow is not a deleted row, we can check for which kind of row it is.
            if (syncRow != null && syncRow.rowState == DataRowState.Unchanged)
                syncRow.rowState = DataRowState.Modified

            return syncRow
        }
    }

    /**
     * Try to report progress
     */
    internal fun reportProgress(
        context: SyncContext,
        progress: Progress<ProgressArgs>?,
        args: ProgressArgs
    ) {
        // Check logger, because we make some reflection here
//        if (this.Logger.IsEnabled(LogLevel.Information))
//        {
//            var argsTypeName = args.GetType().Name.Replace("Args", "")
//            if (this.Logger.IsEnabled(LogLevel.Debug))
//                this.Logger.LogDebug(new EventId(args.EventId, argsTypeName), args.Context)
//            else
//            this.Logger.LogInformation(new EventId(args.EventId, argsTypeName), args)
//        }

        progress?.report(args)
    }

    /**
     * Apply a single update in the current datasource. if forceWrite, override conflict situation and force the update
     */
    private fun internalApplyConflictUpdate(
        context: SyncContext, syncAdapter: DbSyncAdapter,
        row: SyncRow, lastTimestamp: Long?, senderScopeId: UUID?,
        forceWrite: Boolean
    ): Boolean {
        if (row.table == null)
            throw  Exception("ArgumentException: Schema table is not present in the row")

        val rowUpdatedCount =
            syncAdapter.updateRow(senderScopeId, lastTimestamp, false, forceWrite, row)

        return rowUpdatedCount > 0
    }

    /**
     * Apply a delete on a row
     */
    private fun internalApplyConflictDelete(
        context: SyncContext, syncAdapter: DbSyncAdapter,
        row: SyncRow, lastTimestamp: Long?, senderScopeId: UUID?,
        forceWrite: Boolean
    ): Boolean {
        if (row.table == null)
            throw Exception("ArgumentException: Schema table is not present in the row")

        // Check if we have a return value instead
        val rowDeletedCount =
            syncAdapter.deleteRow(senderScopeId, lastTimestamp, true, forceWrite, row)

        return rowDeletedCount > 0
    }

    /**
     * Update a metadata row
     */
    private fun internalUpdateMetadatas(
        context: SyncContext, syncAdapter: DbSyncAdapter,
        row: SyncRow, senderScopeId: UUID?, forceWrite: Boolean
    ): Boolean {
        val metadataUpdatedRowsCount = syncAdapter.updateMetadata(
            senderScopeId,
            row.rowState == DataRowState.Deleted,
            forceWrite,
            row
        )

        return metadataUpdatedRowsCount > 0
    }

    /**
     * Get the rows count cleaned for all tables, during a DeleteMetadatasAsync call
     */
    internal fun internalDeleteMetadatas(
        context: SyncContext, schema: SyncSet, setup: SyncSetup,
        timestampLimit: Long, progress: Progress<ProgressArgs>?
    ): DatabaseMetadatasCleaned {
//        this.intercept(MetadataCleaningArgs(context, this.Setup, timestampLimit, connection, transaction), cancellationToken).ConfigureAwait(false);

        val databaseMetadatasCleaned = DatabaseMetadatasCleaned(timestampLimit = timestampLimit)

        for (syncTable in schema.tables) {
            // Create sync adapter
            val syncAdapter = this.getSyncAdapter(syncTable, setup)

            val rowsCleanedCount = syncAdapter.deleteMetadata(timestampLimit)

            // Only add a new table metadata stats object, if we have, at least, purged 1 or more rows
            if (rowsCleanedCount > 0) {
                val tableMetadatasCleaned = TableMetadatasCleaned(
                    syncTable.tableName,
                    syncTable.schemaName,
                    timestampLimit,
                    rowsCleanedCount
                )

                databaseMetadatasCleaned.tables.add(tableMetadatasCleaned)
            }

        }

//        this.intercept(MetadataCleanedArgs(context, databaseMetadatasCleaned, connection), cancellationToken).ConfigureAwait(false);
        return databaseMetadatasCleaned
    }

    /**
     * Gets a batch of changes to synchronize when given batch size,
     * destination knowledge, and change data retriever parameters.
     * @return A @see DbSyncContext object that will be used to retrieve the modified data.
     */
    internal fun internalGetChanges(
        context: SyncContext,
        message: MessageGetChangesBatch,
        progress: Progress<ProgressArgs>?
    ): Triple<SyncContext, BatchInfo?, DatabaseChangesSelected?> {
        // batch info containing changes
        val batchInfo: BatchInfo

        // Statistics about changes that are selected
        val changesSelected: DatabaseChangesSelected

        if (context.syncWay == SyncWay.Upload && context.syncType == SyncType.Reinitialize) {
            val (batch, changes) = this.internalGetEmptyChanges(message)
            return Triple(context, batch, changes)
        }

        // create local directory
        if (message.batchSize > 0 && message.batchDirectory.isNotBlank() && !File(message.batchDirectory).exists()) {
            File(message.batchDirectory).mkdir()
        }

        changesSelected = DatabaseChangesSelected()

        // numbers of batch files generated
        var batchIndex = 0

        // Check if we are in batch mode
        val isBatch = message.batchSize > 0

        // Create a batch info in memory (if !isBatch) or serialized on disk (if isBatch)
        // batchinfo generate a schema clone with scope columns if needed
        batchInfo = BatchInfo(!isBatch, message.schema, message.batchDirectory)

        // Clean SyncSet, we will add only tables we need in the batch info
        var changesSet = SyncSet()

        var cptSyncTable = 0
        val currentProgress = context.progressPercentage
        for (syncTable in message.schema.tables) {
            // tmp count of table for report progress pct
            cptSyncTable++

            // Only table schema is replicated, no datas are applied
            if (syncTable.syncDirection == SyncDirection.None)
                continue

            // if we are in upload stage, so check if table is not download only
            if (context.syncWay == SyncWay.Upload && syncTable.syncDirection == SyncDirection.DownloadOnly)
                continue

            // if we are in download stage, so check if table is not download only
            if (context.syncWay == SyncWay.Download && syncTable.syncDirection == SyncDirection.UploadOnly)
                continue

            // Get Command
            //var selectIncrementalChangesCommand = this.getSelectChangesCommand(context, syncTable, message.setup, message.isNew)

            // Statistics
            val tableChangesSelected =
                TableChangesSelected(syncTable.tableName, syncTable.schemaName)

            // Create a chnages table with scope columns
            var changesSetTable = DbSyncAdapter.createChangesTable(
                message.schema.tables[syncTable.tableName, syncTable.schemaName]!!,
                changesSet
            )

            val cursor = this.getSelectChangesCursor(
                context,
                syncTable,
                message.setup,
                message.isNew,
                message.lastTimestamp
            )

            // memory size total
            var rowsMemorySize = 0.0

            while (cursor.moveToNext()) {
//                Log.d(TAG, "Reading ${changesSetTable.tableName} row: ${cursor.position} of ${cursor.count}")
                // Create a row from dataReader
                val row = createSyncRowFromReader(cursor, changesSetTable)

                // Add the row to the changes set
                changesSetTable.rows.add(row)

                // Set the correct state to be applied
                if (row.rowState == DataRowState.Deleted)
                    tableChangesSelected.deletes++
                else if (row.rowState == DataRowState.Modified)
                    tableChangesSelected.upserts++

                // calculate row size if in batch mode
                if (isBatch) {
                    val fieldsSize = ContainerTable.getRowSizeFromDataRow(row.toArray())
                    val finalFieldSize = fieldsSize / 1024.0

                    if (finalFieldSize > message.batchSize)
                        throw RowOverSizedException(finalFieldSize.toString())

                    // Calculate the new memory size
                    rowsMemorySize += finalFieldSize

                    // Next line if we don't reach the batch size yet.
                    if (rowsMemorySize <= message.batchSize)
                        continue

                    // Check interceptor
                    val batchTableChangesSelectedArgs = TableChangesSelectedArgs(
                        context,
                        changesSetTable,
                        tableChangesSelected
                    )
                    this.intercept(batchTableChangesSelectedArgs)

                    // add changes to batchinfo
                    batchInfo.addChanges(changesSet, batchIndex, false, this)

                    // increment batch index
                    batchIndex++

                    // we know the datas are serialized here, so we can flush  the set
                    changesSet.clear()

                    // Recreate an empty ContainerSet and a ContainerTable
                    changesSet = SyncSet()

                    changesSetTable = DbSyncAdapter.createChangesTable(
                        message.schema.tables[syncTable.tableName, syncTable.schemaName]!!,
                        changesSet
                    )

                    // Init the row memory size
                    rowsMemorySize = 0.0
                }
            }

            cursor.close()

            // We don't report progress if no table changes is empty, to limit verbosity
            if (tableChangesSelected.deletes > 0 || tableChangesSelected.upserts > 0)
                changesSelected.tableChangesSelected.add(tableChangesSelected)

            // even if no rows raise the interceptor
            val tableChangesSelectedArgs =
                TableChangesSelectedArgs(context, changesSetTable, tableChangesSelected)
            this.intercept(tableChangesSelectedArgs)

            context.progressPercentage =
                currentProgress + (cptSyncTable * 0.2 / message.schema.tables.size)

            // only raise report progress if we have something
            if (tableChangesSelectedArgs.tableChangesSelected.totalChanges > 0)
                this.reportProgress(context, progress, tableChangesSelectedArgs)
        }

        // We are in batch mode, and we are at the last batchpart info
        // Even if we don't have rows inside, we return the changesSet, since it contains at least schema
        if (changesSet != null && changesSet.hasTables && changesSet.hasRows) {
            batchInfo.addChanges(changesSet, batchIndex, true, this)
        }

        //Set the total rows count contained in the batch info
        batchInfo.rowsCount = changesSelected.totalChangesSelected

        // Check the last index as the last batch
        batchInfo.ensureLastBatch()

        // Raise database changes selected
        if (changesSelected.totalChangesSelected > 0 || changesSelected.totalChangesSelectedDeletes > 0 || changesSelected.totalChangesSelectedUpdates > 0) {
            val databaseChangesSelectedArgs = DatabaseChangesSelectedArgs(
                context,
                message.lastTimestamp,
                batchInfo,
                changesSelected
            )
            this.reportProgress(context, progress, databaseChangesSelectedArgs)
            this.intercept(databaseChangesSelectedArgs)
        }

        return Triple(context, batchInfo, changesSelected)
    }

    /**
     * Generate an empty BatchInfo
     */
    internal fun internalGetEmptyChanges(message: MessageGetChangesBatch): Pair<BatchInfo?, DatabaseChangesSelected?> {
        // Get config
        val isBatched = message.batchSize > 0

        // Create the batch info, in memory
        val batchInfo = BatchInfo(!isBatched, message.schema, message.batchDirectory)

        // Create a new empty in-memory batch info
        return Pair(batchInfo, DatabaseChangesSelected())
    }

    /**
     * Delete metadatas items from tracking tables
     */
    open fun deleteMetadatas(
        timeStampStart: Long?,
        progress: Progress<ProgressArgs>? = null
    ): DatabaseMetadatasCleaned? {
        val ctx = getContext()
        ctx.syncStage = SyncStage.MetadataCleaning

        if (timeStampStart == null)
            return null

        // Create a dummy schema to be able to call the DeprovisionAsync method on the provider
        // No need columns or primary keys to be able to deprovision a table
        val schema = SyncSet(this.setup)

        return internalDeleteMetadatas(ctx, schema, setup, timeStampStart, progress);
    }

    internal fun internalDeprovision(
        ctx: SyncContext,
        schema: SyncSet,
        setup: SyncSetup,
        provision: EnumSet<SyncProvision>,
        scope: Any?,
        progress: Progress<ProgressArgs>? = null
    ): Boolean {
        this.intercept(DeprovisioningArgs(ctx, provision, schema))

        // get Database builder
        var builder = this.provider.getDatabaseBuilder()

        // Sorting tables based on dependencies between them
        val schemaTables = schema.tables
            .sortByDependencies({ tab ->
                tab.getRelations()
                    .map { r -> r.getParentTable()!! }
            })

        // Disable check constraints
        if (this.options.disableConstraintsOnApplyChanges)
            for (table in schemaTables.reversed())
                this.internalDisableConstraints(ctx, this.getSyncAdapter(table, setup))


        // Checking if we have to deprovision tables
        val hasDeprovisionTableFlag = provision.contains(SyncProvision.Table)

        // Firstly, removing the flag from the provision, because we need to drop everything in correct order, then drop tables in reverse side
        if (hasDeprovisionTableFlag)
            provision.remove(SyncProvision.Table)

        for (schemaTable in schemaTables) {
            val tableBuilder = this.getTableBuilder(schemaTable, setup)

            if (provision.contains(SyncProvision.StoredProcedures)) {
                val allStoredProcedures = DbStoredProcedureType.values()

                allStoredProcedures.reverse()

                for (storedProcedureType in allStoredProcedures) {
                    // if we are iterating on bulk, but provider do not support it, just loop through and continue
                    if ((storedProcedureType == DbStoredProcedureType.BulkTableType || storedProcedureType == DbStoredProcedureType.BulkUpdateRows || storedProcedureType == DbStoredProcedureType.BulkDeleteRows)
                        && !this.provider.supportBulkOperations
                    )
                        continue;

                    val exists = internalExistsStoredProcedure(
                        ctx,
                        tableBuilder,
                        storedProcedureType,
                        progress
                    )

                    // Drop storedProcedure if already exists
                    if (exists)
                        internalDropStoredProcedure(
                            ctx,
                            tableBuilder,
                            storedProcedureType,
                            progress
                        )
                }

                // Removing cached commands
//                val syncAdapter = this.getSyncAdapter(schemaTable, setup)
//                syncAdapter.removeCommands()
            }

            if (provision.contains(SyncProvision.Triggers)) {
                for (triggerType in DbTriggerType.values()) {
                    val exists = internalExistsTrigger(ctx, tableBuilder, triggerType, progress)

                    // Drop trigger if already exists
                    if (exists)
                        internalDropTrigger(ctx, tableBuilder, triggerType, progress)
                }
            }

            if (provision.contains(SyncProvision.TrackingTable)) {
                val exists = internalExistsTrackingTable(ctx, tableBuilder, progress)

                if (exists)
                    this.internalDropTrackingTable(ctx, setup, tableBuilder, progress)
            }
        }

        // Eventually if we have the "Table" flag, then drop the table
        if (hasDeprovisionTableFlag) {
            for (schemaTable in schemaTables.reversed()) {
                val tableBuilder = this.getTableBuilder(schemaTable, setup)

                val exists = internalExistsTable(ctx, tableBuilder, progress)

                if (exists)
                    this.internalDropTable(ctx, setup, tableBuilder, progress)
            }
        }

        // Get Scope Builder
        val scopeBuilder = this.getScopeBuilder(this.options.scopeInfoTableName)

        var hasDeleteClientScopeTable = false
        var hasDeleteServerScopeTable = false
        if (provision.contains(SyncProvision.ClientScope)) {
            val exists = scopeBuilder.existsScopeInfoTable()

            if (exists) {
                this.internalDropScopeInfoTable(ctx, scopeBuilder, progress)
                hasDeleteClientScopeTable = true
            }
        }

//        if (provision.contains(SyncProvision.ServerScope)) {
//            val exists = this.internalExistsScopeInfoTable(ctx, DbScopeType.Server, scopeBuilder, progress)
//
//            if (exists) {
//                this.internalDropScopeInfoTable(ctx, DbScopeType.Server, scopeBuilder, progress)
//                hasDeleteServerScopeTable = true
//            }
//        }

//        if (provision.contains(SyncProvision.ServerHistoryScope)) {
//            val exists = this.internalExistsScopeInfoTable(ctx, DbScopeType.ServerHistory, scopeBuilder, progress).ConfigureAwait(false);
//
//            if (exists)
//                this.internalDropScopeInfoTable(ctx, DbScopeType.ServerHistory, scopeBuilder, progress)
//        }

        // save scope
        if (this is LocalOrchestrator && !hasDeleteClientScopeTable && scope != null) {
            var clientScopeInfo = scope as ScopeInfo
            clientScopeInfo.schema = null
            clientScopeInfo.setup = null

            val exists = scopeBuilder.existsScopeInfoTable()

            if (exists)
                this.internalSaveScope(clientScopeInfo, scopeBuilder, progress)
        } else if (this is RemoteOrchestrator && !hasDeleteServerScopeTable && scope != null) {
//            var serverScopeInfo = scope as ServerScopeInfo
//            serverScopeInfo.schema = schema
//            serverScopeInfo.setup = setup
//
//            val exists = this.internalExistsScopeInfoTable(ctx, DbScopeType.Server, scopeBuilder, progress)
//
//            if (exists)
//                this.internalSaveScope(ctx, DbScopeType.Server, serverScopeInfo, scopeBuilder, progress)
        }

        val args = DeprovisionedArgs(ctx, provision, schema)
        this.intercept(args)
        this.reportProgress(ctx, progress, args);

        return true
    }

    /**
     * Internal exists storedProcedure procedure routine
     */
    internal fun internalExistsStoredProcedure(
        ctx: SyncContext, tableBuilder: DbTableBuilder,
        storedProcedureType: DbStoredProcedureType,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        val filter = tableBuilder.tableDescription.getFilter()

        // Do not processing with because Sqlite does not support this
        return false

//        val existsCommand = tableBuilder.getExistsStoredProcedureCommandAsync(storedProcedureType, filter, connection, transaction).ConfigureAwait(false);
//
//        if (existsCommand == null)
//            return false
//
//        var existsResultObject = await existsCommand.ExecuteScalarAsync().ConfigureAwait(false);
//        var exists = Convert.ToInt32(existsResultObject) > 0;
//
//        return exists
    }

    /**
     * Internal exists storedProcedure procedure routine
     */
    internal fun internalDropStoredProcedure(
        ctx: SyncContext, tableBuilder: DbTableBuilder,
        storedProcedureType: DbStoredProcedureType,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        val filter = tableBuilder.tableDescription.getFilter()

        // Do not processing with because Sqlite does not support this
        return false

//        val existsCommand = tableBuilder.getExistsStoredProcedureCommand(storedProcedureType, filter)
//
//        if (existsCommand == null)
//            return false
//
//        var existsResultObject = await existsCommand . ExecuteScalarAsync ().ConfigureAwait(false)
//        var exists = Convert.ToInt32(existsResultObject) > 0
//
//        return exists
    }

    /**
     * Internal exists trigger procedure routine
     */
    internal fun internalExistsTrigger(
        ctx: SyncContext, tableBuilder: DbTableBuilder,
        triggerType: DbTriggerType,
        progress: Progress<ProgressArgs>? = null
    ): Boolean {
        // Get exists command
//        val existsCommand =  tableBuilder.getExistsTriggerCommand(triggerType)
//
//        if (existsCommand == null)
//            return false
//
//        var existsResultObject = await existsCommand.ExecuteScalarAsync()
//        var exists = Convert.ToInt32(existsResultObject) > 0
        return tableBuilder.existsTrigger(triggerType)
    }

    /**
     * Internal drop trigger routine
     */
    internal fun internalDropTrigger(
        ctx: SyncContext, tableBuilder: DbTableBuilder,
        triggerType: DbTriggerType, progress: Progress<ProgressArgs>?
    ): Boolean {
        tableBuilder.dropTrigger(triggerType)
        return true
    }

    /**
     * Internal drop tracking table routine
     */
    internal fun internalDropTrackingTable(
        ctx: SyncContext, setup: SyncSetup,
        tableBuilder: DbTableBuilder,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        tableBuilder.dropTrackingTable()
        val (_, trackingTableName) = this.provider.getParsers(tableBuilder.tableDescription, setup)
        return true
    }

    /**
     * Internal drop table routine
     */
    internal fun internalDropTable(
        ctx: SyncContext, setup: SyncSetup, tableBuilder: DbTableBuilder,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        tableBuilder.dropTable()
        val (tableName, _) = this.provider.getParsers(tableBuilder.tableDescription, setup)
        return true
    }

    /**
     * Internal drop scope info table routine
     */
    internal fun internalDropScopeInfoTable(
        ctx: SyncContext, scopeBuilder: DbScopeBuilder,
        progress: Progress<ProgressArgs>?
    ): Boolean {
        scopeBuilder.dropScopeInfoTable()
        return true
    }

    companion object {
        private val TAG = BaseOrchestrator::class.java.simpleName
    }
}

/**
 * Sorts an enumeration based on dependency
 * @param dependencies dependencies
 * @param throwOnCycle if true throw exception if Cyclic dependency found
 * @param defaultCapacity default capacity of sorterd buffer
 */
private fun List<SyncTable>.sortByDependencies(
    dependencies: (SyncTable) -> List<SyncTable>,
    throwOnCycle: Boolean = false,
    defaultCapacity: Int = 10
): List<SyncTable> {
    val defaultCapacity = this.size + 1
    val sorted = ArrayList<SyncTable>(defaultCapacity)
    val visited = HashSet<SyncTable>()

    for (item in this) {
        visit(item, visited, sorted, dependencies, throwOnCycle)
    }

    return sorted
}

private fun visit(
    item: SyncTable,
    visited: HashSet<SyncTable>,
    sorted: MutableList<SyncTable>,
    dependencies: (SyncTable) -> List<SyncTable>,
    throwOnCycle: Boolean
) {
    if (!visited.contains(item)) {
        visited.add(item)

        for (dep in dependencies.invoke(item))
            visit(dep, visited, sorted, dependencies, throwOnCycle)

        sorted.add(item)
    } else {
        if (throwOnCycle && !sorted.contains(item))
            throw Exception("Cyclic dependency found")
    }
}
