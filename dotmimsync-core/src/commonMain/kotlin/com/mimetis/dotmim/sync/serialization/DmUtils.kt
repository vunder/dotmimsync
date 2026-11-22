package com.mimetis.dotmim.sync.serialization

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDateTime
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object DmUtils {
    fun getAssemblyQualifiedName(valueType: KClass<*>): String =
            when (valueType) {
                Boolean::class ->
                    "1"
                Byte::class ->
                    "2"
                Char::class ->
                    "3"
                Double::class ->
                    "4"
                Float::class ->
                    "5"
                Int::class ->
                    "6"
                Long::class ->
                    "7"
                Short::class ->
                    "8"
                UInt::class ->
                    "9"
                ULong::class ->
                    "10"
                UShort::class ->
                    "11"
                ByteArray::class ->
                    "12"
                LocalDateTime::class ->
                    "13"
//                 DateTimeOffset::class.java ->
//                    "14"
                BigDecimal::class ->
                    "15"
                Uuid::class ->
                    "16"
                String::class ->
                    "17"
//                    SByte::class.java ->
//                        "18"
//                    TimeSpan::class.java ->
//                        "19"
                CharArray::class ->
                    "20"
                else ->
                    valueType.simpleName ?: valueType.toString()
            }
}
