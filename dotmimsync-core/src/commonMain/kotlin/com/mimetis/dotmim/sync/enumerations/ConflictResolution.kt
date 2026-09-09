package com.mimetis.dotmim.sync.enumerations

enum class ConflictResolution {
    /**
     * Indicates that the change on the server is the conflict winner
     */
    ServerWins,

    /**
     * Indicates that the change sent by the client is the conflict winner
     */
    ClientWins,

    /**
     * Indicates that you will manage the conflict by filling the final row and sent it to both client and server
     */
    MergeRow,

    /**
     * Indicates that you want to rollback the whole sync process
     */
    Rollback
}
