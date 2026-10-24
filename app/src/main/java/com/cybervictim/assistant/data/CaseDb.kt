package com.cybervictim.assistant.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Entity(tableName = "evidence")
data class EvidenceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: String,
    val displayName: String,
    val uri: String,
    val kind: String, // screenshot | sms | statement | other
    val extractedFactIds: String, // JSON array of fact ids linked to this item
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "facts")
data class CaseFact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: String,
    val type: String, // upi_id | phone | txn_ref | timestamp | amount | other
    @androidx.room.ColumnInfo(name = "value") val valueText: String,
    val sourceEvidenceId: Long,
    val confidence: Float = 1f,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "events")
data class CaseEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: String,
    val kind: String, // created | portal_step | submitted | reminder ...
    val detail: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface CaseDao {
    @Insert(onConflict = REPLACE) suspend fun addEvidence(e: EvidenceItem): Long
    @Query("SELECT * FROM evidence WHERE caseId = :caseId ORDER BY createdAt DESC")
    fun evidenceOf(caseId: String): Flow<List<EvidenceItem>>

    @Insert(onConflict = REPLACE) suspend fun addFact(f: CaseFact)
    @Query("SELECT * FROM facts WHERE caseId = :caseId ORDER BY id")
    fun factsOf(caseId: String): Flow<List<CaseFact>>

    @Insert(onConflict = REPLACE) suspend fun addEvent(e: CaseEvent)
    @Query("SELECT * FROM events WHERE caseId = :caseId ORDER BY createdAt DESC")
    fun eventsOf(caseId: String): Flow<List<CaseEvent>>

    @Query("DELETE FROM evidence WHERE id = :id") suspend fun removeEvidence(id: Long)
}

@Database(entities = [EvidenceItem::class, CaseFact::class, CaseEvent::class], version = 1, exportSchema = false)
abstract class CaseDb : RoomDatabase() {
    abstract fun dao(): CaseDao

    companion object {
        @Volatile private var instance: CaseDb? = null
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun get(ctx: Context): CaseDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(ctx, CaseDb::class.java, "case_store.db")
                    .build().also { instance = it }
            }
    }
}
