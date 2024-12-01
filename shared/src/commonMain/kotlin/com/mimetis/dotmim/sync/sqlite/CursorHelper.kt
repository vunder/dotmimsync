package com.mimetis.dotmim.sync.sqlite

import android.database.Cursor
import android.database.Cursor.*

object CursorHelper {
    fun Cursor.getValue(columnIndex: Int): Any? =
        if (this.isNull(columnIndex))
            null
    else
            when (this.getType(columnIndex)) {
                FIELD_TYPE_NULL -> null
                FIELD_TYPE_INTEGER -> this.getInt(columnIndex)
                FIELD_TYPE_FLOAT -> this.getDouble(columnIndex)
                FIELD_TYPE_STRING -> this.getString(columnIndex)
                FIELD_TYPE_BLOB -> this.getBlob(columnIndex)
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
