package com.mimetis.dotmim.sync.batch

import com.mimetis.dotmim.sync.MissingFileException
import com.mimetis.dotmim.sync.args.DeserializingSetArgs
import com.mimetis.dotmim.sync.args.SerializingSetArgs
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.ContainerSet
import com.mimetis.dotmim.sync.set.SyncSet
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readText
import kotlinx.io.buffered
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.write
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * Info about a BatchPart
 * Will be serialized in the BatchInfo file
 */
@Serializable
class BatchPartInfo {
    @SerialName("file")
    var fileName: String = ""

    @SerialName("index")
    var index: Int = 0

    @SerialName("last")
    var isLastBatch: Boolean = false

    /**
     * Tables contained in the SyncSet (serialiazed or not)
     */
    @SerialName("tables")
    var tables: List<BatchPartTableInfo>? = null

    /**
     * Tables contained rows count
     */
    @SerialName("rc")
    var rowsCount: Int = 0

    /**
     * Get a SyncSet corresponding to this batch part info
     */
    @Transient
    var data: SyncSet? = null

    /**
     * Gets or Sets the serialized type
     */
    @Transient
    var serializedType: KClass<*>? = null

    /**
     * Loads the batch file and import the rows in a SyncSet instance
     */
    fun loadBatch(
        sanitizedSchema: SyncSet,
        directoryFullPath: String,
        orchestrator: BaseOrchestrator? = null
    ) {
        if (this.data == null)
            return

        if (this.fileName.isBlank())
            return

        // Clone the schema to get a unique instance
        val set = sanitizedSchema.clone()

        // Get a Batch part, and deserialise the file into a the BatchPartInfo Set property
        val data = deserialize(this.fileName, directoryFullPath, orchestrator)

        // Import data in a typed Set
        set.importContainerSet(data, true)

        this.data = set
    }

    /**
     * Delete the SyncSet affiliated with the BatchPart, if exists.
     */
    fun clear() {
//        this.data?.dispose()
        this.data = null
    }

    private fun deserialize(
        fileName: String,
        directoryFullPath: String,
        orchestrator: BaseOrchestrator? = null
    ): ContainerSet {
        if (fileName.isBlank())
            throw Exception("ArgumentNullException: fileName")

        val file = Path(directoryFullPath, fileName)
        //val fullPath = Path.Combine(directoryFullPath, fileName)

        if (!SystemFileSystem.exists(file))
            throw MissingFileException(file.toString())

        //var jsonConverter = new Utf8JsonConverter<ContainerSet>()

        // backward compatibility
        if (serializedType == null)
            serializedType = ContainerSet::class

        var set: ContainerSet? = null

        if (orchestrator != null) {
            val interceptorArgs = DeserializingSetArgs(
                orchestrator.getContext(),
                SystemFileSystem.sink(file),
                fileName,
                directoryFullPath
            )
            orchestrator.intercept(interceptorArgs)
            set = interceptorArgs.result
        }

        if (set == null) {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            val jsonText = SystemFileSystem.source(file).use { it.buffered().readText() }
            set = if (serializedType == ContainerSet::class) {
                json.decodeFromString(jsonText)
            } else {
                val jsonString: ContainerSetBoilerPlate = json.decodeFromString(jsonText)
                jsonString.changes
            }
        }

        return set!!
    }

    companion object {
        /**
         * Serialize a container set instance
         */
        private fun serialize(
            set: ContainerSet?,
            fileName: String,
            directoryFullPath: String,
            orchestrator: BaseOrchestrator? = null
        ) {
            if (set == null)
                return

            val dir = Path(directoryFullPath)

            if (!SystemFileSystem.exists(dir))
                SystemFileSystem.createDirectories(dir)

            val file = Path(directoryFullPath, fileName)

            var serializedBytes: ByteArray? = null

            if (orchestrator != null) {
                val interceptorArgs =
                    SerializingSetArgs(orchestrator.getContext(), set, fileName, directoryFullPath)
                orchestrator.intercept(interceptorArgs)
                serializedBytes = interceptorArgs.result
            }

            if (serializedBytes == null) {
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                val jsonString = json.encodeToString(set)
                serializedBytes = jsonString.toByteArray()
            }

            SystemFileSystem.sink(file).use {
                it.buffered().write(ByteString(serializedBytes))
            }
        }

        /**
         * Create a new BPI, and serialize the changeset if not in memory
         */
        internal fun createBatchPartInfo(
            batchIndex: Int,
            set: SyncSet?,
            fileName: String,
            directoryFullPath: String,
            isLastBatch: Boolean,
            orchestrator: BaseOrchestrator? = null
        ): BatchPartInfo {
            val bpi: BatchPartInfo

            // Create a batch part
            // The batch part creation process will serialize the changesSet to the disk

            // Serialize the file !
            serialize(set?.getContainerSet(), fileName, directoryFullPath, orchestrator)

            bpi = BatchPartInfo().apply { this.fileName = fileName }

            bpi.index = batchIndex
            bpi.isLastBatch = isLastBatch

            // Even if the set is empty (serialized on disk), we should retain the tables names
            if (set != null) {
                bpi.tables = set.tables.map { t ->
                    BatchPartTableInfo(
                        t.tableName,
                        t.schemaName,
                        t.rows.size
                    )
                }.toList()
                bpi.rowsCount = set.tables.sumOf { t -> t.rows.size }
            }

            return bpi
        }
    }

    /**
     * Boiler plate for backward compatibility with HttpMessageSendChangesResponse
     */
    @Serializable
    class ContainerSetBoilerPlate {
        /**
         * Gets the BatchParInfo send from the server
         */
        @SerialName("changes")
        var changes: ContainerSet? =
            null // BE CAREFUL : Order is coming from "HttpMessageSendChangesResponse"
    }
}
