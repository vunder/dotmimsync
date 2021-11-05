package com.mimetis.dotmim.sync.sqlite

import com.mimetis.dotmim.sync.builders.DbMetadata
import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.setup.DbType
import java.util.*

class SqliteDbMetadata : DbMetadata() {
    override fun isValid(columnDefinition: SyncColumn): Boolean {
        var typeName = columnDefinition.originalTypeName.lowercase(Locale.getDefault())

        if (typeName.contains("("))
            typeName = typeName.substring(0, typeName.indexOf("("))

        return when (typeName) {
            "integer",
            "float",
            "decimal",
            "bit",
            "bigint",
            "numeric",
            "blob",
            "image",
            "datetime",
            "time",
            "text",
            "varchar",
            "real" ->
                true
            else ->
                false
        }
    }

    override fun validateMaxLength(typeName: String, isUnsigned: Boolean, isUnicode: Boolean, maxLength: Int): Int =
            if (maxLength > Int.MAX_VALUE)
                Int.MAX_VALUE
            else
                maxLength

    override fun validateOwnerDbType(typeName: String, isUnsigned: Boolean, isUnicode: Boolean, maxLength: Int): Any {
        var tn = typeName
        if (tn.contains("("))
            tn = tn.substring(0, typeName.indexOf("("))

        when (tn.lowercase(Locale.getDefault())) {
            "bit",
            "integer",
            "bigint" ->
                return SqliteType.Integer
            "numeric",
            "decimal",
            "real",
            "float" ->
                return SqliteType.Real
            "blob",
            "image" ->
                return SqliteType.Blob
            "datetime",
            "time",
            "varchar",
            "text" ->
                return SqliteType.Text

        }
        throw Exception("this type name $tn is not supported")
    }

    override fun validateType(ownerType: Any): Class<*> {
        return when (ownerType as SqliteType) {
            SqliteType.Integer ->
                Long::class.java
            SqliteType.Real ->
                Double::class.java
            SqliteType.Text ->
                String::class.java
            SqliteType.Blob ->
                Any::class.java
            else ->
                throw Exception("this DbType $ownerType is not supported")
        }
    }

    override fun validateDbType(typeName: String, isUnsigned: Boolean, isUnicode: Boolean, maxLength: Int): DbType {
        var tn = typeName
        if (tn.contains("("))
            tn = tn.substring(0, typeName.indexOf("("))

        when (tn.lowercase(Locale.getDefault())) {
            "bit" ->
                return DbType.Boolean
            "integer",
            "bigint" ->
                return DbType.Int64
            "numeric",
            "real",
            "float" ->
                return DbType.Double
            "decimal" ->
                return DbType.Decimal
            "blob",
            "image" ->
                return DbType.Binary
            "datetime" ->
                return DbType.DateTime
            "time" ->
                return DbType.Time
            "text",
            "varchar" ->
                return DbType.String
        }
        throw Exception("this type name $tn is not supported")
    }

    override fun validateIsReadonly(columnDefinition: SyncColumn): Boolean =
            false

    override fun isNumericType(typeName: String): Boolean {
        var tn = typeName.lowercase(Locale.getDefault())

        if (tn.contains("("))
            tn = typeName.substring(0, tn.indexOf("("))

        return tn == "numeric" || tn == "decimal" || tn == "real"
                || tn == "integer" || tn == "bigint"
    }

    override fun isTextType(typeName: String): Boolean {
        val tn = typeName.lowercase(Locale.getDefault())
        return tn == "text"
    }

    override fun supportScale(typeName: String): Boolean {
        val tn = typeName.lowercase(Locale.getDefault())
        return tn == "numeric" || tn == "decimal" || tn == "real" || tn == "flot"
    }

    override fun validatePrecisionAndScale(columnDefinition: SyncColumn): Pair<Byte, Byte> =
            Pair(columnDefinition.precision, columnDefinition.scale)

    override fun validatePrecision(columnDefinition: SyncColumn): Byte =
            columnDefinition.precision
}
