package com.mimetis.dotmim.sync.sqlite

import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteStatement
import kotlin.reflect.KClass

object CursorHelper {
    fun SQLiteStatement.getValue(columnIndex: Int): Any? =
        if (this.isNull(columnIndex))
            null
    else
            when (this.getColumnType(columnIndex)) {
                SQLITE_DATA_NULL -> null
                SQLITE_DATA_INTEGER -> this.getInt(columnIndex)
                SQLITE_DATA_FLOAT -> this.getDouble(columnIndex)
                SQLITE_DATA_TEXT -> this.getText(columnIndex)
                SQLITE_DATA_BLOB -> this.getBlob(columnIndex)
                else -> null
            }

    fun Int.getDataType(): KClass<*> =
            when (this) {
                SQLITE_DATA_INTEGER -> Long::class
                SQLITE_DATA_TEXT -> String::class
                SQLITE_DATA_FLOAT -> Double::class
                SQLITE_DATA_BLOB -> ByteArray::class
                else -> Any::class
            }
}
