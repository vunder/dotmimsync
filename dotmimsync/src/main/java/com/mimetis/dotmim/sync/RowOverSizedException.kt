package com.mimetis.dotmim.sync

class RowOverSizedException(finalFieldSize: String) : Exception("Row is too big ($finalFieldSize kb.) for the current DownloadBatchSizeInKB.") {
}
