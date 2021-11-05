package com.mimetis.dotmim.sync.set

abstract class SyncNamedItem<T> {
    /**
     * Get all comparable names properties to determine if two instances are identifed as "same" based on their name properties
     */
    abstract fun getAllNamesProperties(): List<String?>

    /**
     * Gets a true boolean if other instance has the same name, defined by properties marked as names
     */
    fun equalsByName(otherInstance: T?): Boolean {
        if (otherInstance == null)
            return false

        val namedOtherInstance = otherInstance as? SyncNamedItem<T>

        if (namedOtherInstance == null)
            return false

        val props1 = this.getAllNamesProperties().iterator()
        val props2 = namedOtherInstance.getAllNamesProperties().iterator()

        while (props1.hasNext()) {
            val prop1 = props1.next()
            val prop2 = props2.next()

            if (prop1.isNullOrBlank() && !prop2.isNullOrBlank())
                return false

            if (!prop1.isNullOrBlank() && prop2.isNullOrBlank())
                return false

            if (prop1.isNullOrBlank() && prop2.isNullOrBlank())
                continue

            if (!prop1.equals(prop2, true))
                return false
        }

        return true
    }

    /**
     * Gets a true boolean if other instance is defined as same based on all properties
     * By default, if not overriden, check the names properties
     */
    open fun equalsByProperties(otherInstance: T?) =
            this.equalsByName(otherInstance)

    override fun equals(other: Any?): Boolean =
            this.equalsByProperties(other as? T)

    override fun hashCode(): Int =
            javaClass.hashCode()
}
