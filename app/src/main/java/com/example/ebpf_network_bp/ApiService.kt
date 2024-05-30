package com.example.ebpf_network_bp

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// eBPF Server API Client --------------------------------------------
object ApiService {
    private val client = OkHttpClient()
    private const val BASE_URL = "http://localhost:8527"

    // 获取任务列表
    fun getTasks(): String {
        val request = Request.Builder()
            .url("$BASE_URL/task")
            .build()
        return executeRequest(request)
    }

    // 获取日志
    fun getTaskLog(taskId: Long, logCursor: Long, maxCount: Long): String {
        val json = JSONObject().apply {
            put("id", taskId)
            put("log_cursor", logCursor)
            put("maximum_count", maxCount)
        }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/log")
            .post(body)
            .build()
        return executeRequest(request)
    }

    // 暂停任务
    fun pauseTask(taskId: Long): String {
        val json = JSONObject().put("id", taskId)
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/pause")
            .post(body)
            .build()
        return executeRequest(request)
    }

    // 恢复任务
    fun resumeTask(taskId: Long): String {
        val json = JSONObject().put("id", taskId)
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/resume")
            .post(body)
            .build()
        return executeRequest(request)
    }

    // 通用执行网络请求函数
    private fun executeRequest(request: Request): String {
        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                response.body?.string() ?: "No response body"
            } else {
                "Error: ${response.message}"
            }
        }
    }
}

// Task Analyse -----------------------------------------------------------
data class Task(
    val id: Int,
    val status: String,
    val name: String
)

fun parseTaskList(jsonData: String): List<Task> {
    val tasks = mutableListOf<Task>()
    val jsonObject = JSONObject(jsonData)
    val tasksArray = jsonObject.getJSONArray("tasks")
    for (i in 0 until tasksArray.length()) {
        val taskObject = tasksArray.getJSONObject(i)
        val status = taskObject.getString("status")
        val id = taskObject.getInt("id")
        val name = taskObject.getString("name")
        val task = Task( id, status, name)
        tasks.add(task)
    }
    return tasks
}

// Log Analyse -------------------------------------------------------------

@OptIn(ExperimentalUnsignedTypes::class)
data class MergedLogEntry(
    val task_id: Long,
    val cursor: Long,
    val timestamp: Long,
    val hDest: String,             // 目的MAC地址
    val hSource: String,           // 源MAC地址
    val hProto: String,            // 以太网帧中的协议字段（可读形式）
    val version: UByte,            // IP报文版本号
    val ihl: UByte,                // IP报文头长度
    val totLen: UShort,            // IP报文总长度
    val id: UShort,                // IP报文标识
    val df: UByte,                 // IP报文是否标记为不分片
    val mf: UByte,                 // IP报文是否标记为更多分片
    val fragOff: UShort,           // IP报文分片偏移量
    val ttl: UByte,                // IP报文生存时间
    val protocol: UByte,           // IP报文中的协议字段
    val sAddr: String,             // 源IP地址
    val dAddr: String,             // 目的IP地址
    var eventId: ULong,            // 事件ID
    var eventFragIndex: UShort,    // 事件分片索引
    val payload: UByteArray       // 负载数据
)


@RequiresApi(Build.VERSION_CODES.O)
fun parseLogEntries(taskId: Long, jsonData: String): List<MergedLogEntry> {
    val gson = Gson()
    val type = object : TypeToken<List<RawLogEntry>>() {}.type
    val rawEntries: List<RawLogEntry> = gson.fromJson(jsonData, type)
    return rawEntries.map { rawEntry ->
        parseSOEvent(rawEntry.log.log, rawEntry.cursor, taskId)
    }
}

data class RawLogEntry(
    val cursor: Long,
    val log: RawLogDetails
)

data class RawLogDetails(
    val log: String
)


@OptIn(ExperimentalUnsignedTypes::class)
@RequiresApi(Build.VERSION_CODES.O)
fun parseSOEvent(logData: String, cursor: Long, taskId: Long): MergedLogEntry {
    val parts = logData.split(" ").filter { it.isNotBlank() }
    val timestamp = parseTime(parts[0])
    val hDest = parseMacAddress(parts[1])
    val hSource = parseMacAddress(parts[2])
    val hProto = parseEthernetProtocol(parts[3].toUShort())
    val version = parts[4].toUByte()
    val ihl = parts[5].toUByte()
    val totLen = parts[6].toUShort()
    val id = parts[7].toUShort()
    val df = parts[8].toUByte()
    val mf = parts[9].toUByte()
    val fragOff = parts[10].toUShort()
    val ttl = parts[11].toUByte()
    val protocol = parts[12].toUByte()
    val sAddr = parseIPAddress(parts[13].toUInt())
    val dAddr = parseIPAddress(parts[14].toUInt())
    val eventId = parts[15].toULong()
    val eventFragIndex = parts[16].toUShort()
    val payload = parsePayload(parts)
    return MergedLogEntry(
        task_id = taskId,
        cursor = cursor,
        timestamp = timestamp,
        hDest = hDest,
        hSource = hSource,
        hProto = hProto,
        version = version,
        ihl = ihl,
        totLen = totLen,
        id = id,
        df = df,
        mf = mf,
        fragOff = fragOff,
        ttl = ttl,
        protocol = protocol,
        sAddr = sAddr,
        dAddr = dAddr,
        eventId = eventId,
        eventFragIndex = eventFragIndex,
        payload = payload
    )
}

@OptIn(ExperimentalUnsignedTypes::class)
fun parsePayload(parts: List<String>): UByteArray {
    // 将从第17个元素开始的部分转为字符串，假设这部分包含有效的数据
    val payloadString = parts.subList(17, parts.size).joinToString(" ")
    // 假设有效数据被方括号包围，移除方括号并分割为单独的数值
    val cleanPayloadString = payloadString.trimStart('[').trimEnd(']')
    // 分割字符串，移除空白并将每个数值转换为 UByte
    return cleanPayloadString.split(",").map { it.trim().toUByte() }.toUByteArray()
}


@RequiresApi(Build.VERSION_CODES.O)
fun parseTime(timeStr: String): Long {
    // 默认使用当前日期或指定一个固定日期
    val currentDate = LocalDate.now()  // 或 LocalDate.of(2021, Month.JANUARY, 1)
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val time = LocalTime.parse(timeStr, formatter)
    val dateTime = LocalDateTime.of(currentDate, time)

    // 转换 LocalDateTime 到秒级时间戳
    return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
}



fun parseMacAddress(macAddressStr: String): String {
    val parts = macAddressStr.substring(1, macAddressStr.length - 1).split(",").map { it.toInt() and 0xFF }
    return parts.joinToString(":") { "%02x".format(it) }
}

fun parseEthernetProtocol(proto: UShort): String {
    return when (proto) {
        0x0800u.toUShort() -> "IPv4"
        0x86DDu.toUShort() -> "IPv6"
        else -> "Unknown"
    }
}

fun parseIPAddress(ip: UInt): String {
    val byte1 = (ip shr 24) and 0xFFu
    val byte2 = (ip shr 16) and 0xFFu
    val byte3 = (ip shr 8) and 0xFFu
    val byte4 = ip and 0xFFu
    return "$byte1.$byte2.$byte3.$byte4"
}



