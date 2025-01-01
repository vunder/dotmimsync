package com.mimetis.dotmim.sync.batch

import com.mimetis.dotmim.sync.DbSyncAdapter
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.set.SyncTable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a Batch, containing a full or serialized change set
 */
@Serializable
class BatchInfo() {
    /**
     * Is the batch parts are in memory
     * If true, only one BPI
     * If false, several serialized BPI
     */
    @Transient
    var inMemory: Boolean = false

    /**
     * If in memory, return the in memory Dm
     */
    @Transient
    var inMemoryData: SyncSet? = null

    /**
     * Gets or Sets directory name
     */
    @SerialName("dirname")
    var directoryName: String = ""

    /**
     * Gets or sets directory root
     */
    @SerialName("dir")
    var directoryRoot: String = ""

    /**
     * Gets or sets server timestamp
     */
    @SerialName("ts")
    var timestamp: Long = 0

    /**
     * List of batch parts if not in memory
     */
   @SerialName("parts")
    var batchPartsInfo: ArrayList<BatchPartInfo>? = null

    /**
     * Gets or Sets the rows count contained in the batch info
     */
    @SerialName("count")
    var rowsCount: Int = 0

    /**
     * Gets or Sets the Serialization Factory Key used to serialize this batch info (if not in memory)
     */
    @SerialName("ser")
    var serializerFactoryKey: String? = null

    /**
     * Internally setting schema
     */
    @SerialName("schema")
    var sanitizedSchema: SyncSet = SyncSet()

    /**
     * Create a new BatchInfo, containing all BatchPartInfo
     */
    constructor(inMemory: Boolean, inSchema: SyncSet, rootDirectory: String = "", directoryName: String = "") : this() {
        this.inMemory = inMemory

        // We need to create a change table set, containing table with columns not readonly
        for (table in inSchema.tables)
            DbSyncAdapter.createChangesTable(inSchema.tables[table.tableName, table.schemaName]!!, this.sanitizedSchema)

        // If not in memory, generate a directory name and initialize batch parts list
        if (!this.inMemory) {
            this.directoryRoot = rootDirectory
            this.batchPartsInfo = ArrayList()
            this.directoryName = if (directoryName.isNotBlank())
                SimpleDateFormat("yyyy_MM_dd_ss", Locale.getDefault()).format(Calendar.getInstance(Locale.getDefault()).time)
            else
                directoryName
        }
    }

    /**
     * Add changes to batch info.
     */
    fun addChanges(changes: SyncSet, batchIndex: Int = 0, isLastBatch: Boolean = true, orchestrator: BaseOrchestrator? = null) {
        if (this.inMemory) {
            this.serializerFactoryKey = null
            this.inMemoryData = changes
        } else {
//            this.serializerFactoryKey = serializerFactoryKey.key
            val bpId = generateNewFileName(batchIndex.toString())
            //var fileName = Path.Combine(this.GetDirectoryFullPath(), bpId)
            val bpi = BatchPartInfo.createBatchPartInfo(batchIndex, changes, bpId, getDirectoryFullPath(), isLastBatch, orchestrator)

            // add the batchpartinfo tp the current batchinfo
            this.batchPartsInfo?.add(bpi)
        }
    }

    /**
     * Get the full path of the Batch directory
     */
    fun getDirectoryFullPath(): String =
            if (inMemory) "" else File(directoryRoot, directoryName).absolutePath

    /**
     * Ensure the last batch part (if not in memory) has the correct IsLastBatch flag
     */
    fun ensureLastBatch() {
        if (this.inMemory)
            return

        if (this.batchPartsInfo!!.isEmpty())
            return

        // get last index
        val maxIndex = this.batchPartsInfo!!.maxOf { tBpi -> tBpi.index }

        // Set corret last batch
        this.batchPartsInfo!!.forEach { bpi -> bpi.isLastBatch = bpi.index == maxIndex }
    }

    /**
     * Check if this batchinfo has some data (in memory or not)
     */
    fun hasData(): Boolean {
        if (this.sanitizedSchema == null)
            throw NullPointerException("Batch info schema should not be null")

        if (inMemory && inMemoryData != null && inMemoryData!!.hasTables && inMemoryData!!.hasRows)
            return true

        if (!inMemory && batchPartsInfo != null && batchPartsInfo!!.isNotEmpty()) {
            val rowsCount = batchPartsInfo!!.sumOf { bpi -> bpi.rowsCount }

            return rowsCount > 0
        }

        return false
    }

