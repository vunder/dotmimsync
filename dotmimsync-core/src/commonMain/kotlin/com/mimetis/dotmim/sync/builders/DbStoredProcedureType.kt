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
    BulkTableType,
    BulkUpdateRows,
    BulkDeleteRows,
    Reset
}
