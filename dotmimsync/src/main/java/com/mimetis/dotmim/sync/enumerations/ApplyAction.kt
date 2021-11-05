package com.mimetis.dotmim.sync.enumerations

/**
 * Specifies the options for processing a row when the row cannot be applied during synchronization.
 */
enum class ApplyAction {
    /**
     * Continue processing (ie server wins)
     * This is the default behavior.
     */
    Continue,

    /**
     * Force the row to be applied by using logic that is included in synchronization adapter commands.
     */
    RetryWithForceWrite,

    /**
     * Force to rollback all the sync processus
     */
    Rollback
}
