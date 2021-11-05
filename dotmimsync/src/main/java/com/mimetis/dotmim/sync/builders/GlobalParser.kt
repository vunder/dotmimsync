package com.mimetis.dotmim.sync.builders

object GlobalParser {
    private val parsers: Map<String, Lazy<ParserString>> = HashMap()

    fun getParserString(key: String): ParserString {
        // Try to get the instance
        val parserStringRetrieved = parsers.getOrElse(key) { lazy { internalParse(key) } }

        return parserStringRetrieved.value
    }

    private fun internalParse(key: String): ParserString {
        val t = key.split('^')

        var leftQuote = ""
        var rightQuote = ""
        val input: String

        when (t.size) {
            1 ->
                input = t[0]
            2 -> {
                leftQuote = t[0]
                rightQuote = t[0]
                input = t[1]
            }
            3 -> {
                leftQuote = t[0]
                rightQuote = t[1]
                input = t[2]
            }
            else ->
                throw Exception("Length of Parser key splitted with ^ is invalid (input=$key, Length=${t.size}")
        }

        val parserString = ParserString()
        if (leftQuote.isNotBlank()) {
            parserString.quotePrefix = leftQuote
            parserString.quoteSuffix = leftQuote
        }
        if (rightQuote.isNotBlank())
            parserString.quoteSuffix = rightQuote

        val regexExpression = "(?<quoted>\\w[^\\" + parserString.quotePrefix + "\\" + parserString.quoteSuffix + "\\.]*)"
        val regex = Regex(regexExpression, RegexOption.IGNORE_CASE)

        val matchCollections = regex.findAll(input)
        val strMatches = Array(3) { "" }
        var matchCounts = 0
        for (match in matchCollections) {
            if (matchCounts >= 3)
                break

            if (match.value.isBlank())
                continue

            //val quotedGroup = match.groups["quoted"]
            // Named groups are not supported, taking first item
            val quotedGroup = match.groups[0]
            if (quotedGroup == null || quotedGroup.value.isBlank())
                continue

            strMatches[matchCounts++] = quotedGroup.value.trim()
        }
        when (matchCounts) {
            1 -> {
                parseObjectName(parserString, strMatches[0])
                return parserString
            }
            2 -> {
                parseschemaName(parserString, strMatches[0])
                parseObjectName(parserString, strMatches[1])
                return parserString
            }
            3 -> {
                parsedatabaseName(parserString, strMatches[0])
                parseschemaName(parserString, strMatches[1])
                parseObjectName(parserString, strMatches[2])
                return parserString
            }
            else -> return parserString
        }
    }

    private fun parseObjectName(parserString: ParserString, name: String) {
        parserString.objectName = name
        if (parserString.objectName.startsWith(parserString.quotePrefix))
            parserString.objectName = parserString.objectName.substring(1)
        if (parserString.objectName.endsWith(parserString.quoteSuffix))
            parserString.objectName = parserString.objectName.substring(0, parserString.objectName.length - 1)
        parserString.quotedObjectName = parserString.quotePrefix + parserString.objectName + parserString.quoteSuffix
    }

    private fun parseschemaName(parserString: ParserString, name: String) {
        parserString.schemaName = name
        if (parserString.schemaName.isNotBlank()) {
            if (parserString.schemaName.startsWith(parserString.quotePrefix))
                parserString.schemaName = parserString.schemaName.substring(1)
            if (parserString.schemaName.endsWith(parserString.quoteSuffix))
                parserString.schemaName = parserString.schemaName.substring(0, parserString.schemaName.length - 1)
            parserString.quotedSchemaName = parserString.quotePrefix + parserString.schemaName + parserString.quoteSuffix
        }
    }

    private fun parsedatabaseName(parserString: ParserString, name: String) {
        parserString.databaseName = name
        if (parserString.databaseName.startsWith(parserString.quotePrefix))
            parserString.databaseName = parserString.databaseName.substring(1)
        if (parserString.databaseName.endsWith(parserString.quoteSuffix))
            parserString.databaseName = parserString.databaseName.substring(0, parserString.databaseName.length - 1)
        parserString.quotedDatabaseName = parserString.quotePrefix + parserString.databaseName + parserString.quoteSuffix
    }
}
