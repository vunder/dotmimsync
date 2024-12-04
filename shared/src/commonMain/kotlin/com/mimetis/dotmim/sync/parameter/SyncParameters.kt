package com.mimetis.dotmim.sync.parameter

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

@Serializable(with = SyncParametersSerializer::class)
class SyncParameters : ArrayList<SyncParameter>()

object SyncParametersSerializer : ArrayListLikeSerializer<SyncParameters, SyncParameter>(SyncParameter.serializer())
