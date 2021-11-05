package com.mimetis.dotmim.sync

import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.set.SyncColumns
import com.mimetis.dotmim.sync.set.SyncNamedItem
import java.text.SimpleDateFormat
import java.util.*

fun SyncColumns?.compareWith(other: SyncColumns?): Boolean {
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
            val cSourceItem = sourceItem as? SyncNamedItem<SyncColumn>
            val cOtherItem = otherItem as? SyncNamedItem<SyncColumn>

            if (cSourceItem != null && cOtherItem != null)
                return cSourceItem.equalsByProperties(otherItem)
            else
                return sourceItem.equals(otherItem)

        }
    }
}

fun utcNow(): Long {
    val time = Calendar.getInstance(Locale.getDefault()).timeInMillis
    try {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
        val date = Date(time)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val strDate = dateFormat.format(date)
//            System.out.println("Local Millis * " + date.getTime() + "  ---UTC time  " + strDate);//correct

        val dateFormatLocal = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
        val utcDate = dateFormatLocal.parse(strDate)
//            System.out.println("UTC Millis * " + utcDate.getTime() + " ------  " + dateFormatLocal.format(utcDate));
        val utcMillis = utcDate?.time ?: Date().time
        return utcMillis
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return time
}
