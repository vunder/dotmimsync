package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

@Serializable(with = SetupColumnsSerializer::class)
class SetupColumns : ArrayList<String>()

object SetupColumnsSerializer : ArrayListLikeSerializer<SetupColumns, String>(String.serializer())
