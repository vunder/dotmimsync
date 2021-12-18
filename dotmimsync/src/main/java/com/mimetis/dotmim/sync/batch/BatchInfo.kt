package com.mimetis.dotmim.sync.batch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.DbSyncAdapter
import com.mimetis.dotmim.sync.set.SyncSet
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Represents a Batch, containing a full or serialized change set
 */
@Serializable
class BatchInfo() {
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
     * List of batch parts
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
    constructor(inSchema: SyncSet, rootDirectory: String = "", directoryName: String = "") : this() {
        // We need to create a change table set, containing table with columns not readonly
        for (table in inSchema.tables)
            DbSyncAdapter.createChangesTable(inSchema.tables[table.tableName, table.schemaName]!!, this.sanitizedSchema)

            this.directoryRoot = rootDirectory
            this.batchPartsInfo = ArrayList()
            this.directoryName = if (directoryName.isNotBlank())
                SimpleDateFormat("yyyy_MM_dd_ss", Locale.getDefault()).format(Calendar.getInstance(Locale.getDefault()).time) +
                        UUID.randomUUID()
            else
                directoryName
    }

    /**
     * Get the full path of the Batch directory
     */
    fun getDirectoryFullPath(): String = File(directoryRoot, directoryName).absolutePath

    /**
     * Check if this batchinfo has some data
     */
    fun hasData(): Boolean {
        if (this.sanitizedSchema == null)
            throw NullPointerException("Batch info schema should not be null")

        if (batchPartsInfo != null && batchPartsInfo!!.isNotEmpty()) {
            val rowsCount = batchPartsInfo!!.sumOf { bpi -> bpi.rowsCount }

            return rowsCount > 0
        }

        return false
    }

    /**
     * Check if this batchinfo has some data
     */
    fun hasData(tableName: String, schemaName: String): Boolean {
        if (this.sanitizedSchema == null)
            throw NullPointerException("Batch info schema should not be null")

        if (batchPartsInfo?.isNotEmpty() == true) {
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
     * Ensure the last batch part has the correct IsLastBatch flag
     */
    fun ensureLastBatch() {
        if (this.batchPartsInfo!!.isEmpty())
            return

        // get last index
        val maxIndex = this.batchPartsInfo!!.maxOf { tBpi -> tBpi.index }

        // Set corret last batch
        this.batchPartsInfo!!.forEach { bpi -> bpi.isLastBatch = bpi.index == maxIndex }
    }

    /**
     * Clear all batch parts info and try to delete tmp folder if needed
     */
    fun clear(deleteFolder: Boolean) {
        // Delete folders before deleting batch parts
        if (deleteFolder)
            this.tryRemoveDirectory()
    }

    /**
     * try to delete the Batch tmp directory and all the files stored in it
     */
    fun tryRemoveDirectory() {
        // Once we have applied all the batch, we can safely remove the temp dir and all it's files
        if (this.directoryRoot.isNotBlank() && this.directoryName.isBlank()) {
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

            return "${index}_${UUID.randomUUID().toString().replace(".", "_")}.batch"
        }
    }
}
