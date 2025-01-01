package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.ContainerSet
import java.io.File
import java.io.FileOutputStream

/**
 * Raise just after loading a binary change set from disk, just before calling the deserializer
 */
class DeserializingSetArgs(
    context: SyncContext,

    /**
     * Gets the Filestream to deserialize
     */
    val fileStream: FileOutputStream,

    /**
     * File name containing the set to be deserialized
     */
    val fileName: String,

    /**
     * Directory containing the file, about to be deserialized
     */
    val directoryPath: String
) : ProgressArgs(context) {
    override val message: String =
        "[$fileName] Deserializing Set."

    override val eventId: Int =
        8050

    override val source: String =
        if (directoryPath.isBlank()) "" else File(directoryPath).name

    /**
     * Gets or Sets the container set result, after having deserialized the FileStream. If the Result property is Null, Dotmim.Sync will deserialized the stream using a simple Json converter
     */
    var result: ContainerSet? = null
}

/**
 * Occurs just after loading a serialized set from disk
 */
fun BaseOrchestrator.onDeserializingSet(action: (DeserializingSetArgs) -> Unit) =
    this.setInterceptor(action)
