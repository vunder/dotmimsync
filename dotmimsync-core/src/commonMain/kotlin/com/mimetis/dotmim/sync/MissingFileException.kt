package com.mimetis.dotmim.sync

class MissingFileException(fileName: String) : Exception("File $fileName doesn't exist.") {
}
