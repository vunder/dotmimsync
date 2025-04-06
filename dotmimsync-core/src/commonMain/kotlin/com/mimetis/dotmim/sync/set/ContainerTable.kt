package com.mimetis.dotmim.sync.set

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.mimetis.dotmim.sync.PrimitiveSerializer
import com.mimetis.dotmim.sync.serialization.DmUtils
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    @OptIn(ExperimentalUuidApi::class)
    companion object {
        /**
         * Calculate an estimation of the dictionary values size
         */
        fun getRowSizeFromDataRow(itemArray: Array<Any?>): Long {
            var byteCount: Long = 0

            for (obj in itemArray) {
                val objType = if (obj != null) obj::class else null

                if (obj == null)
                    byteCount += 5
//                else if (obj is DBNull)
//                    byteCount += 5
                else if (objType == String::class)
                    byteCount += (obj as String).length
                else if (objType == ByteArray::class)
                    byteCount += (obj as ByteArray).size
                else
                    byteCount += getSizeForType(obj::class)

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
        fun getSizeForType(type: KClass<*>): Long =
                when (type) {
                    Long::class, ULong::class, Double::class, LocalDateTime::class ->
                        8
                    Boolean::class, Byte::class ->
                        1
                    Char::class, Short::class, UShort::class ->
                        2
                    Int::class, UInt::class, Float::class ->
                        4
                    BigDecimal::class, Uuid::class ->
                        16
                    else ->
                        0
                }
    }
}
