package com.mimetis.dotmim.sync

/**
 * Occurs when we local orchestrator tries to update untracked rows, but no tracking table exists
 */
class MissingTrackingTableException(
    tableName: String
) : Exception("No tracking table for table $tableName. Please Provision your database before calling this method")
