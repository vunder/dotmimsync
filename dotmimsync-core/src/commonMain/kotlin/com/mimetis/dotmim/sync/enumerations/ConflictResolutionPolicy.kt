package com.mimetis.dotmim.sync.enumerations

import kotlinx.serialization.Serializable

/**
 * Represents the options for the conflict resolution policy to use for synchronization.
 * Used in the configuration class
 */
@Serializable
enum class ConflictResolutionPolicy {
    /**
     * Indicates that the change on the server wins in case of a conflict.
     */
    ServerWins,

    /**
     * Indicates that the change sent by the client wins in case of a conflict.
     */
    ClientWins
}
