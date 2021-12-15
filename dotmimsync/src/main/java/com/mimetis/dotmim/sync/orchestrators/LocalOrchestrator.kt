package com.mimetis.dotmim.sync.orchestrators

import com.mimetis.dotmim.sync.*
import com.mimetis.dotmim.sync.SyncVersion.major
import com.mimetis.dotmim.sync.SyncVersion.toVersionInt
import com.mimetis.dotmim.sync.args.ProgressArgs
import com.mimetis.dotmim.sync.args.SessionBeginArgs
import com.mimetis.dotmim.sync.args.SessionEndArgs
import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.builders.DbScopeBuilder
import com.mimetis.dotmim.sync.builders.DbScopeType
import com.mimetis.dotmim.sync.enumerations.ConflictResolutionPolicy
import com.mimetis.dotmim.sync.enumerations.SyncProvision
import com.mimetis.dotmim.sync.enumerations.SyncStage
import com.mimetis.dotmim.sync.enumerations.SyncWay
import com.mimetis.dotmim.sync.messages.*
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.util.*

class LocalOrchestrator(
    provider: CoreProvider,
    options: SyncOptions,
    setup: SyncSetup,
    scopeName: String = SyncOptions.DefaultScopeName
) : BaseOrchestrator(provider, options, setup, scopeName) {
    fun beginSession(progress: Progress<ProgressArgs>?) {
        if (startTime == null)
            startTime = utcNow()

        val ctx = getContext()
        ctx.syncStage = SyncStage.BeginSession

        // Progress & interceptor
        val sessionArgs = SessionBeginArgs(ctx)
        this.intercept(sessionArgs)
        this.reportProgress(ctx, progress, sessionArgs)
    }

    /**
     * Called when the sync is over
     */
    fun endSession(progress: Progress<ProgressArgs>?) {
        if (startTime == null)
            startTime = utcNow()

        val ctx = getContext()
        ctx.syncStage = SyncStage.EndSession

        // Progress & interceptor
        val sessionArgs = SessionEndArgs(ctx)
        this.intercept(sessionArgs)
        this.reportProgress(ctx, progress, sessionArgs)
    }

    /**
     * Get the local configuration, ensures the local scope is created
     * @return current context, the local scope info created or get from the database and the configuration from the client if changed
     */
    fun getClientScope(progress: Progress<ProgressArgs>? = null): ScopeInfo {
        val ctx = getContext()
        ctx.syncStage = SyncStage.ScopeLoading
        val scopeBuilder = getScopeBuilder(this.options.scopeInfoTableName)
        val exists = scopeBuilder.existsScopeInfoTable()
        if (!exists)
            internalCreateScopeInfoTable(ctx, DbScopeType.Client, scopeBuilder, progress)
        return internalGetScope(ctx, DbScopeType.Client, scopeName, scopeBuilder, progress)
    }

    fun internalNeedsToUpgrade(context: SyncContext, scopeInfo: ScopeInfo): Boolean {
        val version = SyncVersion.ensureVersion(scopeInfo.version)
        return version.toVersionInt() < SyncVersion.current.toVersionInt()
    }

    /**
     * Upgrade the database structure to reach the last DMS version
     */
    fun upgrade(scopeInfo: ScopeInfo? = null, progress: Progress<ProgressArgs>?): ScopeInfo? {
        val ctx = getContext()
        ctx.syncStage = SyncStage.Provisioning

        val dbBuilder = provider.getDatabaseBuilder()

        // Initialize database if needed
        dbBuilder.ensureDatabase()

        val builder = this.getScopeBuilder(this.options.scopeInfoTableName)

        var scopeInfo = scopeInfo
        if (scopeInfo == null || !scopeInfo.schema!!.hasTables) {
            val exists = builder.existsScopeInfoTable()

            if (exists)
                scopeInfo = this.internalGetScope(ctx, DbScopeType.Client, this.scopeName, builder, progress)

            if (scopeInfo == null)
                throw MissingClientScopeInfoException()
        }

        val setup = scopeInfo.setup ?: this.setup
        // Get schema
        val schema = scopeInfo.schema ?: this.internalGetSchema(ctx, setup, progress)

        // If schema does not have any table, raise an exception
        if (schema?.tables == null || !schema.hasTables)
            throw  MissingTablesException()

        return internalUpgrade(ctx, schema, setup, scopeInfo, builder, progress)
    }

    /**
     * Migrate an old setup configuration to a new one. This method is usefull if you are changing your SyncSetup when a database has been already configured previously
     */
    fun migration(oldSetup: SyncSetup, schema: SyncSet, progress: Progress<ProgressArgs>?) {
        val ctx = getContext()
        ctx.syncStage = SyncStage.Migrating
        // Migrate the db structure
        this.internalMigration(ctx, schema, oldSetup, this.setup, progress)

        // Get Scope Builder
        val scopeBuilder = this.getScopeBuilder(this.options.scopeInfoTableName)

        val localScope: ScopeInfo?

        val exists = scopeBuilder.existsScopeInfoTable()

        if (!exists)
            scopeBuilder.createScopeInfoTable()

        localScope = this.internalGetScope(ctx, DbScopeType.Client, this.scopeName, scopeBuilder, progress) ?: return

        localScope.setup = this.setup
        localScope.schema = schema

        this.internalSaveScope(ctx, localScope, scopeBuilder, progress)
    }

    /**
     * Get changes from local database
     */
    fun getChanges(
        localScopeInfo: ScopeInfo? = null,
        progress: Progress<ProgressArgs>?
    ): Triple<Long, BatchInfo?, DatabaseChangesSelected?> {
        val ctx = getContext()
        ctx.syncStage = SyncStage.ChangesSelecting
        var scopeInfo = localScopeInfo

        // Output
        // Output
        val clientTimestamp: Long
        val clientBatchInfo: BatchInfo?
        val clientChangesSelected: DatabaseChangesSelected?

        // Get local scope, if not provided
        if (scopeInfo == null) {
            val scopeBuilder = this.getScopeBuilder(this.options.scopeInfoTableName)

            val exists = scopeBuilder.existsScopeInfoTable()

            if (!exists)
                scopeBuilder.createScopeInfoTable()

            scopeInfo = this.internalGetScope(ctx, DbScopeType.Client, this.scopeName, scopeBuilder, progress)
        }

        // If no schema in the client scope. Maybe the client scope table does not exists, or we never get the schema from server
        if (scopeInfo.schema == null)
            throw MissingLocalOrchestratorSchemaException()

        // On local, we don't want to chase rows from "others"
        // We just want our local rows, so we dont exclude any remote scope id, by setting scope id to NULL
        val remoteScopeId: UUID? = null
        // lastSyncTS : get lines inserted / updated / deteleted after the last sync commited
        val lastSyncTS = scopeInfo.lastSyncTimestamp
        // isNew : If isNew, lasttimestamp is not correct, so grab all
        val isNew = scopeInfo.isNewScope
        //Direction set to Upload
        ctx.syncWay = SyncWay.Upload

        // JUST before the whole process, get the timestamp, to be sure to
        // get rows inserted / updated elsewhere since the sync is not over
        clientTimestamp = this.internalGetLocalTimestamp()

        // Creating the message
        val message = MessageGetChangesBatch(
            remoteScopeId,
            scopeInfo.id,
            isNew,
            lastSyncTS,
            scopeInfo.schema!!,
            this.setup,
            this.options.batchSize,
            this.options.batchDirectory
        )

        // Locally, if we are new, no need to get changes
        if (isNew) {
            val (emptyBatch, emptyChanges) = this.internalGetEmptyChanges(message)
            clientBatchInfo = emptyBatch
            clientChangesSelected = emptyChanges
        } else {
            val (_, batch, changes) = this.internalGetChanges(ctx, message, progress)
            clientBatchInfo = batch
            clientChangesSelected = changes
        }

        return Triple(clientTimestamp, clientBatchInfo, clientChangesSelected)
    }

    private fun internalUpgrade(
        context: SyncContext, schema: SyncSet, setup: SyncSetup,
        scopeInfo: ScopeInfo, builder: DbScopeBuilder,
        progress: Progress<ProgressArgs>?
    ): ScopeInfo {
        val version = SyncVersion.ensureVersion(scopeInfo.version)
        val oldVersion = version

        if (version.major() == 0) { //Major==0
//            if (version.minor()<=5)
//                version = upgdrateTo600Async(context, schema)
//
//            if (version.minor() == 6 && version.build() == 0)
//                version = upgdrateTo601Async(context, schema)
//
//            if (version.minor() == 6 && version.build() == 1)
//                version = upgdrateTo602Async(context, schema)

//            // last version of 0.6 Can be 0.6.2 or beta version 0.6.3 (that will never be released but still in the nuget packages available)
//            if (version.minor() == 6 && version.build() >= 2)
//                version = upgdrateTo700Async(context, schema)
        }

        var si = scopeInfo
        if (oldVersion.toVersionInt() != version.toVersionInt()) {
            si.version = version
            si = this.internalSaveScope(context, scopeInfo, builder, progress)
        }
        return si
    }

    /**
     * Apply a snapshot locally
     */
    internal fun applySnapshot(
        clientScopeInfo: ScopeInfo, serverBatchInfo: BatchInfo?,
        clientTimestamp: Long, remoteClientTimestamp: Long,
        databaseChangesSelected: DatabaseChangesSelected,
        progress: Progress<ProgressArgs>?
    ): Pair<DatabaseChangesApplied, ScopeInfo> {
        if (serverBatchInfo == null)
            return Pair(DatabaseChangesApplied(), clientScopeInfo)

        // Get context or create a new one
        val ctx = this.getContext()

        // store value
        val isNew = clientScopeInfo.isNewScope

        ctx.syncStage = SyncStage.SnapshotApplying

        // Applying changes and getting the new client scope info
        val (changesApplied, newClientScopeInfo) = this.applyChanges(
            clientScopeInfo,
            clientScopeInfo.schema!!,
            serverBatchInfo,
            clientTimestamp,
            remoteClientTimestamp,
            ConflictResolutionPolicy.ServerWins,
            false,
            databaseChangesSelected,
            progress
        )

        // re-apply scope is new flag
        newClientScopeInfo.isNewScope = isNew

        return Pair(changesApplied, newClientScopeInfo)
    }

    /**
     * Apply changes locally
     */
    internal fun applyChanges(
        scope: ScopeInfo, schema: SyncSet, serverBatchInfo: BatchInfo,
        clientTimestamp: Long, remoteClientTimestamp: Long,
        policy: ConflictResolutionPolicy, snapshotApplied: Boolean,
        allChangesSelected: DatabaseChangesSelected,
        progress: Progress<ProgressArgs>?
    ): Pair<DatabaseChangesApplied, ScopeInfo> {
        var ctx = getContext()
        ctx.syncStage = SyncStage.ChangesApplying

        var clientChangesApplied: DatabaseChangesApplied? = null

        // lastSyncTS : apply lines only if they are not modified since last client sync
        val lastSyncTS = scope.lastSyncTimestamp
        // isNew : if IsNew, don't apply deleted rows from server
        val isNew = scope.isNewScope
        // We are in downloading mode
        ctx.syncWay = SyncWay.Download

        // Create the message containing everything needed to apply changes
        val applyChanges = MessageApplyChanges(
            scope.id,
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            isNew,
            lastSyncTS,
            schema,
            this.setup,
            policy,
            this.options.disableConstraintsOnApplyChanges,
            this.options.useBulkOperations,
            this.options.cleanMetadatas,
            this.options.cleanFolder,
            snapshotApplied,
            serverBatchInfo
        )

        // Call apply changes on provider
        val (ctxNew, clientChangesAppliedNew) = this.internalApplyChanges(
            ctx,
            applyChanges,
            progress
        )
        ctx = ctxNew
        clientChangesApplied = clientChangesAppliedNew

        // check if we need to delete metadatas
        if (this.options.cleanMetadatas && clientChangesApplied.totalAppliedChanges > 0 && lastSyncTS != null)
            this.internalDeleteMetadatas(ctx, schema, this.setup, lastSyncTS, progress)

        // now the sync is complete, remember the time
        this.completeTime = utcNow()

        // generate the new scope item
        scope.isNewScope = false
        scope.lastSync = this.completeTime
        scope.lastSyncTimestamp = clientTimestamp
        scope.lastServerSyncTimestamp = remoteClientTimestamp
        scope.lastSyncDuration = this.completeTime!! - this.startTime!!
        scope.setup = this.setup

        // Write scopes locally
        val scopeBuilder = this.getScopeBuilder(this.options.scopeInfoTableName)

        this.internalSaveScope(ctx, scope, scopeBuilder, progress)

        return Pair(clientChangesApplied, scope)
    }

    /**
     * Update all untracked rows from the client database
     */
    fun updateUntrackedRows(schema: SyncSet, progress: Progress<ProgressArgs>? = null): Boolean {
        // If schema does not have any table, just return
        if (!schema.hasTables)
            throw MissingTablesException()

        getContext().syncStage = SyncStage.ChangesApplying

        // Update untracked rows
        for (table in schema.tables) {
            val syncAdapter = this.getSyncAdapter(table, this.setup)
            this.internalUpdateUntrackedRows(this.getContext(), syncAdapter)
        }

        return true
    }

    /**
     * Internal update untracked rows routine
     */
    private fun internalUpdateUntrackedRows(ctx: SyncContext, syncAdapter: DbSyncAdapter): Int {
        // Get table builder
        val tableBuilder = getTableBuilder(syncAdapter.tableDescription, syncAdapter.setup)

        // Check if tracking table exists
        val trackingTableExists = tableBuilder.existsTrackingTable()

        if (!trackingTableExists)
            throw MissingTrackingTableException(tableBuilder.tableDescription.getFullName())


        return syncAdapter.updateUntrackedRows()
    }

    /**
     * Provision the local database based on the schema parameter, and the provision enumeration
     */
    fun provision(
        schema: SyncSet,
        provision: EnumSet<SyncProvision>,
        overwrite: Boolean = false,
        clientScopeInfo: ScopeInfo? = null,
        progress: Progress<ProgressArgs>?
    ): SyncSet {
        val ctx = getContext()
        ctx.syncStage = SyncStage.Provisioning

        var clientSI = clientScopeInfo
        // Get server scope if not supplied
        if (clientSI == null) {
            val scopeBuilder = this.getScopeBuilder(this.options.scopeInfoTableName)

            val exists = scopeBuilder.existsScopeInfoTable()

            if (exists)
                clientSI = this.internalGetScope(ctx, DbScopeType.Client, this.scopeName, scopeBuilder, progress)
        }

        return internalProvision(ctx, overwrite, schema, this.setup, provision, clientSI, progress)
    }

    /**
     * Delete all metadatas from tracking tables, based on min timestamp from scope info table
     */
    fun deleteMetadatas(progress: Progress<ProgressArgs>? = null): DatabaseMetadatasCleaned? {
        if (this.startTime == null)
            this.startTime = utcNow()

        // Get the min timestamp, where we can without any problem, delete metadatas
        val clientScopeInfo = this.getClientScope(progress)

        if (clientScopeInfo.lastSyncTimestamp == 0L)
            return DatabaseMetadatasCleaned()

        return deleteMetadatas(clientScopeInfo.lastSyncTimestamp, progress)
    }

    /**
     * Deprovision the orchestrator database based on the provision enumeration
     */
    open fun deprovision(
        provision: EnumSet<SyncProvision>,
        clientScopeInfo: ScopeInfo? = null,
        progress: Progress<ProgressArgs>? = null
    ): Boolean {
        // Create a temporary SyncSet for attaching to the schemaTable
        val tmpSchema = SyncSet()

        // Add this table to schema
        for (table in this.setup.tables)
            tmpSchema.tables.add(SyncTable(table.tableName, table.schemaName))

        tmpSchema.ensureSchema()

        // copy filters from old setup
        for (filter in this.setup.filters)
            tmpSchema.filters.add(filter)

        val isDeprovisioned = this.deprovision(tmpSchema, provision, clientScopeInfo, progress)
        return isDeprovisioned;
    }

    /**
     * Deprovision the orchestrator database based on the schema argument, and the provision enumeration
     */
    fun deprovision(
        schema: SyncSet,
        provision: EnumSet<SyncProvision>,
        clientScopeInfo: ScopeInfo? = null,
        progress: Progress<ProgressArgs>? = null
    ): Boolean {
        val ctx = getContext()
        ctx.syncStage = SyncStage.Deprovisioning

        var clientScopeInfo = clientScopeInfo
        // Get server scope if not supplied
        if (clientScopeInfo == null) {
            val scopeBuilder = this.getScopeBuilder(this.options.scopeInfoTableName)

            val exists = scopeBuilder.existsScopeInfoTable()

            if (exists)
                clientScopeInfo =
                    this.internalGetScope(ctx, DbScopeType.Client, this.scopeName, scopeBuilder, progress)
        }

        val isDeprovisioned =
            internalDeprovision(ctx, schema, this.setup, provision, clientScopeInfo, progress)

        return isDeprovisioned
    }
}
