package com.mimetis.dotmim.sync.sqlite

import android.database.Cursor
import android.database.Cursor.*
import androidx.core.database.getBlobOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.mimetis.dotmim.sync.setup.DbType

object CursorHelper {
    fun Cursor.getValue(columnIndex: Int): Any? =
            when (this.getType(columnIndex)) {
                FIELD_TYPE_NULL -> null
                FIELD_TYPE_INTEGER -> this.getIntOrNull(columnIndex)
                FIELD_TYPE_FLOAT -> this.getFloatOrNull(columnIndex)
                FIELD_TYPE_STRING -> this.getStringOrNull(columnIndex)
                FIELD_TYPE_BLOB -> this.getBlobOrNull(columnIndex)
                else -> null
            }

    fun Int.getDataType(): Class<*> =
            when (this) {
                FIELD_TYPE_INTEGER -> Long::class.java
                FIELD_TYPE_STRING -> String::class.java
                FIELD_TYPE_FLOAT -> Double::class.java
                FIELD_TYPE_BLOB -> ByteArray::class.java
                else -> Any::class.java
            }
}
