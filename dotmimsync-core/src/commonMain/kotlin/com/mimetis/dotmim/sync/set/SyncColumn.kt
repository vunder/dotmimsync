package com.mimetis.dotmim.sync.set

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.mimetis.dotmim.sync.setup.DbType
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
class SyncColumn(
        @SerialName("n")
        var columnName: String,

        @SerialName("dt")
        var dataType: String,

        @SerialName("an")
        var allowDBNull: Boolean = false,

        @SerialName("iu")
        var isUnique: Boolean = false,

        @SerialName("ir")
        var isReadOnly: Boolean = false,

        @SerialName("ia")
        var isAutoIncrement: Boolean = false,

        @SerialName("seed")
        var autoIncrementSeed: Int = 0,

        @SerialName("step")
        var autoIncrementStep: Int = 0,

        @SerialName("ius")
        var isUnsigned: Boolean = false,

        @SerialName("iuc")
        var isUnicode: Boolean = false,

        @SerialName("ico")
        var isCompute: Boolean = false,

        @SerialName("ml")
        var maxLength: Int = 0,

        @SerialName("o")
        var ordinal: Int = 0,

        @SerialName("ps")
        var precisionSpecified: Boolean = false,

        @SerialName("p1")
        var precision: Byte = 0,

        @SerialName("ss")
        var scaleSpecified: Boolean = false,

        @SerialName("sc")
        var scale: Byte = 0,

        @SerialName("odb")
        var originalDbType: String = "",

        @SerialName("oty")
        var originalTypeName: String = "",

        @SerialName("db")
        var dbType: Int,

        @SerialName("dv")
        var defaultValue: String? = null
) : SyncNamedItem<SyncColumn>() {
    constructor(columnName: String, type: KClass<*>) : this(columnName) {
        this.setType(type)
    }

    constructor(columnName: String) : this(columnName, dataType = "-1", dbType = 0)

    /**
     * Compress string representation of the DataType to be more concise in the serialized stream
     */
    private fun getAssemblyQualifiedName(valueType: KClass<*>): String =
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

    /**
     * Evaluate DbType, if needed
     */
    fun coerceDbType(): DbType =
            when {
                dataType == "1" ->
                    DbType.Boolean
                dataType == "2" ->
                    DbType.Byte
                dataType == "3" ->
                    DbType.StringFixedLength
                dataType == "4" ->
                    DbType.Double
                dataType == "5" ->
                    DbType.Single
                dataType == "6" ->
                    DbType.Int32
                dataType == "7" ->
                    DbType.Int64
                dataType == "8" ->
                    DbType.Int16
                dataType == "9" ->
                    DbType.UInt32
                dataType == "10" ->
                    DbType.UInt64
                dataType == "11" ->
                    DbType.UInt16
                dataType == "12" ->
                    DbType.Binary
                dataType == "13" ->
                    DbType.DateTime
                dataType == "14" ->
                    DbType.DateTimeOffset
                dataType == "15" ->
                    DbType.Decimal
                dataType == "16" ->
                    DbType.Guid
                dataType == "17" && maxLength <= 0 ->
                    DbType.String
                dataType == "17" && maxLength > 0 ->
                    DbType.StringFixedLength
                dataType == "18" ->
                    DbType.SByte
                dataType == "19" ->
                    DbType.Time
                dataType == "20" ->
                    DbType.Binary
                else ->
                    DbType.Object
            }

    fun getDbType(): DbType =
            DbType.values()[this.dbType]

    /**
     * Get auto inc values, coercing Step
     */
    fun getAutoIncrementSeedAndStep(): Pair<Int, Int> {
        val seed = this.autoIncrementSeed
        val step = if (this.autoIncrementStep <= 0) 1 else this.autoIncrementStep

        return Pair(seed, step)
    }

    /**
     * Clone a SyncColumn
     */
    fun clone(): SyncColumn =
            SyncColumn(
                    columnName = this.columnName,
                    dataType = this.dataType,
                    allowDBNull = this.allowDBNull,
                    isUnique = this.isUnique,
                    isReadOnly = this.isReadOnly,
                    isAutoIncrement = this.isAutoIncrement,
                    autoIncrementSeed = this.autoIncrementSeed,
                    autoIncrementStep = this.autoIncrementStep,
                    isUnsigned = this.isUnsigned,
                    isUnicode = this.isUnicode,
                    isCompute = this.isCompute,
                    maxLength = this.maxLength,
                    ordinal = this.ordinal,
                    precisionSpecified = this.precisionSpecified,
                    precision = this.precision,
                    scaleSpecified = this.scaleSpecified,
                    scale = this.scale,
                    originalDbType = this.originalDbType,
                    originalTypeName = this.originalTypeName,
                    dbType = this.dbType,
                    defaultValue = this.defaultValue
            )
//            copy() //in data class only

    /**
     * Get DataType from compressed string type
     */
    fun getDataType(): KClass<*> =
            when (this.dataType) {
                "1" ->
                    Boolean::class
                "2" ->
                    Byte::class
                "3" ->
                    Char::class
                "4" ->
                    Double::class
                "5" ->
                    Float::class
                "6" ->
                    Int::class
                "7" ->
                    Long::class
                "8" ->
                    Short::class
                "9" ->
                    UInt::class
                "10" ->
                    ULong::class
                "11" ->
                    UShort::class
                "12" ->
                    ByteArray::class
                "13" ->
                    LocalDateTime::class
//                "14"->
//                    DateTimeOffset::class.java
                "15" ->
                    BigDecimal::class
                "16" ->
                    Uuid::class
                "17" ->
                    String::class
//                "18"->
//                    SByte::class.java
//                "19"->
//                    TimeSpan::class.java
                "20" ->
                    CharArray::class
                else ->
                    Any::class//KClass.forName(this.dataType)
            }

    fun setType(type: KClass<*>) {
        this.dataType = getAssemblyQualifiedName(type)
        this.dbType = coerceDbType().value
    }

    override fun getAllNamesProperties(): List<String> =
            listOf(this.columnName)

    companion object {
        inline fun <reified T : Any> create(columnName: String): SyncColumn =
                SyncColumn(columnName, T::class)
    }
}
