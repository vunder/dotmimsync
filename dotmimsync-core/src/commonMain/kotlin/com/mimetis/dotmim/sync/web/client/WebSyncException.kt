package com.mimetis.dotmim.sync.web.client

import com.mimetis.dotmim.sync.enumerations.SyncStage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WebSyncException(
    /**
     * Gets or Sets type name of inner exception.
     */
    @SerialName("tn")
    val typeName: String,

    /**
     * Gets or Sets the MEssage associated to the exception.
     */
    @SerialName("m")
    val message: String,

    /**
     * Gets or sets sync stage when exception occured.
     */
    @SerialName("ss")
    val syncStage: SyncStage,

    /**
     * Gets or sets data source error number if available.
     */
    @SerialName("n")
    val number: Int = 0,

    /**
     * Gets or Sets data source if available.
     */
    @SerialName("d")
    val dataSource: String = "",

    /**
     * Gets or Sets initial catalog if available.
     */
    @SerialName("ic")
    val initialCatalog: String = ""
)