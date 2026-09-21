package com.fpa.dangjiandaping.data.learning

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/** One successfully written camera image belonging to a class. */
@Entity(
    tableName = "learning_snapshots",
    indices = [Index(value = ["class_id", "captured_at_millis"])],
)
data class LearningSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "captured_at_millis") val capturedAtMillis: Long,
)

@Dao
interface LearningSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(snapshot: LearningSnapshotEntity)

    @Query(
        "SELECT * FROM learning_snapshots " +
            "WHERE class_id = :classId ORDER BY captured_at_millis DESC, id DESC",
    )
    suspend fun findByClassId(classId: String): List<LearningSnapshotEntity>
}

@Database(entities = [LearningSnapshotEntity::class], version = 2, exportSchema = false)
abstract class LearningSnapshotDatabase : RoomDatabase() {
    abstract fun snapshotDao(): LearningSnapshotDao
}

/**
 * Process-wide repository. Database writes use an application-owned scope so a photo saved just
 * before the player closes is not cancelled with the activity lifecycle.
 */
class LearningSnapshotRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        LearningSnapshotDatabase::class.java,
        DATABASE_NAME,
    ).addMigrations(MIGRATION_1_2).build()
    // Serializing writes and reads makes a just-saved photo visible to a subsequent H5 list
    // request, even when the player closes immediately after the capture callback.
    private val databaseDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LearningSnapshotDb").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val applicationScope = CoroutineScope(SupervisorJob() + databaseDispatcher)

    fun recordSnapshot(classId: String, imagePath: String, capturedAtMillis: Long) {
        applicationScope.launch {
            database.snapshotDao().insert(
                LearningSnapshotEntity(
                    classId = classId.ifBlank { UNKNOWN_CLASS_ID },
                    imagePath = imagePath,
                    capturedAtMillis = capturedAtMillis,
                ),
            )
        }
    }

    /**
     * The JavaScript bridge has a synchronous return contract. It invokes this from WebView's
     * bridge thread, while Room work stays on the repository's serial database dispatcher.
     */
    fun snapshotsJsonForClass(classId: String): String =
        runCatching {
            runBlocking(databaseDispatcher) {
                JSONArray().apply {
                    database.snapshotDao().findByClassId(classId.ifBlank { UNKNOWN_CLASS_ID })
                        .forEach { snapshot ->
                            val photoFile = File(snapshot.imagePath)
                            if (!photoFile.isFile) return@forEach
                            val imageUri = runCatching {
                                FileProvider.getUriForFile(
                                    appContext,
                                    "${appContext.packageName}$FILE_PROVIDER_AUTHORITY_SUFFIX",
                                    photoFile,
                                )
                            }.getOrNull() ?: return@forEach
                            put(
                                JSONObject()
                                    .put("id", snapshot.id)
                                    .put("classId", snapshot.classId)
                                    .put("imageUri", imageUri.toString())
                                    .put("capturedAtMillis", snapshot.capturedAtMillis),
                            )
                        }
                }.toString()
            }
        }.getOrElse {
            Log.d("database", "snapshotsJsonForClass: 失败了")
            "[]" }

    companion object {
        private const val DATABASE_NAME = "learning_snapshot.db"
        private const val UNKNOWN_CLASS_ID = "unknown"
        private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".learning-snapshot-files"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `learning_snapshots_new` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`class_id` TEXT NOT NULL, `image_path` TEXT NOT NULL, " +
                        "`captured_at_millis` INTEGER NOT NULL)",
                )
                database.execSQL(
                    "INSERT INTO `learning_snapshots_new` " +
                        "(`id`, `class_id`, `image_path`, `captured_at_millis`) " +
                        "SELECT `id`, `course_id`, `image_path`, `captured_at_millis` " +
                        "FROM `learning_snapshots`",
                )
                database.execSQL("DROP TABLE `learning_snapshots`")
                database.execSQL(
                    "ALTER TABLE `learning_snapshots_new` RENAME TO `learning_snapshots`",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_learning_snapshots_class_id_captured_at_millis` " +
                        "ON `learning_snapshots` (`class_id`, `captured_at_millis`)",
                )
            }
        }

        @Volatile
        private var instance: LearningSnapshotRepository? = null

        fun get(context: Context): LearningSnapshotRepository =
            instance ?: synchronized(this) {
                instance ?: LearningSnapshotRepository(context.applicationContext).also {
                    instance = it
                }
            }
    }
}