    /**
     * Check if this batchinfo has some data (in memory or not)
     */
    fun hasData(tableName: String, schemaName: String): Boolean {
        if (this.sanitizedSchema == null)
            throw NullPointerException("Batch info schema should not be null")

        if (inMemory && inMemoryData != null && inMemoryData!!.hasTables) {
            val table = inMemoryData!!.tables[tableName, schemaName]
            if (table == null)
                return false

            return table.hasRows
        }

        if (!inMemory && batchPartsInfo != null && batchPartsInfo!!.isNotEmpty()) {
            val tableInfo = BatchPartTableInfo(tableName, schemaName)

            val bptis = batchPartsInfo?.map { bpi ->
                bpi.tables?.filter { t -> t.equalsByName(tableInfo) } ?: emptyList()
            }?.flatten()

            if (bptis == null)
                return false


            return bptis.sumOf { bpti -> bpti.rowsCount } > 0
        }

        return false
    }

    /**
     * Clear all batch parts info and try to delete tmp folder if needed
     */
    fun clear(deleteFolder: Boolean) {
        if (this.inMemory && this.inMemoryData != null) {
            this.inMemoryData?.clear()
            return
        }

        // Delete folders before deleting batch parts
        if (deleteFolder)
            this.tryRemoveDirectory()

        if (this.batchPartsInfo != null) {
            this.batchPartsInfo?.forEach { it.clear() }

            this.batchPartsInfo?.clear()
        }
    }

    /**
     * try to delete the Batch tmp directory and all the files stored in it
     */
    fun tryRemoveDirectory() {
        // Once we have applied all the batch, we can safely remove the temp dir and all it's files
        if (!this.inMemory && this.directoryRoot.isNotBlank() && this.directoryName.isBlank()) {
            val tmpDirectory = File(this.getDirectoryFullPath())

            if (!tmpDirectory.exists())
                return

            try {
                tmpDirectory.deleteRecursively()
            }
            // do nothing here
            catch (e: Exception) {
            }
        }
    }

    fun getTable(tableName: String, schemaName: String, orchestrator: BaseOrchestrator? = null): List<SyncTable?> {
        if (this.sanitizedSchema == null)
            throw NullPointerException("Batch info schema should not be null")

        val tableInfo = BatchPartTableInfo(tableName, schemaName)

        if (inMemory) {
            this.serializerFactoryKey = null
            if (this.inMemoryData != null && this.inMemoryData!!.hasTables)
                return listOf(this.inMemoryData!!.tables[tableName, schemaName]!!)
        } else {
            val bpiTables = batchPartsInfo
                    ?.filter { bpi -> bpi.rowsCount > 0 && bpi.tables!!.any { t -> t.equalsByName(tableInfo) } }
                    ?.sortedBy { t -> t.index }

            if (bpiTables != null) {
                return bpiTables.mapNotNull { batchPartinInfo ->
                    // load only if not already loaded in memory
                    if (batchPartinInfo.data == null)
                        batchPartinInfo.loadBatch(this.sanitizedSchema, getDirectoryFullPath(), orchestrator)

                    // Get the table from the batchPartInfo
                    // generate a tmp SyncTable for
                    val batchTable = batchPartinInfo.data?.tables?.firstOrNull { bt -> bt.equalsByName(SyncTable(tableName, schemaName)) }

                    if (batchTable != null) {

                        // We may need this same BatchPartInfo for another table,
                        // but we dispose it anyway, because memory can be quickly a bottleneck
                        // if batchpartinfos are resident in memory
                        batchPartinInfo.data?.clear()
                        batchPartinInfo.data = null

                    }

                    return@mapNotNull batchTable
                }
            }
        }
        return emptyList()
    }


    @OptIn(ExperimentalUuidApi::class)
    companion object {
        /**
         * generate a batch file name
         */
        fun generateNewFileName(batchIndex: String): String {
            val index = when (batchIndex.length) {
                1 -> "000$batchIndex"
                2 -> "00${batchIndex}"
                3 -> "0${batchIndex}"
                //4 -> index = index
                else -> throw Exception("OverflowException: too much batches !!!")
            }

            return "${index}_${Uuid.random().toString().replace(".", "_")}.batch"
        }
    }
}
