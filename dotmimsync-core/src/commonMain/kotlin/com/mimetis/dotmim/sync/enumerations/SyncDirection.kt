package com.mimetis.dotmim.sync.enumerations

import com.mimetis.dotmim.sync.data.EnumWithValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Sync direction : Can be Bidirectional (default), DownloadOnly, UploadOnly
 */
@Serializable(with = SyncDirectionSerializer::class)
enum class SyncDirection(override val value: Int) : EnumWithValue {
    /**
     * Table will be sync from server to client and from client to server
     */
    Bidirectional(0),

    /**
     * Table will be sync from server to client only.
     * All changes occured client won't be uploaded to server
     */
    DownloadOnly(2),

    /**
     * Table will be sync from client to server only
     * All changes from server won't be downloaded to client
     */
    UploadOnly(4),

    /**
     * Table structure is replicated, but not the datas
     * Note : The value should be 0, but for compatibility issue with previous version, we go for a new value
     */
    None(8)
}

class SyncDirectionSerializer : KSerializer<SyncDirection> {
    private var values: Map<Int, SyncDirection>? = null

    override val descriptor = PrimitiveSerialDescriptor("com.mimetis.dotmim.sync.EnumByValue.SyncDirection", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: SyncDirection) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): SyncDirection =
        (values
            ?: SyncDirection.entries.associateBy { it.value }.also { values = it })[decoder.decodeInt()]!!
}
