package com.mimetis.dotmim.sync.set

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.setup.DbType
import java.math.BigDecimal
import java.util.*

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
    constructor(columnName: String, type: Class<*>) : this(columnName) {
        this.setType(type)
    }

    constructor(columnName: String) : this(columnName, dataType = "-1", dbType = 0)

    /**
     * Compress string representation of the DataType to be more concise in the serialized stream
     */
    private fun getAssemblyQualifiedName(valueType: Class<*>): String =
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
                UUID::class.java ->
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
    fun getDataType(): Class<*> =
            when (this.dataType) {
                "1" ->
                    Boolean::class.java
                "2" ->
                    Byte::class.java
                "3" ->
                    Char::class.java
                "4" ->
                    Double::class.java
                "5" ->
                    Float::class.java
                "6" ->
                    Int::class.java
                "7" ->
                    Long::class.java
                "8" ->
                    Short::class.java
                "9" ->
                    UInt::class.java
                "10" ->
                    ULong::class.java
                "11" ->
                    UShort::class.java
                "12" ->
                    ByteArray::class.java
                "13" ->
                    Date::class.java
//                "14"->
//                    DateTimeOffset::class.java
                "15" ->
                    BigDecimal::class.java
                "16" ->
                    UUID::class.java
                "17" ->
                    String::class.java
//                "18"->
//                    SByte::class.java
//                "19"->
//                    TimeSpan::class.java
                "20" ->
                    CharArray::class.java
                else ->
                    Class.forName(this.dataType)
            }

    fun setType(type: Class<*>) {
        this.dataType = getAssemblyQualifiedName(type)
        this.dbType = coerceDbType().value
    }

    override fun getAllNamesProperties(): List<String> =
            listOf(this.columnName)

    companion object {
        inline fun <reified T : Any> create(columnName: String): SyncColumn =
                SyncColumn(columnName, T::class.java)
    }
}
