package com.mimetis.dotmim.sync.messages

import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.enumerations.ConflictResolutionPolicy
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.util.*

/**
 * Message exchanged during the Begin session sync stage
 */
class MessageApplyChanges(
        /**
         * Gets the local Scope Id
         */
        val localScopeId: UUID,

        /**
         * Gets the sender Scope Id
         */
        val senderScopeId: UUID?,

        /**
         * Gets or Sets if the sync is a first sync. In this case, the last sync timestamp is ignored
         */
        val isNew: Boolean,

        /**
         * Gets or Sets the last date timestamp from where we want rows
         */
        val lastTimestamp: Long?,

        /**
         * Gets or Sets the schema used for this sync
         */
        val schema: SyncSet,

        /**
         * Gets or Sets the setup used for this sync
         */
        val setup: SyncSetup,

        /**
         * Gets or Sets the current Conflict resolution policy
         */
        val policy: ConflictResolutionPolicy,

        /**
         * Gets or Sets if we should disable all constraints on apply changes.
         */
        val disableConstraintsOnApplyChanges: Boolean,

        /**
         * Gets or Sets if during appy changes, we are using bulk operations
         */
        val useBulkOperations: Boolean,

        /**
         * Gets or Sets if we should cleaning tracking table metadatas
         */
        val cleanMetadatas: Boolean,

        /**
         * Gets or Sets if we should cleaning tmp dir files after sync.
         */
        val cleanFolder: Boolean,

        /**
         * Gets or Sets if we have already applied a snapshot. So far, we don't reset the tables, even if we are in reinit mode.
         */
        val snapshoteApplied: Boolean,

        /**
         * Gets or Sets the changes to apply
         */
        val changes: BatchInfo
)
