package com.mimetis.dotmim.sync.sqlite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.mimetis.dotmim.sync.SyncVersion
import com.mimetis.dotmim.sync.builders.DbScopeBuilder
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.SyncSetup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SqliteScopeBuilder(
    scopeInfoTableName: String,
    private val database: SQLiteDatabase
) : DbScopeBuilder(scopeInfoTableName) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun existsScopeInfoTable(): Boolean =
        database.rawQuery(
            "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='${scopeInfoTableName.unquoted()}'",
            null
        ).use { cursor ->
            cursor.moveToNext() && cursor.getInt(0) > 0
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
        database.rawQuery(
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
                            WHERE sync_scope_name = ?""",
            arrayOf(scopeName)
        ).use { cursor ->
            ArrayList<ScopeInfo>().apply {
                while (cursor.moveToNext()) {
                    add(readScope(cursor, json))
                }
            }
        }

    override fun existsScopeInfo(scopeId: Uuid): Boolean =
        database.rawQuery(
            "Select count(*) from ${scopeInfoTableName.unquoted()} where sync_scope_id=?",
            arrayOf(scopeId.toString())
        ).use { cursor ->
            cursor.moveToNext() && cursor.getInt(0) > 0
        }

    override fun insertScope(scopeInfo: ScopeInfo): ScopeInfo {
        database.execSQL(
            "Insert into ${scopeInfoTableName.unquoted()} (sync_scope_name, sync_scope_schema, sync_scope_setup, sync_scope_version, scope_last_sync, scope_last_sync_duration, scope_last_server_sync_timestamp, scope_last_sync_timestamp, sync_scope_id) values(?,?,?,?,?,?,?,?,?)",
            arrayOf(
                scopeInfo.name,
                if (scopeInfo.schema != null) json.encodeToString(scopeInfo.schema) else null,
                if (scopeInfo.setup != null) json.encodeToString(scopeInfo.setup) else null,
                scopeInfo.version,
                scopeInfo.lastSync?.toString(),
                scopeInfo.lastSyncDuration.toString(),
                scopeInfo.lastServerSyncTimestamp?.toString(),
                scopeInfo.lastSyncTimestamp?.toString(),
                scopeInfo.id.toString()
            )
        )
        return getAllScopes(scopeInfo.name).first()
    }

    override fun updateScope(scopeInfo: ScopeInfo): ScopeInfo {
        database.execSQL(
            "Update ${scopeInfoTableName.unquoted()} set sync_scope_name=?, sync_scope_schema=?, sync_scope_setup=?, sync_scope_version=?, scope_last_sync=?, scope_last_server_sync_timestamp=?, scope_last_sync_timestamp=?, scope_last_sync_duration=? where sync_scope_id=?",
            arrayOf(
                scopeInfo.name,
                if (scopeInfo.schema != null) json.encodeToString(scopeInfo.schema) else null,
                if (scopeInfo.setup != null) json.encodeToString(scopeInfo.setup) else null,
                scopeInfo.version,
                scopeInfo.lastSync?.toString(),
                scopeInfo.lastServerSyncTimestamp?.toString(),
                scopeInfo.lastSyncTimestamp?.toString(),
                scopeInfo.lastSyncDuration.toString(),
                scopeInfo.id.toString()
            )
        )
        return getAllScopes(scopeInfo.name).first()
    }

    override fun getLocalTimestamp(): Long =
        database.rawQuery("Select " + SqliteTableBuilder.TimestampValue, null).use { cursor ->
            if (cursor.moveToNext())
                cursor.getLong(0)
            else
                0L
        }

    override fun dropScopeInfoTable() {
        database.execSQL("DROP Table ${scopeInfoTableName.unquoted()}")
    }

    private fun readScope(cursor: Cursor, json: Json) =
        ScopeInfo(
            id = Uuid.parse(cursor.getString(cursor.getColumnIndex("sync_scope_id"))),
            name = cursor.getString(cursor.getColumnIndex("sync_scope_name")),
            schema = if (cursor.isNull(cursor.getColumnIndex("sync_scope_schema"))) null else json.decodeFromString<SyncSet>(
                cursor.getString(cursor.getColumnIndex("sync_scope_schema"))
            ),
            setup = if (cursor.isNull(cursor.getColumnIndex("sync_scope_setup"))) null else json.decodeFromString<SyncSetup>(
                cursor.getString(cursor.getColumnIndex("sync_scope_setup"))
            ),
            version = (if (cursor.isNull(cursor.getColumnIndex("sync_scope_version"))) null else cursor.getString(
                cursor.getColumnIndex("sync_scope_version")
            )) ?: SyncVersion.current,
            lastSync = if (cursor.isNull(cursor.getColumnIndex("scope_last_sync"))) null else cursor.getLong(
                cursor.getColumnIndex("scope_last_sync")
            ),
            lastServerSyncTimestamp = if (cursor.isNull(cursor.getColumnIndex("scope_last_server_sync_timestamp"))) null else cursor.getLong(
                cursor.getColumnIndex("scope_last_server_sync_timestamp")
            ),
            lastSyncTimestamp = if (cursor.isNull(cursor.getColumnIndex("scope_last_sync_timestamp"))) null else cursor.getLong(
                cursor.getColumnIndex("scope_last_sync_timestamp")
            ),
            lastSyncDuration = if (cursor.isNull(cursor.getColumnIndex("scope_last_sync_duration"))) 0 else cursor.getLong(
                cursor.getColumnIndex("scope_last_sync_duration")
            ),
            isNewScope = false
        )
}
