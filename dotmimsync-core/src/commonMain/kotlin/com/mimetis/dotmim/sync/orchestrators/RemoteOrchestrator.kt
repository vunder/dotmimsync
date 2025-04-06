package com.mimetis.dotmim.sync.orchestrators

import com.mimetis.dotmim.sync.CoreProvider
import com.mimetis.dotmim.sync.Progress
import com.mimetis.dotmim.sync.args.ProgressArgs
import com.mimetis.dotmim.sync.SyncOptions
import com.mimetis.dotmim.sync.Tuple
import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.enumerations.ConflictResolutionPolicy
import com.mimetis.dotmim.sync.messages.DatabaseChangesApplied
import com.mimetis.dotmim.sync.messages.DatabaseChangesSelected
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup

abstract class RemoteOrchestrator(
        provider: CoreProvider,
        options: SyncOptions,
        setup: SyncSetup,
        scopeName: String = SyncOptions.DefaultScopeName
) : BaseOrchestrator(provider, options, setup, scopeName) {
    abstract suspend fun ensureSchema(progress: Progress<ProgressArgs>?): ServerScopeInfo
    abstract suspend fun getServerScope(progress: Progress<ProgressArgs>?): ServerScopeInfo
    abstract suspend fun getSnapshot(schema: SyncSet? = null, progress: Progress<ProgressArgs>? = null): Triple<Long, BatchInfo?, DatabaseChangesSelected>

    /**
     * Apply changes on remote provider
     */
    abstract suspend fun applyThenGetChanges(scope: ScopeInfo, clientBatchInfo: BatchInfo?,
                                     progress: Progress<ProgressArgs>?
    ): Tuple<Long, BatchInfo, ConflictResolutionPolicy, DatabaseChangesApplied?, DatabaseChangesSelected?>
}
