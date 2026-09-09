package com.mimetis.dotmim.sync

class OutOfDateException(
    timestampLimit: Long?,
    serverLastCleanTimestamp: Long
) : Exception("Client database is out of date. Last client sync timestamp:$timestampLimit. Last server cleanup metadata:$serverLastCleanTimestamp Try to make a Reinitialize sync.")
