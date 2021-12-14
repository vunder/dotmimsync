package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel

open class ProgressArgs(
    /**
     * Gets the current context
     */
    val context: SyncContext
) {
    /**
     * Gets the Progress level
     */
    open val progressLevel: SyncProgressLevel = SyncProgressLevel.Information

    /**
     * Gets or Sets an arbitrary args you can use for you own purpose
     */
    open val hint: String = ""

    /**
     * Gets the args type
     */
    val typeName: String
        get() = this::class.java.name

    /**
     * return a global message about current progress
     */
    open val message: String
        get() = ""

    /**
     * return the progress initiator source
     */
    open val source: String
        get() = ""

    /**
     * Gets the event id, used for logging purpose
     */
    open val eventId: Int
        get() = 1

    /**
     * Gets the overall percentage progress
     */
    val progressPercentage: Double
        get() = this.context.progressPercentage

    override fun toString(): String =
        if (message.isNotBlank()) message else super.toString()
}
