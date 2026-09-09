package com.mimetis.dotmim.sync.set

import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.builders.ParserName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncTablesSerializer::class)
class SyncTables() : CustomList<SyncTable>() {
    internal constructor(items: List<SyncTable>) : this() {
        internalList.addAll(items)
    }

    /**
     * Table's schema
     */
    @Transient
    var schema: SyncSet? = null

    constructor(schema: SyncSet) : this() {
        this.schema = schema
    }

    /**
     * Since we don't serializer the reference to the schema, this method will reaffect the correct schema
     */
    fun ensureTables(schema: SyncSet) {
        this.schema = schema
        this.forEach { it.ensureTable(schema) }
    }

    override fun clear() {
        this.forEach { it.clear() }
        super.clear()
    }

    override fun add(element: SyncTable): Boolean {
        element.schema = schema
        return super.add(element)
    }

    /**
     * Get a table by its name
     */
    operator fun get(tableName: String, schemaName: String): SyncTable? {
        if (tableName.isBlank())
            throw Exception("`tableName' parameter in empty ($tableName)")

        val parser = ParserName.parse(tableName)
        val tblName = parser.objectName

        val scn = if (schemaName.isBlank()) "" else schemaName
        val table = this.firstOrNull { innerTable ->
            val innerTableSchemaName = if (innerTable.schemaName.isBlank()) "" else innerTable.schemaName
            return@firstOrNull innerTable.tableName.equals(tblName, true) && innerTableSchemaName.equals(scn, true)

        }
        return table
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SyncTablesSerializer : ArrayListLikeSerializer<SyncTables, SyncTable>(SyncTable.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncTable>()

    override fun deserialize(decoder: Decoder): SyncTables {
        val items = ArrayList<SyncTable>()

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

        return SyncTables(items)
    }
}
