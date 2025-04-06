package com.mimetis.dotmim.sync.enumerations

/**
 * Defines the types of conflicts that can occur during synchronization.
 */
enum class ConflictType {
    /**
     * The peer database threw an exception while applying a change.
     */
    ErrorsOccurred,

    /**
     * The remote datasource raised an unique key constraint error
     */
    UniqueKeyConstraint,

    // Classic conflicts on update / update or deleted / deleted.

    /**
     * The Remote and Local datasources have both updated the same row.
     */
    RemoteExistsLocalExists,

    /**
     * The Remote and Local datasource have both deleted the same row.
     */
    RemoteIsDeletedLocalIsDeleted,

    // Updated or Inserted on one side and Not Exists on the other

    /**
     * The Remote datasource has updated or inserted a row that does not exists in the local datasource.
     */
    RemoteExistsLocalNotExists,

    /**
     * The Local datasource has inserted or updated a row that does not exists in the Remote datasource
     */
    RemoteNotExistsLocalExists,

    // Deleted on one side and Updated or Inserted on the other

    /**
     * The Remote datasource has inserted or updated a row that the Local datasource has deleted.
     */
    RemoteExistsLocalIsDeleted,

    /**
     * The Remote datasource has deleted a row that the Local datasource has inserted or updated.
     */
    RemoteIsDeletedLocalExists,

    // Deleted on one side and Not Exists on the other

    /**
     * The Local datasource has deleted a row that does not exists in the Remote datasource
     * Note : this Case can't happen
     * From the server point of view : Remote Not Exists means client has not the row. SO it will just not send anything to the server
     * From the client point of view : Remote Not Exists means server has not the row. SO it will just not send back anything to client
     */
//    RemoteNotExistsLocalIsDeleted,

    /**
     * The Remote datasource has deleted a row that does not exists in the Local datasource
     */
    RemoteIsDeletedLocalNotExists
}
