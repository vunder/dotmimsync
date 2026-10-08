package com.mimetis.dotmim.sync.manager

class DbRelationColumnDefinition {
    lateinit var keyColumnName: String
    lateinit var referenceColumnName: String
    var order: Int = 0
}
