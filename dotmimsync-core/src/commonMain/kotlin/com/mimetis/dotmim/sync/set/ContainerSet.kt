package com.mimetis.dotmim.sync.set

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ContainerSet(
        @SerialName("t")
        var tables: ArrayList<ContainerTable> = ArrayList()
) {
    /**
     * Check if we have some tables in the container
     */
    val hasTables: Boolean
        get() = this.tables.isNotEmpty()

    /**
     * Getting the container rows count
     */
    fun rowsCount(): Int {
        if (!hasTables)
            return 0

        return this.tables.sumOf { t -> t.rows!!.size }
    }

    fun clear() {
        this.tables.forEach { t -> t.clear() }
        tables.clear()
    }
}
