package com.mimetis.dotmim.sync

import android.util.Log
import com.mimetis.dotmim.sync.args.ProgressArgs
import com.mimetis.dotmim.sync.enumerations.ConflictResolutionPolicy
import com.mimetis.dotmim.sync.enumerations.SyncProvision
import com.mimetis.dotmim.sync.enumerations.SyncType
import com.mimetis.dotmim.sync.orchestrators.LocalOrchestrator
import com.mimetis.dotmim.sync.orchestrators.RemoteOrchestrator
import com.mimetis.dotmim.sync.parameter.SyncParameters
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.util.*

class SyncAgent(
        clientProvider: CoreProvider,
        val remoteOrchestrator: RemoteOrchestrator,
        val options: SyncOptions,
        val setup: SyncSetup,
        private val scopeName: String = SyncOptions.DefaultScopeName
) {
    val localOrchestrator: LocalOrchestrator = LocalOrchestrator(clientProvider, options, setup, scopeName)
    private lateinit var schema: SyncSet
    val parameters: SyncParameters = SyncParameters()

    suspend fun synchronize(syncType: SyncType = SyncType.Normal, progress: Progress<ProgressArgs>? = null): SyncResult {
        val startTime = utcNow()
        var completeTime = utcNow()

        val context = SyncContext(
                sessionId = UUID.randomUUID(),
                scopeName = this.scopeName,
                parameters = this.parameters,
                syncType = syncType
        )

        // Result, with sync results stats.
        val result = SyncResult(
                context.sessionId,
                startTime,
                completeTime,
        )

        try {
            var serverScopeInfo: ServerScopeInfo?

            this.localOrchestrator.syncContext = context
            this.remoteOrchestrator.syncContext = context
            this.localOrchestrator.startTime = startTime
            this.remoteOrchestrator.startTime = startTime

            localOrchestrator.beginSession(progress)

            var clientScopeInfo = localOrchestrator.getClientScope(progress)

            context.clientScopeId = clientScopeInfo.id

            // if client is new or else schema does not exists
            // We need to get it from server
            if (clientScopeInfo.isNewScope || clientScopeInfo.schema == null) {
                // Ensure schema is defined on remote side
                // This action will create schema on server if needed
                // if schema already exists on server, then the server setup will be compared with this one
                // if setup is different, it will be migrated.
                // so serverScopeInfo.Setup MUST be equal to this.Setup
                serverScopeInfo = this.remoteOrchestrator.ensureSchema(progress)

                // Affect local setup since the setup could potentially comes from Web server
                // Affect local setup (equivalent to this.Setup)
                if (!this.setup.equalsByProperties(serverScopeInfo.setup) && !this.setup.hasTables)
                    this.localOrchestrator.setup = serverScopeInfo.setup

                // Provision local database
                val provision = EnumSet.of(SyncProvision.Table, SyncProvision.TrackingTable, SyncProvision.StoredProcedures, SyncProvision.Triggers)
                this.localOrchestrator.provision(serverScopeInfo.schema!!, provision, false, clientScopeInfo, progress)

                // Set schema for agent, just to let the opportunity to user to use it.
                this.schema = serverScopeInfo.schema!!
            } else {
                // Do we need to upgrade ?
                if (localOrchestrator.internalNeedsToUpgrade(context, clientScopeInfo)) {
                    val newScope = this.localOrchestrator.upgrade(clientScopeInfo, progress)
                    if (newScope != null)
                        clientScopeInfo = newScope
                }

                // on remote orchestrator get scope info as well
                // if setup is different, it will be migrated.
                // so serverScopeInfo.Setup MUST be equal to this.Setup
                serverScopeInfo = this.remoteOrchestrator.getServerScope(progress)

                // compare local setup options with setup provided on SyncAgent constructor (check if pref / suf have changed)
                val hasSameOptions = clientScopeInfo.setup!!.hasSameOptions(this.setup)

                // compare local setup strucutre with remote structure
                val hasSameStructure = clientScopeInfo.setup!!.hasSameStructure(serverScopeInfo.setup)

                if (hasSameStructure) {
                    // Sett schema & setup
                    this.schema = clientScopeInfo.schema!!

                    //schema could be null if from web server
                    if (serverScopeInfo.schema == null)
                        serverScopeInfo.schema = clientScopeInfo.schema
                } else {
                    // Get full schema from server
                    serverScopeInfo = this.remoteOrchestrator.ensureSchema(progress)

                    // Set the correct schema
                    this.schema = serverScopeInfo.schema!!
                }

                // Affect local setup (equivalent to this.Setup)
                this.localOrchestrator.setup.filters = serverScopeInfo.setup.filters
                this.localOrchestrator.setup.tables = serverScopeInfo.setup.tables
                //this.LocalOrchestrator.Setup.Version = serverScopeInfo.Setup.Version

                // If one of the comparison is false, we make a migration
                if (!hasSameOptions || !hasSameStructure) {
                    this.localOrchestrator.migration(clientScopeInfo.setup!!, serverScopeInfo.schema!!, progress)
                    clientScopeInfo.setup = this.setup
                    clientScopeInfo.schema = serverScopeInfo.schema
                }

                // get scope again
                clientScopeInfo.schema = serverScopeInfo.schema
                clientScopeInfo.setup = serverScopeInfo.setup
            }

            // Before call the changes from localorchestrator, check if we are outdated
            if (context.syncType != SyncType.Reinitialize && context.syncType != SyncType.ReinitializeWithUpload) {
                val isOutDated = this.localOrchestrator.isOutDated(clientScopeInfo, serverScopeInfo)

                // if client does not change SyncType to Reinitialize / ReinitializeWithUpload on SyncInterceptor, we raise an error
                // otherwise, we are outdated, but we can continue, because we have a new mode.
                if (isOutDated)
                    Log.w(TAG, "Client id outdated, but we change mode to ${context.syncType}")
            }

            context.progressPercentage = 0.1

            // On local orchestrator, get local changes
            val clientChanges = this.localOrchestrator.getChanges(clientScopeInfo, progress)

            // Reinitialize timestamp is in Reinit Mode
            if (context.syncType == SyncType.Reinitialize || context.syncType == SyncType.ReinitializeWithUpload)
                clientScopeInfo.lastServerSyncTimestamp = null

            // Get if we need to get all rows from the datasource
            val fromScratch = clientScopeInfo.isNewScope || context.syncType == SyncType.Reinitialize || context.syncType == SyncType.ReinitializeWithUpload

            // IF is new and we have a snapshot directory, try to apply a snapshot
            if (fromScratch) {
                // Get snapshot files
                val serverSnapshotChanges = this.remoteOrchestrator.getSnapshot(this.schema, progress)

                // Apply snapshot
                if (serverSnapshotChanges.second != null) {
                    val (snapshotChangesAppliedOnClient, clientSI) = this.localOrchestrator.applySnapshot(clientScopeInfo, serverSnapshotChanges.second, clientChanges.first, serverSnapshotChanges.first, serverSnapshotChanges.third, progress)
                    result.snapshotChangesAppliedOnClient = snapshotChangesAppliedOnClient
                    clientScopeInfo = clientSI
                }
            }

            context.progressPercentage = 0.3
            // apply is 25%, get changes is 20%
            val serverChanges = this.remoteOrchestrator.applyThenGetChanges(clientScopeInfo, clientChanges.second, progress)

            // Policy is always Server policy, so reverse this policy to get the client policy
            val reverseConflictResolutionPolicy = if (serverChanges.value3 == ConflictResolutionPolicy.ServerWins) ConflictResolutionPolicy.ClientWins else ConflictResolutionPolicy.ServerWins

            // Get if we have already applied a snapshot, so far we don't need to reset table even if we are i Reinitialize Mode
            val snapshotApplied = result.snapshotChangesAppliedOnClient != null

            // apply is 25%
            context.progressPercentage = 0.75
            val clientChangesApplied = this.localOrchestrator.applyChanges(
                    clientScopeInfo, this.schema, serverChanges.value2,
                    clientChanges.first, serverChanges.value1, reverseConflictResolutionPolicy, snapshotApplied,
                    serverChanges.value5!!, progress)

            completeTime = utcNow()
            this.localOrchestrator.completeTime = completeTime
            this.remoteOrchestrator.completeTime = completeTime

            result.completeTime = completeTime

            // All clients changes selected
            result.clientChangesSelected = clientChanges.third
            result.serverChangesSelected = serverChanges.value5
            result.changesAppliedOnClient = clientChangesApplied.first
            result.changesAppliedOnServer = serverChanges.value4!!

            // Begin session
            context.progressPercentage = 1.0
            this.localOrchestrator.endSession(progress)

        } catch (ex: Throwable) {
            throw ex
        } finally {
//            this.sessionState = SyncSessionState.Ready
//            this.SessionStateChanged?.Invoke(this, this.SessionState);
        }

        return result
    }

    init {
        remoteOrchestrator.options = options
        remoteOrchestrator.setup = setup
        remoteOrchestrator.scopeName = scopeName
    }

    companion object {
        private val TAG = SyncAgent::class.java.simpleName
    }
}
