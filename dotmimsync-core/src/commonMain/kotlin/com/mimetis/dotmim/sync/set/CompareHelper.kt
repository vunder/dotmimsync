package com.mimetis.dotmim.sync.set

fun <E> List<E>?.compareWith(other: List<E>?): Boolean {
    // checking null ref
    if ((this == null && other != null) || (this != null && other == null))
        return false

    // If both are null, return true
    if (this == null && other == null)
        return true

    if (this!!.size != other!!.size)
        return false

    // Check all items are identical
    return this.all { sourceItem ->
        other.any { otherItem ->
            val cSourceItem = sourceItem as? SyncNamedItem<E>
            val cOtherItem = sourceItem as? SyncNamedItem<E>

            if (cSourceItem != null && cOtherItem != null)
                return cSourceItem.equalsByProperties(otherItem)
            else
                sourceItem == otherItem
        }
    }
}

fun <E> CustomList<E>?.compareWith(other: CustomList<E>?): Boolean {
    // checking null ref
    if ((this == null && other != null) || (this != null && other == null))
        return false

    // If both are null, return true
    if (this == null && other == null)
        return true

    if (this!!.size != other!!.size)
        return false

    // Check all items are identical
    return this.all { sourceItem ->
        other.any { otherItem ->
            val cSourceItem = sourceItem as? SyncNamedItem<E>
            val cOtherItem = sourceItem as? SyncNamedItem<E>

            if (cSourceItem != null && cOtherItem != null)
                return cSourceItem.equalsByProperties(otherItem)
            else
                sourceItem == otherItem
        }
    }
}

//fun SyncColumns?.compareWith(other: SyncColumns?): Boolean {
//    // checking null ref
//    if ((this == null && other != null) || (this != null && other == null))
//        return false
//
//    // If both are null, return true
//    if (this == null && other == null)
//        return true
//
//    if (this!!.size != other!!.size)
//        return false;
//
//    // Check all items are identical
//    return this.all { sourceItem ->
//        other.any { otherItem ->
//            val cSourceItem = sourceItem as? SyncNamedItem<SyncColumn>
//            val cOtherItem = otherItem as? SyncNamedItem<SyncColumn>
//
//            if (cSourceItem != null && cOtherItem != null)
//                return cSourceItem.equalsByProperties(otherItem);
//            else
//                return sourceItem.equals(otherItem);
//
//        }
//    }
//}
