import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.ebpf_network_bp.MergedLogEntry
import com.example.ebpf_network_bp.Task


class DatabaseManager private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // 创建 Tasks 表
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS Tasks (
                id INTEGER PRIMARY KEY,
                status TEXT,
                name TEXT
            );
        """)

        // 创建 LogEntries 表
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS LogEntries (
                task_id INTEGER ,
                cursor INTEGER ,
                timestamp TEXT,
                hDest TEXT,
                hSource TEXT,
                hProto TEXT,
                version INTEGER,
                ihl INTEGER,
                totLen INTEGER,
                id INTEGER,
                df INTEGER,
                mf INTEGER,
                fragOff INTEGER,
                ttl INTEGER,
                protocol INTEGER,
                sAddr TEXT,
                dAddr TEXT,
                eventId INTEGER,
                eventFragIndex INTEGER,
                payload BLOB,
                PRIMARY KEY (task_id, cursor)
            );
        """)

        // 创建规则表 Rules，其中 rule_id 设为自增长
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS Rules (
            rule_id INTEGER PRIMARY KEY AUTOINCREMENT,
            object_field TEXT,
            regex TEXT
         );
        """)


        // 创建命中表 Hits
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS Hits (
            task_id INTEGER,
            cursor INTEGER,
            rule_id INTEGER,
            PRIMARY KEY (task_id, cursor, rule_id),
            FOREIGN KEY (task_id, cursor) REFERENCES LogEntries (task_id, cursor) ON DELETE CASCADE,
            FOREIGN KEY (rule_id) REFERENCES Rules (rule_id) ON DELETE CASCADE
        );
    """);

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 这里处理数据库升级逻辑，比如调整表结构等
        // 例如：如果需要升级表结构，可以添加 db.execSQL("DROP TABLE IF EXISTS ...");
        // 然后调用 onCreate(db) 重新创建表
    }

    companion object {
        private const val DATABASE_NAME = "AppDatabase.db"
        private const val DATABASE_VERSION = 1
        @Volatile private var instance: DatabaseManager? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(DatabaseManager::class) {
                    if (instance == null) {
                        instance = DatabaseManager(context)
                    }
                }
            }
        }

        fun getInstance(): DatabaseManager {
            return instance ?: throw IllegalStateException("DatabaseManager is not initialized, call initialize(Context) first")
        }
    }

    fun insertTask(task: com.example.ebpf_network_bp.Task) {
        val db = writableDatabase
        db.execSQL("INSERT OR REPLACE INTO Tasks (id, status, name) VALUES (?, ?, ?)", arrayOf(task.id, task.status, task.name))
        db.close()
    }

    fun queryTasks(): List<Task> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, status, name FROM Tasks", null)
        val tasks = mutableListOf<Task>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            tasks.add(Task(id, status, name))
        }
        cursor.close()
        db.close()
        return tasks
    }

    fun getDatabasePath(context: Context): String {
        return context.getDatabasePath(DATABASE_NAME).absolutePath
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun insertLogEntries(logEntries: List<MergedLogEntry>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement("""
        INSERT INTO LogEntries (task_id, cursor, timestamp, hDest, hSource, hProto, version, ihl, totLen, id, df, mf, fragOff, ttl, protocol, sAddr, dAddr, eventId, eventFragIndex, payload) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)
            for (entry in logEntries) {
                stmt.bindLong(1, entry.task_id)
                stmt.bindLong(2, entry.cursor)
                stmt.bindLong(3, entry.timestamp)
                stmt.bindString(4, entry.hDest)
                stmt.bindString(5, entry.hSource)
                stmt.bindString(6, entry.hProto)
                stmt.bindLong(7, entry.version.toLong())
                stmt.bindLong(8, entry.ihl.toLong())
                stmt.bindLong(9, entry.totLen.toLong())
                stmt.bindLong(10, entry.id.toLong())
                stmt.bindLong(11, entry.df.toLong())
                stmt.bindLong(12, entry.mf.toLong())
                stmt.bindLong(13, entry.fragOff.toLong())
                stmt.bindLong(14, entry.ttl.toLong())
                stmt.bindLong(15, entry.protocol.toLong())
                stmt.bindString(16, entry.sAddr)
                stmt.bindString(17, entry.dAddr)
                stmt.bindLong(18, entry.eventId.toLong())
                stmt.bindLong(19, entry.eventFragIndex.toLong())
                stmt.bindBlob(20, entry.payload.toByteArray())  // Convert UByteArray to ByteArray
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        }
        catch (e: Exception) {
            // 处理异常，例如打印日志或显示弹窗提示用户
            println("Error inserting log entries: ${e.localizedMessage}")
        }
        finally {
            db.endTransaction()
            db.close()
        }
    }


    @OptIn(ExperimentalUnsignedTypes::class)
    fun queryLogEntries(startCursor: Long, limit: Int): List<MergedLogEntry> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM LogEntries WHERE cursor >= ? ORDER BY cursor ASC LIMIT ?", arrayOf(startCursor.toString(), limit.toString()))
        val logEntries = mutableListOf<MergedLogEntry>()
        while (cursor.moveToNext()) {
            logEntries.add(extractLogEntry(cursor))
        }
        cursor.close()
        db.close()
        return logEntries
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private  fun extractLogEntry(cursor: Cursor): MergedLogEntry {
        return MergedLogEntry(
            task_id = cursor.getLong(cursor.getColumnIndexOrThrow("task_id")),
            cursor = cursor.getLong(cursor.getColumnIndexOrThrow("cursor")),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
            hDest = cursor.getString(cursor.getColumnIndexOrThrow("hDest")),
            hSource = cursor.getString(cursor.getColumnIndexOrThrow("hSource")),
            hProto = cursor.getString(cursor.getColumnIndexOrThrow("hProto")),
            version = cursor.getShort(cursor.getColumnIndexOrThrow("version")).toUByte(),
            ihl = cursor.getShort(cursor.getColumnIndexOrThrow("ihl")).toUByte(),
            totLen = cursor.getInt(cursor.getColumnIndexOrThrow("totLen")).toUShort(),
            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")).toUShort(),
            df = cursor.getShort(cursor.getColumnIndexOrThrow("df")).toUByte(),
            mf = cursor.getShort(cursor.getColumnIndexOrThrow("mf")).toUByte(),
            fragOff = cursor.getInt(cursor.getColumnIndexOrThrow("fragOff")).toUShort(),
            ttl = cursor.getShort(cursor.getColumnIndexOrThrow("ttl")).toUByte(),
            protocol = cursor.getShort(cursor.getColumnIndexOrThrow("protocol")).toUByte(),
            sAddr = cursor.getString(cursor.getColumnIndexOrThrow("sAddr")),
            dAddr = cursor.getString(cursor.getColumnIndexOrThrow("dAddr")),
            eventId = cursor.getLong(cursor.getColumnIndexOrThrow("eventId")).toULong(),
            eventFragIndex = cursor.getInt(cursor.getColumnIndexOrThrow("eventFragIndex")).toUShort(),
            payload = cursor.getBlob(cursor.getColumnIndexOrThrow("payload")).toUByteArray()
        )
    }


    @OptIn(ExperimentalUnsignedTypes::class)
    fun queryLogEntriesByTaskIdIdAndEventId(taskId: Long, id: Long, eventId: Long): List<MergedLogEntry> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM LogEntries WHERE task_id = ? AND id = ? AND eventId = ?", arrayOf(taskId.toString(), id.toString(), eventId.toString()))
        val logEntries = mutableListOf<MergedLogEntry>()
        while (cursor.moveToNext()) {
            logEntries.add(extractLogEntry(cursor))
        }
        cursor.close()
        db.close()
        return logEntries
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun deleteLogEntry(taskId: Long, id: Long, eventId: ULong) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 准备删除语句
            val deleteStmt = "DELETE FROM LogEntries WHERE task_id = ? AND id = ? AND eventId = ?"
            val stmt = db.compileStatement(deleteStmt)
            // 绑定参数以防止SQL注入
            stmt.bindLong(1, taskId)
            stmt.bindLong(2, id)
            stmt.bindLong(3, eventId.toLong())

            // 执行删除操作
            stmt.executeUpdateDelete()
            db.setTransactionSuccessful()  // 标记事务成功，将其提交
        } catch (e: Exception) {
            // 可以处理异常，例如打印日志或向用户反馈错误信息
            println("Error deleting entry: ${e.localizedMessage}")
        } finally {
            // 结束事务并关闭数据库连接
            db.endTransaction()
            db.close()
        }
    }


    @OptIn(ExperimentalUnsignedTypes::class)
    fun LogprocessEntries(taskId: Long, id: Long, eventId: Long): Boolean {
        if (id == 0L) {
            return false
        }

        val entries = queryLogEntriesByTaskIdIdAndEventId(taskId, id, eventId)
        if (entries.isEmpty()) {
            return true
        }

        // 按 eventFragIndex 排序
        val sortedEntries = entries.sortedBy { it.eventFragIndex }

        // 拼接 payload
        val concatenatedPayload = sortedEntries.flatMap { it.payload.toList() }.toUByteArray()

        // 取得第一个条目以获得其他共有字段
        val firstEntry = sortedEntries.first()

        // 创建新的 MergedLogEntry 对象
        val newEntry = firstEntry.copy(
            payload = concatenatedPayload,
            eventId = firstEntry.eventId,
            eventFragIndex = firstEntry.eventFragIndex
        )

        // 如果拼接后的payload长度和totLen差值小于100，则设置eventId和eventFragIndex为0
        if (Math.abs(concatenatedPayload.size - firstEntry.totLen.toInt()) < 100) {
            newEntry.eventId = 0UL
            newEntry.eventFragIndex = 0u
        }

        // 删除原有条目
        sortedEntries.forEach { entry ->
            deleteLogEntry(taskId, id, entry.eventId)
        }

        // 插入新的 MergedLogEntry
        insertLogEntries(listOf(newEntry))

        return true
    }

    fun getAllTaskIdsFromLogEntries(): List<Long> {
        val taskIds = mutableListOf<Long>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT task_id FROM LogEntries", null)
        while (cursor.moveToNext()) {
            val taskId = cursor.getLong(cursor.getColumnIndexOrThrow("task_id"))
            taskIds.add(taskId)
        }
        cursor.close()
        db.close()
        return taskIds
    }

    fun countLogEntriesBetween(startTime: Long, endTime: Long): Int {
        val db = readableDatabase
        val query = "SELECT COUNT(*) FROM LogEntries WHERE timestamp BETWEEN ? AND ?"
        val cursor = db.rawQuery(query, arrayOf(startTime.toString(), endTime.toString()))
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        db.close()
        return count
    }

    fun findNonZeroEventIds(): List<Pair<Long, Long>> {
        val db = readableDatabase
        val resultList = mutableListOf<Pair<Long, Long>>()
        // 查询 LogEntries 表中 eventId 不为 0 的所有唯一条目
        val cursor = db.rawQuery("SELECT DISTINCT id, eventId FROM LogEntries WHERE eventId != 0", null)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
            val eventId = cursor.getLong(cursor.getColumnIndexOrThrow("eventId"))
            // 将 id 和 eventId 添加到结果列表中
            resultList.add(Pair(id, eventId))
        }
        cursor.close()
        db.close()
        return resultList
    }

    fun readLogEntriesDescendingOrder(): List<MergedLogEntry> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM LogEntries ORDER BY timestamp DESC LIMIT 100", null)
        val logEntries = mutableListOf<MergedLogEntry>()
        while (cursor.moveToNext()) {
            logEntries.add(extractLogEntry(cursor))
        }
        cursor.close()
        db.close()
        return logEntries
    }

    fun readAllLogEntriesDescendingOrder(): List<MergedLogEntry> {
        val db = readableDatabase
        // 移除 LIMIT 子句以返回所有条目
        val cursor = db.rawQuery("SELECT * FROM LogEntries ORDER BY timestamp DESC", null)
        val logEntries = mutableListOf<MergedLogEntry>()
        while (cursor.moveToNext()) {
            logEntries.add(extractLogEntry(cursor))
        }
        cursor.close()
        db.close()
        return logEntries
    }

    fun getAllDistinctSAddr(): List<String> {
        val db = readableDatabase
        val sAddrs = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT DISTINCT sAddr FROM LogEntries", null)
        while (cursor.moveToNext()) {
            sAddrs.add(cursor.getString(cursor.getColumnIndexOrThrow("sAddr")))
        }
        cursor.close()
        db.close()
        return sAddrs
    }

    // 获取所有不重复的 dAddr
    fun getAllDistinctDAddr(): List<String> {
        val db = readableDatabase
        val dAddrs = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT DISTINCT dAddr FROM LogEntries", null)
        while (cursor.moveToNext()) {
            dAddrs.add(cursor.getString(cursor.getColumnIndexOrThrow("dAddr")))
        }
        cursor.close()
        db.close()
        return dAddrs
    }

    // 获取所有不重复的 hDest
    fun getAllDistinctHDest(): List<String> {
        val db = readableDatabase
        val hDests = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT DISTINCT hDest FROM LogEntries", null)
        while (cursor.moveToNext()) {
            hDests.add(cursor.getString(cursor.getColumnIndexOrThrow("hDest")))
        }
        cursor.close()
        db.close()
        return hDests
    }

    // 获取所有不重复的 hSource
    fun getAllDistinctHSource(): List<String> {
        val db = readableDatabase
        val hSources = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT DISTINCT hSource FROM LogEntries", null)
        while (cursor.moveToNext()) {
            hSources.add(cursor.getString(cursor.getColumnIndexOrThrow("hSource")))
        }
        cursor.close()
        db.close()
        return hSources
    }

    fun getMaxTimestamp(): Long {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT MAX(timestamp) FROM LogEntries", null)
        val maxTimestamp: Long = if (cursor.moveToFirst()) {
            cursor.getLong(0)
        } else {
            // 如果没有记录，则返回一个默认值，例如 -1
            -1
        }
        cursor.close()
        db.close()
        return maxTimestamp
    }


    class Rule(val ruleId: Int? = null, val objectField: String, val regex: String)

    class Hit(val taskId: Int, val cursor: Int, val ruleId: Int)

    fun insertRule(rule: Rule): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            // 移除了 rule.ruleId 的插入，因为它是自增字段
            put("object_field", rule.objectField)
            put("regex", rule.regex)
        }
        return try {
            db.insertOrThrow("Rules", null, values)
            db.close()
            true
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error inserting rule: ${e.message}")
            db.close()
            false
        }
    }


    fun deleteRule(ruleId: Int): Boolean {
        val db = writableDatabase
        return try {
            // db.delete returns the number of rows affected by the delete operation
            val rowsDeleted = db.delete("Rules", "rule_id = ?", arrayOf(ruleId.toString()))
            rowsDeleted > 0  // Return true if one or more rows were deleted
        } finally {
            db.close()
        }
    }


    @SuppressLint("Range")
    fun getAllRules(): List<Rule> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Rules", null)
        val rules = mutableListOf<Rule>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("rule_id"))
            val objectField = cursor.getString(cursor.getColumnIndex("object_field"))
            val regex = cursor.getString(cursor.getColumnIndex("regex"))
            rules.add(Rule(id, objectField, regex))
        }
        cursor.close()
        db.close()
        return rules
    }

    fun insertHit(hit: Hit): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("task_id", hit.taskId)
            put("cursor", hit.cursor)
            put("rule_id", hit.ruleId)
        }
        return try {
            db.insertOrThrow("Hits", null, values)
            db.close()
            true
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error inserting hit: ${e.message}")
            db.close()
            false
        }
    }

    @SuppressLint("Range")
    fun getLastNHits(n: Int): List<Hit> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Hits ORDER BY task_id DESC, cursor DESC LIMIT $n", null)
        val hits = mutableListOf<Hit>()
        while (cursor.moveToNext()) {
            val taskId = cursor.getInt(cursor.getColumnIndex("task_id"))
            val cursorIndex = cursor.getInt(cursor.getColumnIndex("cursor"))
            val ruleId = cursor.getInt(cursor.getColumnIndex("rule_id"))
            hits.add(Hit(taskId, cursorIndex, ruleId))
        }
        cursor.close()
        db.close()
        return hits
    }

    fun getMergedLogEntry(taskId: Long, cursor: Long): MergedLogEntry? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM LogEntries WHERE task_id = ? AND cursor = ?",
            arrayOf(taskId.toString(), cursor.toString())
        )
        var mergedLogEntry: MergedLogEntry? = null
        if (cursor.moveToFirst()) {
            mergedLogEntry = extractLogEntry(cursor)
        }
        cursor.close()
        db.close()
        return mergedLogEntry
    }


}

