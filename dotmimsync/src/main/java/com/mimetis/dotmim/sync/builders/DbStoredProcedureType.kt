package com.mimetis.dotmim.sync.builders

enum class DbStoredProcedureType {
    SelectChanges,
    SelectChangesWithFilters,
    SelectInitializedChanges,
    SelectInitializedChangesWithFilters,
    SelectRow,
    UpdateRow,
    DeleteRow,
    DeleteMetadata,
    BulkInitRows,
    BulkUpdateRows,
    BulkDeleteRows,
    Reset,
    BulkTableType
}
