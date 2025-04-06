package com.mimetis.dotmim.sync.sqlite

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import com.mimetis.dotmim.sync.SyncVersion
import com.mimetis.dotmim.sync.builders.DbScopeBuilder
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SqliteScopeBuilder(
    scopeInfoTableName: String,
    private val database: SQLiteConnection
) : DbScopeBuilder(scopeInfoTableName) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun existsScopeInfoTable(): Boolean =
        database.prepare(
            "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='${scopeInfoTableName.unquoted()}'"
        ).use { cursor ->
            cursor.step() && cursor.getInt(0) > 0
        }


    override fun createScopeInfoTable() {
        database.execSQL(
            """CREATE TABLE ${scopeInfoTableName.unquoted()}(
                         sync_scope_id blob NOT NULL PRIMARY KEY,
                         sync_scope_name text NOT NULL,
                         sync_scope_schema text NULL,
                         sync_scope_setup text NULL,
                         sync_scope_version text NULL,
                         scope_last_server_sync_timestamp integer NULL,
                         scope_last_sync_timestamp integer NULL,
                         scope_last_sync_duration integer NULL,
                         scope_last_sync datetime NULL
                        )"""
        )
    }

    override fun getAllScopes(scopeName: String): MutableList<ScopeInfo> =
        database.prepare(
            """SELECT sync_scope_id
                            , sync_scope_name
                            , sync_scope_schema
                            , sync_scope_setup
                            , sync_scope_version
                            , scope_last_sync
                            , scope_last_server_sync_timestamp
                            , scope_last_sync_timestamp
                            , scope_last_sync_duration
                            FROM ${scopeInfoTableName.unquoted()}
                            WHERE sync_scope_name = ?"""
        ).use { cursor ->
            cursor.bindText(1, scopeName)
            ArrayList<ScopeInfo>().apply {
                while (cursor.step()) {
                    add(readScope(cursor, json))
                }
            }
        }

    override fun existsScopeInfo(scopeId: Uuid): Boolean =
        database.prepare(
            "Select count(*) from ${scopeInfoTableName.unquoted()} where sync_scope_id=?"
        ).use { cursor ->
            cursor.bindText(1, scopeId.toString())
            cursor.step() && cursor.getInt(0) > 0
        }

    override fun insertScope(scopeInfo: ScopeInfo): ScopeInfo {
        database.prepare(
            "Insert into ${scopeInfoTableName.unquoted()} (sync_scope_name, sync_scope_schema, sync_scope_setup, sync_scope_version, scope_last_sync, scope_last_sync_duration, scope_last_server_sync_timestamp, scope_last_sync_timestamp, sync_scope_id) values(?,?,?,?,?,?,?,?,?)"
        ).use {
            it.bindText(1, scopeInfo.name)
            if (scopeInfo.schema != null)
                it.bindText(2, json.encodeToString(scopeInfo.schema))
            else
                it.bindNull(2)
            if (scopeInfo.setup != null)
                it.bindText(3, json.encodeToString(scopeInfo.setup))
            else
                it.bindNull(3)
            it.bindText(4, scopeInfo.version)
            if (scopeInfo.lastSync != null)
                it.bindLong(5, scopeInfo.lastSync!!)
            else
                it.bindNull(5)
            it.bindLong(6, scopeInfo.lastSyncDuration)
            if (scopeInfo.lastServerSyncTimestamp != null)
                it.bindLong(7, scopeInfo.lastServerSyncTimestamp!!)
            else
                it.bindNull(7)
            if (scopeInfo.lastSyncTimestamp != null)
                it.bindLong(8, scopeInfo.lastSyncTimestamp!!)
            else
                it.bindNull(8)
            it.bindText(9, scopeInfo.id.toString())

            it.step()
        }
        return getAllScopes(scopeInfo.name).first()
    }

    override fun updateScope(scopeInfo: ScopeInfo): ScopeInfo {
        database.prepare(
            "Update ${scopeInfoTableName.unquoted()} set sync_scope_name=?, sync_scope_schema=?, sync_scope_setup=?, sync_scope_version=?, scope_last_sync=?, scope_last_server_sync_timestamp=?, scope_last_sync_timestamp=?, scope_last_sync_duration=? where sync_scope_id=?"
        ).use {
            it.bindText(1, scopeInfo.name)
            if (scopeInfo.schema != null)
                it.bindText(2, json.encodeToString(scopeInfo.schema))
            else
                it.bindNull(2)
            if (scopeInfo.setup != null)
                it.bindText(3, json.encodeToString(scopeInfo.setup))
            else
                it.bindNull(3)
            it.bindText(4, scopeInfo.version)
            if (scopeInfo.lastSync != null)
                it.bindLong(5, scopeInfo.lastSync!!)
            else
                it.bindNull(5)
            if (scopeInfo.lastServerSyncTimestamp != null)
                it.bindLong(6, scopeInfo.lastServerSyncTimestamp!!)
            else
                it.bindNull(6)
            if (scopeInfo.lastSyncTimestamp != null)
                it.bindLong(7, scopeInfo.lastSyncTimestamp!!)
            else
                it.bindNull(7)
            it.bindLong(8, scopeInfo.lastSyncDuration)
            it.bindText(9, scopeInfo.id.toString())

            it.step()
        }
        return getAllScopes(scopeInfo.name).first()
    }

    override fun getLocalTimestamp(): Long =
        database.prepare("Select " + SqliteTableBuilder.TimestampValue).use { cursor ->
            if (cursor.step())
                cursor.getLong(0)
            else
                0L
        }

    override fun dropScopeInfoTable() {
        database.execSQL("DROP Table ${scopeInfoTableName.unquoted()}")
    }

    private fun readScope(cursor: SQLiteStatement, json: Json): ScopeInfo {
        val columns = cursor.getColumnNames()
        return ScopeInfo(
            id = Uuid.parse(cursor.getText(columns.indexOf("sync_scope_id"))),
            name = cursor.getText(columns.indexOf("sync_scope_name")),
            schema = if (cursor.isNull(columns.indexOf("sync_scope_schema")))
                null
            else json.decodeFromString<SyncSet>(cursor.getText(columns.indexOf("sync_scope_schema"))),
            setup = if (cursor.isNull(columns.indexOf("sync_scope_setup")))
                null
            else
                json.decodeFromString<SyncSetup>(cursor.getText(columns.indexOf("sync_scope_setup"))),
            version = if (cursor.isNull(columns.indexOf("sync_scope_version")))
                SyncVersion.current
            else
                cursor.getText(columns.indexOf("sync_scope_version")),
            lastSync = if (cursor.isNull(columns.indexOf("scope_last_sync")))
                null
            else
                cursor.getLong(columns.indexOf("scope_last_sync")),
            lastServerSyncTimestamp = if (cursor.isNull(columns.indexOf("scope_last_server_sync_timestamp")))
                null
            else
                cursor.getLong(columns.indexOf("scope_last_server_sync_timestamp")),
            lastSyncTimestamp = if (cursor.isNull(columns.indexOf("scope_last_sync_timestamp")))
                null
            else
                cursor.getLong(columns.indexOf("scope_last_sync_timestamp")),
            lastSyncDuration = if (cursor.isNull(columns.indexOf("scope_last_sync_duration")))
                0
            else
                cursor.getLong(columns.indexOf("scope_last_sync_duration")),
            isNewScope = false
        )
    }
}
