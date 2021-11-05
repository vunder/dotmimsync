package com.mimetis.dotmim.sync

/**
 * Occurs when a scope info is needed, but does not exists
 */
class MissingClientScopeInfoException():Exception("The client scope info is invalid. You need to make a first sync before.") {
}
