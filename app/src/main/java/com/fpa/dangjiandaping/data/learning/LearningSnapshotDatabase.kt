package com.fpa.dangjiandaping.data.learning

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

/** One successfully written camera image belonging to a class. */
@Entity(
    tableName = "learning_snapshots",
    indices = [
        Index(value = ["class_id", "captured_at_millis"]),
        Index(value = ["resource_id", "captured_at_millis"]),
    ],
)
data class LearningSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "resource_id") val resourceId: String,
    @ColumnInfo(name = "resource_name") val resourceName: String,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "captured_at_millis") val capturedAtMillis: Long,
)

/** Aggregated local learning progress for one course resource inside one training class. */
@Entity(
    tableName = "learning_courses",
    primaryKeys = ["class_id", "resource_id"],
    indices = [Index(value = ["class_id", "last_studied_at_millis"])],
)
data class LearningCourseEntity(
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "resource_id") val resourceId: String,
    @ColumnInfo(name = "resource_name") val resourceName: String,
    @ColumnInfo(name = "resource_type") val resourceType: String,
    @ColumnInfo(name = "resource_type_name") val resourceTypeName: String,
    @ColumnInfo(name = "played_duration_ms") val playedDurationMs: Long,
    @ColumnInfo(name = "last_studied_at_millis") val lastStudiedAtMillis: Long,
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

    @Query(
        "SELECT * FROM learning_snapshots " +
            "WHERE resource_id = :resourceId ORDER BY captured_at_millis ASC, id ASC",
    )
    suspend fun findByResourceId(resourceId: String): List<LearningSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCourseIfAbsent(course: LearningCourseEntity): Long

    @Query(
        "UPDATE learning_courses SET resource_name = :resourceName, " +
            "resource_type = :resourceType, resource_type_name = :resourceTypeName, " +
            "played_duration_ms = played_duration_ms + :durationDeltaMs, " +
            "last_studied_at_millis = :studiedAtMillis " +
            "WHERE class_id = :classId AND resource_id = :resourceId",
    )
    suspend fun addCourseProgress(
        classId: String,
        resourceId: String,
        resourceName: String,
        resourceType: String,
        resourceTypeName: String,
        durationDeltaMs: Long,
        studiedAtMillis: Long,
    )

    @Query(
        "SELECT * FROM learning_courses WHERE class_id = :classId " +
            "ORDER BY last_studied_at_millis DESC, resource_id ASC",
    )
    suspend fun findCoursesByClassId(classId: String): List<LearningCourseEntity>
}

