package com.mimetis.dotmim.sync.manager

class DbRelationDefinition {
    lateinit var foreignKey: String
    lateinit var tableName: String
    lateinit var schemaName: String

    val columns: ArrayList<DbRelationColumnDefinition> = ArrayList()
    lateinit var referenceTableName: String
    lateinit var referenceSchemaName: String
}
