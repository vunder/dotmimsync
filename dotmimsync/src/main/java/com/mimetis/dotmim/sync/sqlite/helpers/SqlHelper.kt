package com.mimetis.dotmim.sync.sqlite.helpers

/**
 * Process sql-query with named parameters to a query without named parameters to run it over
 * SQLiteDatabase that does not support named parameters
 * @param query Input sql-query
 * @param parameters List of parameter names
 * @return Transformed sql-query, parameters order to match values with unnamed parameters in the
 * query
 */
fun String.processSql(parameters: List<String>): Pair<String, List<String>> {
    val bindOrder = mutableListOf<String>()
    var sql = this
    do {
        val startIndex = sql.indexOf("@")
        if (startIndex >= 0) {
            var index = startIndex + 1
            while (index < sql.length && (sql[index].isLetterOrDigit() || sql[index] == '_'))
                ++index
            index = kotlin.math.min(index, sql.length)
            val name = sql.substring(startIndex, index)
            bindOrder.add(name)
//                    sql =
//                        "${
//                            sql.substring(
//                                0,
//                                startIndex
//                            )
//                        }${value.asSqlValue()}${sql.substring(index)}"
            sql = "${sql.substring(0, startIndex)}?${sql.substring(index)}"
        }
    } while (startIndex >= 0)

    return Pair(sql, bindOrder)
}