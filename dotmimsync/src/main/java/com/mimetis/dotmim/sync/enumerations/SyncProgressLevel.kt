package com.mimetis.dotmim.sync.enumerations

import com.mimetis.dotmim.sync.data.EnumByValueSerializer
import com.mimetis.dotmim.sync.data.EnumWithValue
import kotlinx.serialization.Serializable

/**
 * Defines logging severity levels.
 */
@Serializable(with = SyncProgressLevelSerializer::class)
enum class SyncProgressLevel(override val value: Int):EnumWithValue {
    /**
     * Progress that contain the most detailed messages and the Sql statement executed
     * These messages may contain sensitive
     * application data. These messages are disabled by default and should never be
     * enabled in a production environment.
     */
    Sql(0),

    /**
     * Progress that contain the most detailed messages. These messages may contain sensitive
     * application data. These messages are disabled by default and should never be
     * enabled in a production environment.
     */
    Trace(1),

    /**
     * Progress that are used for interactive investigation during development. These logs
     * should primarily contain information useful for debugging and have no long-term
     * value.
     */
    Debug(2),

    /**
     * Progress that track the general flow of the application. These logs should have long-term
     * value.
     */
    Information(3),

    /**
     * Progress that highlight an abnormal or unexpected event in the application flow,
     * but do not otherwise cause the application execution to stop.
     */
    Warning(4),

    /**
     * Progress that highlight when the current flow of execution is stopped due to a failure.
     * These should indicate a failure in the current activity, not an application-wide
     * failure.
     */
    Error(5),

    /**
     * Not used for writing progress messages. Specifies that a logging category should not
     * write any messages.
     */
    None(6)
}

class SyncProgressLevelSerializer : EnumByValueSerializer<SyncStage>()
