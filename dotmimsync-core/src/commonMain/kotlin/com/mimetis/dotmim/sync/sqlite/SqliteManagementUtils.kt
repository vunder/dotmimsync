package com.mimetis.dotmim.sync.sqlite

import com.mimetis.dotmim.sync.builders.ParserName

object SqliteManagementUtils {
    internal fun joinTwoTablesOnClause(columns: List<String>, leftName: String, rightName: String): String {
        val stringBuilder = StringBuilder()
        val strRightName = if (rightName.isBlank()) "" else "${rightName}."
        val strLeftName = if (leftName.isBlank()) "" else "${leftName}."

        var str = ""
        for (column in columns) {
            val quotedColumn = ParserName.parse(column).quoted().toString()

            stringBuilder.append(str)
            stringBuilder.append(strLeftName)
            stringBuilder.append(quotedColumn)
            stringBuilder.append(" = ")
            stringBuilder.append(strRightName)
            stringBuilder.append(quotedColumn)

            str = " AND "
        }
        return stringBuilder.toString()
    }

    internal fun whereColumnAndParameters(columns: List<String>, fromPrefix: String): String {
        val stringBuilder = StringBuilder()
        val strFromPrefix = if (fromPrefix.isBlank()) "" else "${fromPrefix}."
        var str1 = ""
        for (column in columns) {
            val quotedColumn = ParserName.parse(column).quoted().toString()
            val unquotedColumn = ParserName.parse(column).unquoted().normalized().toString()

            stringBuilder.append(str1)
            stringBuilder.append(strFromPrefix)
            stringBuilder.append(quotedColumn)
            stringBuilder.append(" = ")
            stringBuilder.append("@${unquotedColumn}")
            str1 = " AND "
        }
        return stringBuilder.toString()
    }

    internal fun joinOneTablesOnParametersValues(columns: List<String>, leftName: String): String {
        val stringBuilder = StringBuilder()
        val strLeftName = if (leftName.isBlank()) "" else "$leftName."

        var str = ""
        for (column in columns) {
            val quotedColumn = ParserName.parse(column).quoted().toString()
            val unquotedColumn = ParserName.parse(column).unquoted().normalized().toString()


            stringBuilder.append(str)
            stringBuilder.append(strLeftName)
            stringBuilder.append(quotedColumn)
            stringBuilder.append(" = ")
            stringBuilder.append("@${unquotedColumn}")

            str = " AND "
        }
        return stringBuilder.toString()
    }

    internal fun whereColumnIsNull(columns: List<String>, fromPrefix: String): String {
        val stringBuilder = StringBuilder()
        val strFromPrefix = if (fromPrefix.isBlank()) "" else "${fromPrefix}."
        var str1 = ""
        for (column in columns) {
            val quotedColumn = ParserName.parse(column).quoted().toString()

            stringBuilder.append(str1)
            stringBuilder.append(strFromPrefix)
            stringBuilder.append(quotedColumn)
            stringBuilder.append(" IS NULL ")
            str1 = " AND "
        }
        return stringBuilder.toString()
    }
}
