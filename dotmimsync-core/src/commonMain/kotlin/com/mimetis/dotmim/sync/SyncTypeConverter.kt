package com.mimetis.dotmim.sync

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDateTime
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object SyncTypeConverter {
    fun <T> tryConvertTo(value: Any?): T? {
        if (value == null)
            return null

//        val typeOfT = T
//        val typeOfU = value::class.java

        return value as? T
    }

    fun tryConvertTo(value: Any?, typeOfT: KClass<*>): Any? =
            when {
                typeOfT == Short::class ->
                    tryConvertTo<Short>(value)
                typeOfT == Int::class ->
                    tryConvertTo<Int>(value)
                typeOfT == Long::class ->
                    tryConvertTo<Long>(value)
                typeOfT == UShort::class ->
                    tryConvertTo<UShort>(value)
                typeOfT == UInt::class ->
                    tryConvertTo<UInt>(value)
                typeOfT == ULong::class ->
                    tryConvertTo<ULong>(value)
                typeOfT == LocalDateTime::class ->
                    tryConvertTo<LocalDateTime>(value)
//            typeOfT == DateTimOffset::class.java->
//                tryConvertTo<DateTimOffset>(value)
                typeOfT == String::class ->
                    tryConvertTo<String>(value)
                typeOfT == Byte::class ->
                    tryConvertTo<Byte>(value)
                typeOfT == Boolean::class ->
                    tryConvertTo<Boolean>(value)
                typeOfT == Uuid::class ->
                    tryConvertTo<Uuid>(value)
                typeOfT == Char::class ->
                    tryConvertTo<Char>(value)
                typeOfT == BigDecimal::class ->
                    tryConvertTo<BigDecimal>(value)
                typeOfT == Double::class ->
                    tryConvertTo<Double>(value)
                typeOfT == Float::class ->
                    tryConvertTo<Float>(value)
//            typeOfT == SByte::class.java->
//                tryConvertTo<SByte>(value)
//            typeOfT == TimeSpan::class.java->
//                tryConvertTo<TimeSpan>(value)
                typeOfT == ByteArray::class ->
                    tryConvertTo<ByteArray>(value)
//            typeConverter.CanConvertFrom(typeOfT) ->
//                Convert.ChangeType(typeConverter.ConvertFrom(value), typeOfT, provider);
                else -> value
            }
}
