package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

@Serializable(with = SetupFiltersSerializer::class)
class SetupFilters : ArrayList<SetupFilter>()

object SetupFiltersSerializer : ArrayListLikeSerializer<SetupFilters, SetupFilter>(SetupFilter.serializer())
