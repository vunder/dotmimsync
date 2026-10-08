package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.setup.SetupFilter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncFiltersSerializer::class)
class SyncFilters() : CustomList<SyncFilter>() {
    internal constructor(items: List<SyncFilter>) : this() {
        internalList.addAll(items)
    }

    fun add(setupFilter: SetupFilter) {
        val item = SyncFilter(setupFilter.tableName, setupFilter.schemaName, schema = this.schema)
        setupFilter.parameters.forEach { s ->
            item.parameters.add(SyncFilterParameter(
                    name = s.name,
                    schemaName = s.schemaName,
                    tableName = s.tableName,
                    dbType = s.dbType,
                    defaultValue = s.defaultValue,
                    allowNull = s.allowNull,
                    maxLength = s.maxLength
            ))
        }
        setupFilter.wheres.forEach { s ->
            item.wheres.add(SyncFilterWhereSideItem(
                    columnName = s.columnName,
                    tableName = s.tableName,
                    schemaName = s.schemaName,
                    parameterName = s.parameterName
            ))
        }
        setupFilter.joins.forEach { s ->
            item.joins.add(SyncFilterJoin(
                    tableName = s.tableName,
                    joinEnum = s.joinEnum,
                    leftTableName = s.leftTableName,
                    leftColumnName = s.leftColumnName,
                    rightTableName = s.rightTableName,
                    rightColumnName = s.rightColumnName
            ))
        }
    }

    /**
     * Filter's schema
     */
    @Transient
    var schema: SyncSet? = null

    constructor(schema: SyncSet) : this() {
        this.schema = schema
    }

    /**
     * Since we don't serializer the reference to the schema, this method will reaffect the correct schema
     */
    fun ensureFilters(schema: SyncSet) {
        this.schema = schema
        this.forEach { it.ensureFilter(schema) }
    }

    override fun clear() {
        this.forEach { it.clear() }
        super.clear()
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SyncFiltersSerializer : ArrayListLikeSerializer<SyncFilters, SyncFilter>(SyncFilter.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncFilter>()

    override fun deserialize(decoder: Decoder): SyncFilters {
        val items = ArrayList<SyncFilter>()

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

        return SyncFilters(items)
    }
}
