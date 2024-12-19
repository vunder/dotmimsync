package com.mimetis.dotmim.sync.serialization

import com.benasher44.uuid.Uuid
import java.math.BigDecimal
import java.util.*

object DmUtils {
    fun getAssemblyQualifiedName(valueType: Class<*>): String =
            when (valueType) {
                Boolean::class.java ->
                    "1"
                Byte::class.java ->
                    "2"
                Char::class.java ->
                    "3"
                Double::class.java ->
                    "4"
                Float::class.java ->
                    "5"
                Int::class.java ->
                    "6"
                Long::class.java ->
                    "7"
                Short::class.java ->
                    "8"
                UInt::class.java ->
                    "9"
                ULong::class.java ->
                    "10"
                UShort::class.java ->
                    "11"
                ByteArray::class.java ->
                    "12"
                Date::class.java ->
                    "13"
//                 DateTimeOffset::class.java ->
//                    "14"
                BigDecimal::class.java ->
                    "15"
                Uuid::class.java ->
                    "16"
                String::class.java ->
                    "17"
//                    SByte::class.java ->
//                        "18"
//                    TimeSpan::class.java ->
//                        "19"
                CharArray::class.java ->
                    "20"
                else ->
                    valueType.name
            }
}
