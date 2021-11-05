package com.mimetis.dotmim.sync.builders

import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.setup.DbType

abstract class DbMetadata {
    /**
     * Validate if a column definition is actualy supported by the provider
     */
    abstract fun isValid(columnDefinition: SyncColumn): Boolean

    /**
     * Gets and validate a max length issued from the database definition
     */
    abstract fun validateMaxLength(typeName: String, isUnsigned: Boolean, isUnicode: Boolean, maxLength: Int): Int

    /**
     * Get a datastore DbType from a datastore type name
     */
    abstract fun validateOwnerDbType(typeName: String, isUnsigned: Boolean, isUnicode: Boolean, maxLength: Int): Any

    /**
     * Get a managed type from a datastore dbType
     */
    abstract fun validateType(ownerType: Any): Class<*>

    /**
     * Get a DbType from a datastore type name
     */
    abstract fun validateDbType(typeName: String, isUnsigned: Boolean, isUnicode: Boolean, maxLength: Int): DbType

    /**
     * Validate if a column is readonly or not
     */
    abstract fun validateIsReadonly(columnDefinition: SyncColumn): Boolean

    /**
     * Check if a type name is a numeric type
     */
    abstract fun isNumericType(typeName: String): Boolean

    /**
     * Check if a type name is a text type
     */
    abstract fun isTextType(typeName: String): Boolean

    /**
     * Check if a type name support scale
     */
    abstract fun supportScale(typeName: String): Boolean

    /**
     * Get precision and scale from a SchemaColumn
     */
    abstract fun validatePrecisionAndScale(columnDefinition: SyncColumn): Pair<Byte, Byte>

    /**
     * Get precision if supported (MySql supports int(10))
     */
    abstract fun validatePrecision(columnDefinition: SyncColumn): Byte
}
