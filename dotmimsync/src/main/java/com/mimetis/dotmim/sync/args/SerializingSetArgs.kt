package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.ContainerSet
import java.io.File

/**
 * Raise before serialize a change set to get a byte array
 */
class SerializingSetArgs(
    context: SyncContext,

    /**
     * Container set to serialize
     */
    val set: ContainerSet,

    /**
     * File name, where the content will be serialized
     */
    val fileName: String,

    /**
     * Directory containing the file, about to be serialized
     */
    val directoryPath: String
) : ProgressArgs(context) {
    /**
     * Gets or Sets byte array representing the Set to serialize to the disk. If the Result property is Null, Dotmim.Sync will serialized the container set using the serializer factory configured in the SyncOptions instance
     */
    var result: ByteArray? = null

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String = "[$fileName] Serializing Set."

    override val eventId: Int = 8000

    override val source: String =
        if (directoryPath.isBlank()) "" else File(directoryPath).name
}

/**
 * Occurs just before saving a serialized set to disk
 */
fun BaseOrchestrator.onSerializingSet(action: (SerializingSetArgs) -> Unit) =
    this.setInterceptor(action)
