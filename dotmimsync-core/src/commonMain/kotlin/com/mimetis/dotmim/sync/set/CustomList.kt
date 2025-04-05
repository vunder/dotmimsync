package com.mimetis.dotmim.sync.set

abstract class CustomList<T>() {
    protected val internalList = ArrayList<T>()

    constructor(list: List<T>) : this() {
        internalList.addAll(list)
    }

    fun add(item: T) = internalList.add(item)

    operator fun get(index: Int): T = internalList[index]

    open fun clear() {
        internalList.clear()
    }

    val size
        get() = internalList.size
}