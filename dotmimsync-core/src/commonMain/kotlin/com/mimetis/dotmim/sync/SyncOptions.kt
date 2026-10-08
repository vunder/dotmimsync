package com.mimetis.dotmim.sync

import com.mimetis.dotmim.sync.enumerations.ConflictResolutionPolicy

class SyncOptions(
        /**
         * Gets or Sets the directory used for batch mode.
         * Default value is [User Temp Path]/[DotmimSync]
         */
        var batchDirectory: String =  "DotmimSync",

        /**
         * Gets or Sets the directory where snapshots are stored.
         * This value could be overwritten by server is used in an http mode
         */
        var snapshotsDirectory: String = "",

        /**
         * Gets or Sets the size used (approximatively in kb, depending on the serializer) for each batch file, in batch mode.
         * Default is 0 (no batch mode)
         */
        var batchSize: Int = 0,

        /**
         * Gets or Sets the log level for sync operations. Default value is false.
         */
        var useVerboseErrors: Boolean = false,

        /**
         * Gets or Sets if we should use the bulk operations. Default is true.
         * If provider does not support bulk operations, this option is overrided to false.
         */
        var useBulkOperations: Boolean = true,

        /**
         * Gets or Sets if we should clean tracking table metadatas.
         */
        var cleanMetadatas: Boolean = true,

        /**
         * Gets or Sets if we should cleaning tmp dir files after sync.
         */
        var cleanFolder: Boolean = true,

        /**
         * Gets or Sets if we should disable constraints before making apply changes
         * Default value is false
         * trying false by default : https://github.com/Mimetis/Dotmim.Sync/discussions/453#discussioncomment-380530
         */
        var disableConstraintsOnApplyChanges: Boolean = false,

        /**
         * Gets or Sets the scope_info table name. Default is scope_info
         * On the server side, server scope table is prefixed with _server and history table with _history
         */
        var scopeInfoTableName: String = DefaultScopeInfoTableName,

        /**
         * Gets or Sets the default conflict resolution policy. This value could potentially be ovewritten and replaced by the server
         */
        var conflictResolutionPolicy: ConflictResolutionPolicy = ConflictResolutionPolicy.ServerWins
) {
    companion object {
        /**
         * Default name if nothing is specified for the scope inf table, stored on the client db
         * On the server side, server scope table is prefixed with _server and history table with _history
         */
        const val DefaultScopeInfoTableName = "scope_info"

        /**
         * Default scope name if not specified
         */
        const val DefaultScopeName = "DefaultScope"
    }
}