@Database(
    entities = [LearningSnapshotEntity::class, LearningCourseEntity::class],
    version = 4,
    exportSchema = false,
)
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
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    // Serializing writes and reads makes a just-saved photo visible to a subsequent H5 list
    // request, even when the player closes immediately after the capture callback.
    private val databaseDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LearningSnapshotDb").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val applicationScope = CoroutineScope(SupervisorJob() + databaseDispatcher)

    fun recordSnapshot(
        classId: String,
        resourceId: String,
        resourceName: String,
        imagePath: String,
        capturedAtMillis: Long,
    ) {
        applicationScope.launch {
            database.snapshotDao().insert(
                LearningSnapshotEntity(
                    classId = classId.ifBlank { UNKNOWN_CLASS_ID },
                    resourceId = resourceId,
                    resourceName = resourceName,
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
                            val imageUri = photoFile.toLearningSnapshotWebUrl() ?: return@forEach
                            put(
                                JSONObject()
                                    .put("id", snapshot.id)
                                    .put("classId", snapshot.classId)
                                    .put("resourceId", snapshot.resourceId)
                                    .put("resourceName", snapshot.resourceName)
                                    .put("imageUri", imageUri.toString())
                                    .put("capturedAtMillis", snapshot.capturedAtMillis),
                            )
                        }
                }.toString()
            }
        }.getOrElse {
            Log.e("database", "snapshotsJsonForClass failed", it)
            "[]" }

    fun snapshotsForResource(resourceId: String): List<LearningSnapshotEntity> =
        runCatching {
            runBlocking(databaseDispatcher) {
                database.snapshotDao().findByResourceId(resourceId)
                    .filter { File(it.imagePath).isFile }
            }
        }.getOrElse {
            Log.e("database", "snapshotsForResource failed, resourceId=$resourceId", it)
            emptyList()
        }

    fun recordCourseProgress(
        classId: String,
        resourceId: String,
        resourceName: String,
        resourceType: String,
        resourceTypeName: String,
        durationDeltaMs: Long,
        studiedAtMillis: Long = System.currentTimeMillis(),
    ) {
        if (classId.isBlank() || resourceId.isBlank()) return
        val safeDurationDeltaMs = durationDeltaMs.coerceAtLeast(0L)
        applicationScope.launch {
            val course = LearningCourseEntity(
                classId = classId,
                resourceId = resourceId,
                resourceName = resourceName,
                resourceType = resourceType,
                resourceTypeName = resourceTypeName,
                playedDurationMs = safeDurationDeltaMs,
                lastStudiedAtMillis = studiedAtMillis,
            )
            val insertedId = database.snapshotDao().insertCourseIfAbsent(course)
            if (insertedId == -1L) {
                database.snapshotDao().addCourseProgress(
                    classId = classId,
                    resourceId = resourceId,
                    resourceName = resourceName,
                    resourceType = resourceType,
                    resourceTypeName = resourceTypeName,
                    durationDeltaMs = safeDurationDeltaMs,
                    studiedAtMillis = studiedAtMillis,
                )
            }
        }
    }

    /** H5 passes only classId; native returns locally collected per-course learning summaries. */
    fun learningCoursesJsonForClass(classId: String): String =
        runCatching {
            runBlocking(databaseDispatcher) {
                val normalizedClassId = classId.ifBlank { UNKNOWN_CLASS_ID }
                val snapshotCountByResource = database.snapshotDao()
                    .findByClassId(normalizedClassId)
                    .asSequence()
                    .filter { it.resourceId.isNotBlank() && File(it.imagePath).isFile }
                    .groupingBy { it.resourceId }
                    .eachCount()
                JSONArray().apply {
                    database.snapshotDao().findCoursesByClassId(normalizedClassId)
                        .forEach { course ->
                            val snapshotCount = snapshotCountByResource[course.resourceId] ?: 0
                            put(
                                JSONObject()
                                    .put("id", course.resourceId)
                                    .put("classId", course.classId)
                                    .put("name", course.resourceName)
                                    .put("resourceType", course.resourceType)
                                    .put("resourceTypeName", course.resourceTypeName)
                                    .put("playedDurationMs", course.playedDurationMs)
                                    .put(
                                        "studyDurationText",
                                        formatStudyDurationHours(course.playedDurationMs),
                                    )
                                    .put("snapshotCount", snapshotCount)
                                    .put("hasEvidence", snapshotCount > 0)
                                    .put("lastStudiedAtMillis", course.lastStudiedAtMillis),
                            )
                        }
                }.toString()
            }
        }.getOrElse {
            Log.e("database", "learningCoursesJsonForClass failed, classId=$classId", it)
            "[]"
        }

    private fun File.toLearningSnapshotWebUrl(): String? {
        val root = File(appContext.cacheDir, LEARNING_SNAPSHOT_CACHE_DIRECTORY).canonicalFile
        val photo = canonicalFile
        val rootPrefix = root.path + File.separator
        if (!photo.path.startsWith(rootPrefix)) return null
        val relativeSegments = photo.relativeTo(root).invariantSeparatorsPath.split('/')
        return Uri.Builder()
            .scheme("https")
            .authority(LEARNING_SNAPSHOT_WEB_HOST)
            .appendPath(LEARNING_SNAPSHOT_WEB_ROUTE)
            .apply { relativeSegments.forEach { appendPath(it) } }
            .build()
            .toString()
    }

    companion object {
        private const val DATABASE_NAME = "learning_snapshot.db"
        private const val UNKNOWN_CLASS_ID = "unknown"
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
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `learning_snapshots` " +
                        "ADD COLUMN `resource_id` TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    "ALTER TABLE `learning_snapshots` " +
                        "ADD COLUMN `resource_name` TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_learning_snapshots_resource_id_captured_at_millis` " +
                        "ON `learning_snapshots` (`resource_id`, `captured_at_millis`)",
                )
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `learning_courses` (" +
                        "`class_id` TEXT NOT NULL, `resource_id` TEXT NOT NULL, " +
                        "`resource_name` TEXT NOT NULL, `resource_type` TEXT NOT NULL, " +
                        "`resource_type_name` TEXT NOT NULL, " +
                        "`played_duration_ms` INTEGER NOT NULL, " +
                        "`last_studied_at_millis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`class_id`, `resource_id`))",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_learning_courses_class_id_last_studied_at_millis` " +
                        "ON `learning_courses` (`class_id`, `last_studied_at_millis`)",
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

private fun formatStudyDurationHours(durationMs: Long): String =
    String.format(Locale.US, "%.1f小时", durationMs.coerceAtLeast(0L) / 3_600_000.0)

internal const val LEARNING_SNAPSHOT_CACHE_DIRECTORY = "learning_snapshots"
internal const val LEARNING_SNAPSHOT_WEB_HOST = "appassets.androidplatform.net"
internal const val LEARNING_SNAPSHOT_WEB_ROUTE = "learning-snapshots"
internal const val LEARNING_SNAPSHOT_WEB_PATH = "/$LEARNING_SNAPSHOT_WEB_ROUTE/"
