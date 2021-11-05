package com.mimetis.dotmim.sync.web.client

import android.util.Log
import com.mimetis.dotmim.sync.*
import com.mimetis.dotmim.sync.args.*
import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.batch.BatchPartInfo
import com.mimetis.dotmim.sync.enumerations.ConflictResolutionPolicy
import com.mimetis.dotmim.sync.enumerations.SyncStage
import com.mimetis.dotmim.sync.messages.DatabaseChangesApplied
import com.mimetis.dotmim.sync.messages.DatabaseChangesSelected
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.orchestrators.RemoteOrchestrator
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo
import com.mimetis.dotmim.sync.serialization.Converter
import com.mimetis.dotmim.sync.set.ContainerSet
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class WebClientOrchestrator(
    private val authHeader: String,
    private val converter: Converter? = null,
    private val maxDownladingDegreeOfParallelism: Int = 4
) : RemoteOrchestrator(FancyCoreProvider(), SyncOptions(), SyncSetup()) {
    private val TAG = this::class.java.simpleName
    private val service = DotmimServiceImpl()

    /**
     * Send a request to remote web proxy for First step : Ensure scopes and schema
     */
    override suspend fun ensureSchema(progress: Progress<ProgressArgs>?): ServerScopeInfo {
        val ctx = getContext()
        ctx.syncStage = SyncStage.SchemaReading
        if (startTime == null)
            startTime = utcNow()

        // Raise progress for sending request and waiting server response
        val sendingRequestArgs = HttpGettingSchemaRequestArgs(ctx, this.getServiceHost())
        this.intercept(sendingRequestArgs)
        this.reportProgress(ctx, progress, sendingRequestArgs)

        val httpMessage = HttpMessageEnsureScopesRequest(ctx)
        val ensureScopesResponse = try {
            service.ensureSchema(authHeader, httpMessage, converter)
        } catch (exception: Exception) {
//            CrashesHelper.logError(exception, "WebClientOrchestrator.service.ensureSchema")
            throw exception
        }

        if (ensureScopesResponse.serverScopeInfo == null || ensureScopesResponse.schema == null || ensureScopesResponse.schema.tables.size <= 0)
            throw Exception("ArgumentException: Schema from EnsureScope can't be null and may contains at least one table")

        ensureScopesResponse.schema.ensureSchema()
        ensureScopesResponse.serverScopeInfo.schema = ensureScopesResponse.schema

        // Affect local setup
        this.setup = ensureScopesResponse.serverScopeInfo.setup

        // Reaffect context
        setContext(ensureScopesResponse.syncContext)

        // Report progress
        val args = HttpGettingSchemaResponseArgs(
            ensureScopesResponse.serverScopeInfo,
            ensureScopesResponse.schema,
            ensureScopesResponse.syncContext,
            this.getServiceHost()
        )
        this.intercept(args)

        // Return scopes and new shema
        return ensureScopesResponse.serverScopeInfo
    }

    /**
     * Get server scope from server, by sending an http request to the server
     */
    override suspend fun getServerScope(progress: Progress<ProgressArgs>?): ServerScopeInfo {
        val ctx = getContext()
        ctx.syncStage = SyncStage.ScopeLoading

        if (this.startTime == null)
            this.startTime = utcNow()

        // Raise progress for sending request and waiting server response
        val sendingRequestArgs = HttpGettingScopeRequestArgs(ctx, this.getServiceHost())
        this.intercept(sendingRequestArgs)
        this.reportProgress(ctx, progress, sendingRequestArgs)

        val args = HttpMessageEnsureScopesRequest(ctx)
        val ensureScopesResponse = service.ensureScope(authHeader, args, this.converter)

        // Affect local setup
        this.setup = ensureScopesResponse.serverScopeInfo.setup

        // Reaffect context
        this.setContext(ensureScopesResponse.syncContext)

        // Return scopes and new shema
        return ensureScopesResponse.serverScopeInfo
    }

    override suspend fun getSnapshot(
        schema: SyncSet?,
        progress: Progress<ProgressArgs>?
    ): Triple<Long, BatchInfo?, DatabaseChangesSelected> {
        var ctx = getContext()
        var schema = schema

        if (this.startTime == null)
            this.startTime = utcNow()

        // Make a remote call to get Schema from remote provider
        if (schema == null) {
            val serverScopeInfo = this.ensureSchema(progress)
            schema = serverScopeInfo.schema
            schema?.ensureSchema()
        }

        ctx.syncStage = SyncStage.SnapshotApplying

        // Generate a batch directory
        val batchDirectoryRoot = this.options.batchDirectory
        val batchDirectoryName = SimpleDateFormat("yyyy_MM_dd_ss", Locale.getDefault()).format(
            Calendar.getInstance(Locale.getDefault()).time
        ) + UUID.randomUUID().toString().replace(".", "")
        val batchDirectoryFullPath = File(batchDirectoryRoot, batchDirectoryName).absolutePath

        // Create the BatchInfo serialized (forced because in a snapshot call, so we are obviously serialized on disk)
        val serverBatchInfo = BatchInfo(false, schema!!, batchDirectoryRoot, batchDirectoryName)

        // Firstly, get the snapshot summary
        val changesToSend = HttpMessageSendChangesRequest(ctx, null)
        val summaryResponseContent = service.getSummary(authHeader, changesToSend, this.converter)
        serverBatchInfo.rowsCount = summaryResponseContent.batchInfo?.rowsCount ?: 0
        if (summaryResponseContent.batchInfo?.batchPartsInfo != null)
            for (bpi in summaryResponseContent.batchInfo!!.batchPartsInfo!!)
                serverBatchInfo.batchPartsInfo!!.add(bpi)

        // no snapshot
        if ((serverBatchInfo.batchPartsInfo == null || serverBatchInfo.batchPartsInfo!!.size <= 0) && serverBatchInfo.rowsCount <= 0)
            return Triple(0, null, DatabaseChangesSelected())

        // If we have a snapshot we are raising the batches downloading process that will occurs
        val args1 = HttpBatchesDownloadingArgs(
            ctx,
            this.startTime!!,
            serverBatchInfo,
            this.getServiceHost()
        )
        this.intercept(args1)
        this.reportProgress(ctx, progress, args1)

        serverBatchInfo.batchPartsInfo?.forEach { bpi ->
            // Create the message enveloppe
            Log.d(TAG, "CLIENT bpi.FileName:${bpi.fileName}. bpi.Index:${bpi.index}")

            val changesToSend3 = HttpMessageGetMoreChangesRequest(ctx, bpi.index)
//            var serializer3 = this.options.SerializerFactory.GetSerializer<HttpMessageGetMoreChangesRequest>()
//            val binaryData3 = await serializer3.SerializeAsync(changesToSend3)
//            val step3 = HttpStep.GetMoreChanges

            val args2 = HttpGettingServerChangesRequestArgs(bpi.index, serverBatchInfo.batchPartsInfo!!.size, summaryResponseContent.syncContext, this.getServiceHost())
            this.intercept(args2)

            val response = service.moreChanges(authHeader, changesToSend3, converter)

            // Serialize
            serialize(response, bpi.fileName, batchDirectoryFullPath, this)

            bpi.serializedType = BatchPartInfo::class.java

            // Raise response from server containing a batch changes
            val args3 = HttpGettingServerChangesResponseArgs(
                serverBatchInfo,
                bpi.index,
                bpi.rowsCount,
                summaryResponseContent.syncContext,
                this.getServiceHost()
            )
            this.intercept(args3)
            this.reportProgress(ctx, progress, args3)
        }

        // Reaffect context
        this.setContext(summaryResponseContent.syncContext)

        val args4 = HttpBatchesDownloadedArgs(
            summaryResponseContent,
            summaryResponseContent.syncContext,
            this.startTime!!,
            utcNow(),
            this.getServiceHost()
        )
        this.intercept(args4)
        this.reportProgress(ctx, progress, args4)

        return Triple(
            summaryResponseContent.remoteClientTimestamp,
            serverBatchInfo,
            summaryResponseContent.serverChangesSelected
        )
    }

    /**
     * Apply changes
     */
    override suspend fun applyThenGetChanges(
        scope: ScopeInfo, clientBatchInfo: BatchInfo?,
        progress: Progress<ProgressArgs>?
    ): Tuple<Long, BatchInfo, ConflictResolutionPolicy, DatabaseChangesApplied?, DatabaseChangesSelected?> {
        // Get context or create a new one
        val ctx = this.getContext()

        if (this.startTime == null)
            this.startTime = utcNow()

        // is it something that could happens ?
        val schema = if (scope.schema == null) {
            // Make a remote call to get Schema from remote provider
            val serverScopeInfo = this.ensureSchema(progress)
            serverScopeInfo.schema
        } else {
            scope.schema
        }

        schema?.ensureSchema()

        ctx.syncStage = SyncStage.ChangesApplying

        // if we don't have any BatchPartsInfo, just generate a new one to get, at least, something to send to the server
        // and get a response with new data from server

        var clientBatchInfo = clientBatchInfo
        if (clientBatchInfo == null)
            clientBatchInfo = BatchInfo(true, schema!!)

        // Get sanitized schema, without readonly columns
        val sanitizedSchema = clientBatchInfo.sanitizedSchema

        // --------------------------------------------------------------
        // STEP 1 : Send everything to the server side
        // --------------------------------------------------------------

        var httpMessageContent: HttpMessageSummaryResponse? = null

        // If not in memory and BatchPartsInfo.Count == 0, nothing to send.
        // But we need to send something, so generate a little batch part
        if (clientBatchInfo.inMemory || (!clientBatchInfo.inMemory && clientBatchInfo.batchPartsInfo!!.isEmpty())) {
            val changesToSend = HttpMessageSendChangesRequest(ctx, scope)

            if (this.converter != null && clientBatchInfo.inMemoryData != null && clientBatchInfo.inMemoryData!!.hasRows)
                this.beforeSerializeRows(clientBatchInfo.inMemoryData!!)

            val containerSet =
                if (clientBatchInfo.inMemoryData == null) ContainerSet() else clientBatchInfo.inMemoryData!!.getContainerSet()
            changesToSend.changes = containerSet
            changesToSend.isLastBatch = true
            changesToSend.batchIndex = 0
            changesToSend.batchCount = when {
                clientBatchInfo.inMemoryData == null -> 0
                clientBatchInfo.batchPartsInfo == null -> 0
                else -> clientBatchInfo.batchPartsInfo!!.size
            }
            val inMemoryRowsCount = changesToSend.changes!!.rowsCount()

            ctx.progressPercentage += 0.125

            val args2 = HttpSendingClientChangesRequestArgs(
                changesToSend,
                inMemoryRowsCount,
                inMemoryRowsCount,
                this.getServiceHost()
            )
            this.intercept(args2)
            this.reportProgress(ctx, progress, args2)

            httpMessageContent = service.sendChanges(authHeader, changesToSend, this.converter)
        } else {
            var tmpRowsSendedCount = 0

            // Foreach part, will have to send them to the remote
            // once finished, return context
            val initialPctProgress1 = ctx.progressPercentage
            for (bpi in clientBatchInfo.batchPartsInfo!!.sortedBy { bpi -> bpi.index }) {
                // If BPI is InMempory, no need to deserialize from disk
                // othewise load it
                bpi.loadBatch(sanitizedSchema, clientBatchInfo.getDirectoryFullPath(), this)

                val changesToSend = HttpMessageSendChangesRequest(ctx, scope)

                if (this.converter != null && bpi.data!!.hasRows)
                    beforeSerializeRows(bpi.data!!)

                // Set the change request properties
                changesToSend.changes = bpi.data!!.getContainerSet()
                changesToSend.isLastBatch = bpi.isLastBatch
                changesToSend.batchIndex = bpi.index
                changesToSend.batchCount = clientBatchInfo.batchPartsInfo!!.size

                tmpRowsSendedCount += changesToSend.changes!!.rowsCount()

                ctx.progressPercentage =
                    initialPctProgress1 + ((changesToSend.batchIndex + 1) * 0.2 / changesToSend.batchCount)
                val args2 = HttpSendingClientChangesRequestArgs(
                    changesToSend,
                    tmpRowsSendedCount,
                    clientBatchInfo.rowsCount,
                    this.getServiceHost()
                )
                this.intercept(args2)
                this.reportProgress(ctx, progress, args2)

                // serialize message
//                var serializer = this.SerializerFactory.GetSerializer<HttpMessageSendChangesRequest>();
//                var binaryData = await serializer . SerializeAsync (changesToSend);


                httpMessageContent = service.sendChanges(authHeader, changesToSend, this.converter)
            }

        }

        // --------------------------------------------------------------
        // STEP 2 : Receive everything from the server side
        // --------------------------------------------------------------

        // Now we have sent all the datas to the server and now :
        // We have a FIRST response from the server with new datas
        // 1) Could be the only one response (enough or InMemory is set on the server side)
        // 2) Could bt the first response and we need to download all batchs

        ctx.syncStage = SyncStage.ChangesSelecting
        val initialPctProgress = 0.55
        ctx.progressPercentage = initialPctProgress


        // Get if we need to work in memory or serialize things
        val workInMemoryLocally = this.options.batchSize == 0

        // Create the BatchInfo
        val serverBatchInfo = BatchInfo(workInMemoryLocally, schema!!, this.options.batchDirectory)

        // Deserialize response incoming from server
        val summaryResponseContent: HttpMessageSummaryResponse = httpMessageContent!!

        serverBatchInfo.rowsCount = summaryResponseContent.batchInfo!!.rowsCount
        serverBatchInfo.timestamp = summaryResponseContent.remoteClientTimestamp

        if (summaryResponseContent.batchInfo?.batchPartsInfo != null)
            for (bpi in summaryResponseContent.batchInfo!!.batchPartsInfo!!)
                serverBatchInfo.batchPartsInfo?.add(bpi)

        //-----------------------
        // In Memory Mode
        //-----------------------
        // response contains the rows because we are in memory mode
        if (summaryResponseContent.changes != null && workInMemoryLocally) {
            val changesSet = SyncSet()

            for (tbl in summaryResponseContent.changes!!.tables)
                DbSyncAdapter.createChangesTable(
                    serverBatchInfo.sanitizedSchema.tables[tbl.tableName, tbl.schemaName ?: ""]!!,
                    changesSet
                )

            changesSet.importContainerSet(summaryResponseContent.changes!!, false)

            afterDeserializedRows(changesSet)

            // Create a BatchPartInfo instance
            serverBatchInfo.addChanges(changesSet, 0, true, this)

            // Raise response from server containing one finale batch changes
            val args3 = HttpGettingServerChangesResponseArgs(
                serverBatchInfo,
                0,
                serverBatchInfo.rowsCount,
                summaryResponseContent.syncContext,
                this.getServiceHost()
            )
            this.intercept(args3)
            this.reportProgress(ctx, progress, args3)

            return Tuple(
                summaryResponseContent.remoteClientTimestamp,
                serverBatchInfo,
                summaryResponseContent.conflictResolutionPolicy,
                summaryResponseContent.clientChangesApplied,
                summaryResponseContent.serverChangesSelected
            )
        }

        //-----------------------
        // In Batch Mode
        //-----------------------
        // From here, we need to serialize everything on disk

        // Generate the batch directory
        val batchDirectoryRoot = this.options.batchDirectory
        val batchDirectoryName = SimpleDateFormat("yyyy_MM_dd_ss", Locale.getDefault()).format(
            Calendar.getInstance(Locale.getDefault()).time
        ) + UUID.randomUUID().toString().replace(".", "")

        serverBatchInfo.directoryRoot = batchDirectoryRoot
        serverBatchInfo.directoryName = batchDirectoryName

        //// hook to get the last batch part info at the end
        //var bpis = serverBatchInfo.BatchPartsInfo.Where(bpi => !bpi.IsLastBatch);
        //var lstbpi = serverBatchInfo.BatchPartsInfo.FirstOrDefault(bpi => bpi.IsLastBatch);

        // If we have a snapshot we are raising the batches downloading process that will occurs
        val args1 = HttpBatchesDownloadingArgs(
            syncContext!!,
            this.startTime!!,
            serverBatchInfo,
            this.getServiceHost()
        )
        this.intercept(args1)
        this.reportProgress(ctx, progress, args1)

        val dl: (suspend (BatchPartInfo) -> Unit) = { bpi: BatchPartInfo ->
            val changesToSend3 = HttpMessageGetMoreChangesRequest(ctx, bpi.index)

            val args2 = HttpGettingServerChangesRequestArgs(
                bpi.index,
                serverBatchInfo.batchPartsInfo!!.size,
                summaryResponseContent.syncContext,
                this.getServiceHost()
            )
            this.intercept(args2)

            // Raise get changes request
            ctx.progressPercentage =
                initialPctProgress + ((bpi.index + 1) * 0.2 / serverBatchInfo.batchPartsInfo!!.size)

            val response = service.moreChanges(authHeader, changesToSend3, converter)

            // Serialize
            serialize(response, bpi.fileName, serverBatchInfo.getDirectoryFullPath(), this)

            bpi.serializedType = BatchPartInfo::class.java

            // Raise response from server containing a batch changes
            val args3 = HttpGettingServerChangesResponseArgs(
                serverBatchInfo,
                bpi.index,
                bpi.rowsCount,
                summaryResponseContent.syncContext,
                this.getServiceHost()
            )
            this.intercept(args3)
            this.reportProgress(ctx, progress, args3)
        }

        // Parrallel download of all bpis except the last one (which will launch the delete directory on the server side)
        serverBatchInfo.batchPartsInfo?.forEach { bpi -> dl(bpi) }//, this.MaxDownladingDegreeOfParallelism).ConfigureAwait(false);

        // Send order of end of download
        val lastBpi = serverBatchInfo.batchPartsInfo?.firstOrNull { bpi -> bpi.isLastBatch }

        if (lastBpi != null) {
            val endOfDownloadChanges = HttpMessageGetMoreChangesRequest(ctx, lastBpi.index)
//            val serializerEndOfDownloadChanges = this.options.SerializerFactory.GetSerializer<HttpMessageGetMoreChangesRequest>()
//            val binaryData3 = serializerEndOfDownloadChanges.SerializeAsync(endOfDownloadChanges).ConfigureAwait(false)

            service.endDownloadChanges(authHeader, endOfDownloadChanges, converter)
        }

        // generate the new scope item
        this.completeTime = utcNow()

        // Reaffect context
        this.setContext(summaryResponseContent.syncContext)

        val args4 = HttpBatchesDownloadedArgs(
            summaryResponseContent,
            summaryResponseContent.syncContext,
            this.startTime!!,
            utcNow(),
            this.getServiceHost()
        )
        this.intercept(args4)
        this.reportProgress(ctx, progress, args4)

        return Tuple(
            summaryResponseContent.remoteClientTimestamp,
            serverBatchInfo,
            summaryResponseContent.conflictResolutionPolicy,
            summaryResponseContent.clientChangesApplied,
            summaryResponseContent.serverChangesSelected
        )
    }

    private fun setContext(context: SyncContext) {
        val ctx = getContext()
        context.copyTo(ctx)
    }

    private fun afterDeserializedRows(data: SyncSet) =
        data.tables
            .filter { table -> table.rows.isNotEmpty() }
            .forEach { table -> table.rows.forEach { row -> this.converter?.afterDeserialized(row) } }

    private fun getServiceHost() =
        URL(service.serviceAddress).host

    private fun beforeSerializeRows(data: SyncSet) {
        data.tables
            .filter { table -> table.rows.isNotEmpty() }
            .forEach { table ->
                table.rows.forEach { row -> this.converter?.beforeSerialize(row) }
            }
    }

    private fun serialize(
        data: ByteArray,
        fileName: String,
        directoryFullPath: String,
        orchestrator: BaseOrchestrator
    ) {
        val dir = File(directoryFullPath)
        if (dir.isDirectory && !dir.exists())
            dir.mkdir()

        val file = File(directoryFullPath, fileName)
        file.writeBytes(data)
    }
}
