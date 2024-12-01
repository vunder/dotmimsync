package com.mimetis.dotmim.sync

import com.benasher44.uuid.Uuid
import java.math.BigDecimal
import java.util.*

object SyncTypeConverter {
    fun <T> tryConvertTo(value: Any?): T? {
        if (value == null)
            return null

//        val typeOfT = T
//        val typeOfU = value::class.java

        return value as? T
    }

    fun tryConvertTo(value: Any?, typeOfT: Class<*>): Any? =
            when {
                typeOfT == Short::class.java ->
                    tryConvertTo<Short>(value)
                typeOfT == Int::class.java ->
                    tryConvertTo<Int>(value)
                typeOfT == Long::class.java ->
                    tryConvertTo<Long>(value)
                typeOfT == UShort::class.java ->
                    tryConvertTo<UShort>(value)
                typeOfT == UInt::class.java ->
                    tryConvertTo<UInt>(value)
                typeOfT == ULong::class.java ->
                    tryConvertTo<ULong>(value)
                typeOfT == Date::class.java ->
                    tryConvertTo<Date>(value)
//            typeOfT == DateTimOffset::class.java->
//                tryConvertTo<DateTimOffset>(value)
                typeOfT == String::class.java ->
                    tryConvertTo<String>(value)
                typeOfT == Byte::class.java ->
                    tryConvertTo<Byte>(value)
                typeOfT == Boolean::class.java ->
                    tryConvertTo<Boolean>(value)
                typeOfT == Uuid::class.java ->
                    tryConvertTo<Uuid>(value)
                typeOfT == Char::class.java ->
                    tryConvertTo<Char>(value)
                typeOfT == BigDecimal::class.java ->
                    tryConvertTo<BigDecimal>(value)
                typeOfT == Double::class.java ->
                    tryConvertTo<Double>(value)
                typeOfT == Float::class.java ->
                    tryConvertTo<Float>(value)
//            typeOfT == SByte::class.java->
//                tryConvertTo<SByte>(value)
//            typeOfT == TimeSpan::class.java->
//                tryConvertTo<TimeSpan>(value)
                typeOfT == ByteArray::class.java ->
                    tryConvertTo<ByteArray>(value)
//            typeConverter.CanConvertFrom(typeOfT) ->
//                Convert.ChangeType(typeConverter.ConvertFrom(value), typeOfT, provider);
                else -> value
            }
}
