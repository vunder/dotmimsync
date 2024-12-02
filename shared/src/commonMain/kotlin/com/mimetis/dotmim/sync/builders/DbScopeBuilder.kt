package com.mimetis.dotmim.sync.builders

import com.benasher44.uuid.Uuid
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import java.util.*

abstract class DbScopeBuilder(
        scopeInfoTableName: String
) {
    val scopeInfoTableName = ParserName.parse(scopeInfoTableName)

    abstract fun existsScopeInfoTable(): Boolean
    abstract fun createScopeInfoTable()
    abstract fun getAllScopes(scopeName: String): MutableList<ScopeInfo>
    abstract fun existsScopeInfo(scopeId: Uuid): Boolean
    abstract fun insertScope(scopeInfo: ScopeInfo): ScopeInfo
    abstract fun updateScope(scopeInfo: ScopeInfo): ScopeInfo
    abstract fun getLocalTimestamp(): Long
    abstract fun dropScopeInfoTable()
}
