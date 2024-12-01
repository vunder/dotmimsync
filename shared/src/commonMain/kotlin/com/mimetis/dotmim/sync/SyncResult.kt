package com.mimetis.dotmim.sync

import com.benasher44.uuid.Uuid
import com.mimetis.dotmim.sync.messages.DatabaseChangesApplied
import com.mimetis.dotmim.sync.messages.DatabaseChangesSelected
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.math.max

/**
 * Context of the current Sync session
 * Encapsulates data changes and metadata for a synchronization session.
 */
@Serializable
class SyncResult(
    /**
     * Current Session, in progress
     */
    @Serializable(with = UUIDSerializer::class)
    var sessionId: Uuid,

    /**
     * Gets or sets the time when a sync sessionn started.
     */
    var startTime: Long,

    /**
     * Gets or sets the time when a sync session ended.
     */
    var completeTime: Long,

    /**
     * Gets or Sets the ScopeName for this sync session
     */
    var scopeName: String = "",

    /**
     * Gets the number of sync errors
     */
    var totalSyncErrors: Int = 0,

    ) {
    /**
     * Gets or Sets the summary of client changes that where applied on the server
     */
    var changesAppliedOnServer: DatabaseChangesApplied? = null

    /**
     * Gets or Sets the summary of server changes that where applied on the client
     */
    var changesAppliedOnClient: DatabaseChangesApplied? = null

    /**
     * Gets or Sets the summary of server changes that where applied on the client
     */
    var snapshotChangesAppliedOnClient: DatabaseChangesApplied? = null

    /**
     * Gets or Sets the summary of client changes to be applied on the server
     */
    var clientChangesSelected: DatabaseChangesSelected? = null

    /**
     * Gets or Sets the summary of server changes selected to be applied on the client
     */
    var serverChangesSelected: DatabaseChangesSelected? = null

    /**
     * Gets the number of changes applied on the client
     */
    val totalChangesApplied: Int
        get() = (this.changesAppliedOnClient?.totalAppliedChanges
            ?: 0) + (this.snapshotChangesAppliedOnClient?.totalAppliedChanges
            ?: 0)

    /**
     * Gets total number of changes downloaded from server.
     */
    val totalChangesDownloaded: Int
        get() = (this.serverChangesSelected?.totalChangesSelected
            ?: 0) + (this.snapshotChangesAppliedOnClient?.totalAppliedChanges
            ?: 0)

    /**
     * Gets the number of change uploaded to the server
     */
    val totalChangesUploaded: Int
        get() = this.clientChangesSelected?.totalChangesSelected ?: 0

    /**
     * Gets the number of conflicts resolved
     */
    val totalResolvedConflicts: Int
        get() = max(
            this.changesAppliedOnClient?.totalResolvedConflicts ?: 0,
            this.changesAppliedOnServer?.totalResolvedConflicts ?: 0
        )

    private val durationString: String
        get() = getVConfDuration(startTime, completeTime)

    val hasChanges: Boolean
        get() = this.totalChangesUploaded > 0 ||
                this.totalChangesDownloaded > 0 ||
                this.totalChangesApplied > 0 ||
                this.totalResolvedConflicts > 0

    /**
     * Get the result if sync session is ended
     */
    override fun toString(): String {
        if (this.completeTime != this.startTime && this.completeTime > this.startTime) {
//            var tsEnded = completeTime.toDuration(DurationUnit.MILLISECONDS)
//            var tsStarted = startTime.toDuration(DurationUnit.MILLISECONDS)
//
//            val durationTs = tsEnded.minus(tsStarted)
//            val durationstr = "${durationTs.hours}:${durationTs.Minutes}:${durationTs.Seconds}.${durationTs.Milliseconds}"

            return "Synchronization done\n" +
                    "\tTotal changes  uploaded: $totalChangesUploaded\n" +
                    "\tTotal changes  downloaded: $totalChangesDownloaded\n" +
                    "\tTotal changes  applied: $totalChangesApplied\n" +
                    "\tTotal resolved conflicts: $totalResolvedConflicts\n" +
                    "\tTotal duration :$durationString "

        }

        return super.toString()
    }

    companion object {
        fun getVConfDuration(startTime: Long, endTime: Long): String {
            val duration = endTime - startTime
            if (duration <= 0) {
                return "00:00:00"
            } /*from ww w.j a v  a2 s  .  co  m*/
            val h = duration / 60 / 60 / 1000
            val moH = duration % (60 * 60 * 1000)
            val m = moH / 60 / 1000
            val moM = moH % (60 * 1000)
            val s = moM / 1000
            val sb = StringBuffer()
            if (h < 10) {
                sb.append(0)
            }
            sb.append(h)
            sb.append(":")
            if (m < 10) {
                sb.append(0)
            }
            sb.append(m)
            sb.append(":")
            if (s < 10) {
                sb.append(0)
            }
            sb.append(s)
            return sb.toString()
        }
    }
}
