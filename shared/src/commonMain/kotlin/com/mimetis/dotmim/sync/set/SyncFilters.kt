package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.setup.SetupFilter

@Serializable(with = SyncFiltersSerializer::class)
class SyncFilters() : ArrayList<SyncFilter>() {
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

object SyncFiltersSerializer : ArrayListLikeSerializer<SyncFilters, SyncFilter>(SyncFilter.serializer())
