package com.mimetis.dotmim.sync

class MissingLocalOrchestratorSchemaException : Exception("Schema does not exists yet in your local database. You must make a first sync with your server, to initialize everything required locally.") {
}
