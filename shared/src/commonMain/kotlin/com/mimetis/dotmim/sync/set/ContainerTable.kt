package com.mimetis.dotmim.sync.set

import com.benasher44.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.PrimitiveSerializer
import com.mimetis.dotmim.sync.serialization.DmUtils
import java.math.BigDecimal
import java.util.*

@Serializable
class ContainerTable(
        @SerialName("n")
        var tableName: String,

        @SerialName("s")
        var schemaName: String? = null,

        @SerialName("r")
        var rows: MutableList<Array<@Serializable(with = PrimitiveSerializer::class) Any?>>? = null
) : SyncNamedItem<ContainerTable>() {

    constructor(table: SyncTable) : this(table.tableName, table.schemaName)

    fun clear() =
            this.rows?.clear()

    override fun getAllNamesProperties(): List<String?> =
            listOf(tableName, schemaName)

    companion object {
        /**
         * Calculate an estimation of the dictionary values size
         */
        fun getRowSizeFromDataRow(itemArray: Array<Any?>): Long {
            var byteCount: Long = 0

            for (obj in itemArray) {
                val objType = if (obj != null) obj::class.java else null

                if (obj == null)
                    byteCount += 5
//                else if (obj is DBNull)
//                    byteCount += 5
                else if (objType == String::class.java)
                    byteCount += (obj as String).length
                else if (objType == ByteArray::class.java)
                    byteCount += (obj as ByteArray).size
                else
                    byteCount += getSizeForType(obj::class.java)

                // Size for the type
                if (objType != null)
                    byteCount += DmUtils.getAssemblyQualifiedName(objType).length

                // State
                byteCount += 4

                // Index
                byteCount += 4

            }
            return byteCount
        }

        /**
         * Gets a size for a given type
         */
        fun getSizeForType(type: Class<*>): Long =
                when (type) {
                    Long::class.java, ULong::class.java, Double::class.java, Date::class.java ->
                        8
                    Boolean::class.java, Byte::class.java ->
                        1
                    Char::class.java, Short::class.java, UShort::class.java ->
                        2
                    Int::class.java, UInt::class.java, Float::class.java ->
                        4
                    BigDecimal::class.java, Uuid::class.java ->
                        16
                    else ->
                        0
                }
    }
}
