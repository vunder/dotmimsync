package com.mimetis.dotmim.sync.setup

import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.set.CustomList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

/**
 * Represents a list of tables to be added to the sync process
 */
@Serializable(with = SetupTablesSerializer::class)
class SetupTables() : CustomList<SetupTable>() {
    internal constructor(items: List<SetupTable>) : this() {
        internalList.addAll(items)
    }

    /**
     * Get a table by its name
     */
    operator fun get(tableName: String, schemaName: String): SetupTable? {
        if (tableName.isBlank())
            throw Exception("`tableName' parameter in empty ($tableName)")

        val scn = if (schemaName.isBlank()) "" else schemaName
        val table = this.firstOrNull { innerTable ->
            val innerTableSchemaName = if (innerTable.schemaName.isBlank()) "" else innerTable.schemaName
            return@firstOrNull innerTable.tableName.equals(tableName, true) && innerTableSchemaName.equals(scn, true)

        }
        return table
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SetupTablesSerializer : ArrayListLikeSerializer<SetupTables, SetupTable>(SetupTable.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SetupTable>()

    override fun deserialize(decoder: Decoder): SetupTables {
        val items = ArrayList<SetupTable>()

        val compositeDecoder = decoder.beginStructure(descriptor)

        if (compositeDecoder.decodeSequentially()) {
            val size = compositeDecoder.decodeCollectionSize(descriptor)
            for (i in 0 until size) {
                val element = compositeDecoder.decodeSerializableElement(
                    elementSerializer.descriptor,
                    i,
                    elementSerializer
                )
                items.add(element)
            }
        } else {
            while (true) {
                val index =
                    compositeDecoder.decodeElementIndex(elementSerializer.descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                val element = compositeDecoder.decodeSerializableElement(
                    elementSerializer.descriptor,
                    index,
                    elementSerializer
                )
                items.add(element)
            }
        }
        compositeDecoder.endStructure(descriptor)

        return SetupTables(items)
    }
}
