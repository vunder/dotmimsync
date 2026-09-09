package com.mimetis.dotmim.sync.set

abstract class CustomList<T>() : Iterable<T> {
    protected val internalList = ArrayList<T>()

    open fun add(element: T) = internalList.add(element)

    operator fun get(index: Int): T = internalList[index]

    open fun clear() {
        internalList.clear()
    }

    val size
        get() = internalList.size

    override fun iterator(): Iterator<T> = internalList.iterator()

    fun isEmpty(): Boolean = internalList.isEmpty()
    fun isNotEmpty(): Boolean = internalList.isNotEmpty()

    fun addAll(items: Iterable<T>) = internalList.addAll(items)
}

fun<T> CustomList<T>?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()