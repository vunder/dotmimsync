package com.mimetis.dotmim.sync.sqlite

import com.mimetis.dotmim.sync.data.EnumWithValue

enum class SqliteType(override val value: Int) : EnumWithValue {
    /**
     * A signed integer.
     */
    Integer(1),

    /**
     * A floating point value.
     */
    Real(2),

    /**
     * A text string.
     */
    Text(3),

    /**
     * A blob of data.
     */
    Blob(4)
}
