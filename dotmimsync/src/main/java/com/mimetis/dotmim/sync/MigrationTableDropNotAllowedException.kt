package com.mimetis.dotmim.sync

/**
 * During a migration, droping a table is not allowed
 */
class MigrationTableDropNotAllowedException :
    Exception("During a migration, droping a table is not allowed")
