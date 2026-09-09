package com.mimetis.dotmim.sync.enumerations

import com.mimetis.dotmim.sync.data.EnumWithValue

/**
 * Gets the objects we want to provision or deprovision
 * Be careful, SyncProvision.Table, will (de)provision the data tables !
 */
enum class SyncProvision(override val value: Int) : EnumWithValue {
    None(0),
    Table(1),
    TrackingTable(2),
    StoredProcedures(4),
    Triggers(8),
    ClientScope(16),
    ServerScope(32),
    ServerHistoryScope(64),
}
