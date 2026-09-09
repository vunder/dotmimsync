package com.mimetis.dotmim.sync.enumerations

import kotlinx.serialization.Serializable

@Serializable
enum class SyncType {
    /// <summary>
    /// Normal synchronization
    /// </summary>
    Normal,

    /// <summary>
    /// Reinitialize the whole sync database, applying all rows from the server to the client
    /// </summary>
    Reinitialize,

    /// <summary>
    /// Reinitialize the whole sync database, applying all rows from the server to the client, after trying a client upload
    /// </summary>
    ReinitializeWithUpload
}
