package com.mimetis.dotmim.sync.enumerations

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.data.EnumByValueSerializer
import com.mimetis.dotmim.sync.data.EnumWithValue

/**
 * Sync direction : Can be Bidirectional (default), DownloadOnly, UploadOnly
 */
@Serializable(with = SyncDirectionSeralizer::class)
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

class SyncDirectionSeralizer : EnumByValueSerializer<SyncDirection>()
