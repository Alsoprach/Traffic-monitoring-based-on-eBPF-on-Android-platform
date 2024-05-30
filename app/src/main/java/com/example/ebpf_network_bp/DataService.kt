package com.example.ebpf_network_bp

import DatabaseManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class LogViewModel : ViewModel() {
    var cursor: Long = 1  // 初始cursor值为1
    private val taskId: Long = 1  // 默认从TaskId为1的任务读取日志
    private val maxCount: Long = 20  // 每次请求的最大日志条数

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchAndStoreLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonLogs = ApiService.getTaskLog(taskId, cursor, maxCount)
            val logEntries = parseLogEntries(taskId, jsonLogs)
            if (logEntries.isNotEmpty()) {
                DatabaseManager.getInstance().insertLogEntries(logEntries)
                updateCursor(logEntries)
            }
        }
    }

    private fun updateCursor(logEntries: List<MergedLogEntry>) {
        logEntries.maxByOrNull { it.cursor }?.let {
            cursor = it.cursor +1  // 更新cursor，准备下一次加载
        }
    }
}





// Message parser BEGIN ---------------------------------------------------------

data class TcpHeader @OptIn(ExperimentalUnsignedTypes::class) constructor(
    val sourcePort: Int,
    val destinationPort: Int,
    val sequenceNumber: Long,
    val acknowledgmentNumber: Long,
    val headerLength: Int,
    val flags: Int,
    val windowSize: Int,
    val checksum: Int,
    val urgentPointer: Int,
    val dataPayload: UByteArray  // 存储数据段
)

// 辅助函数，用于翻转整数的字节序
fun reverseBytes(value: Int): Long {
    val byte0 = value shr 24 and 0xFF
    val byte1 = value shr 16 and 0xFF
    val byte2 = value shr 8 and 0xFF
    val byte3 = value and 0xFF
    return ((byte3.toLong() shl 24) + (byte2 shl 16) + (byte1 shl 8) + byte0)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun parseTcpHeader(tcpBytes: UByteArray): TcpHeader {
    val bb = ByteBuffer.wrap(tcpBytes.toByteArray())
    bb.order(ByteOrder.BIG_ENDIAN)  // 明确指定使用大端序

    val sourcePort = bb.getShort().toInt() and 0xFFFF
    val destPort = bb.getShort().toInt() and 0xFFFF
    val sequenceNumberRaw = bb.getInt()
    val acknowledgmentNumberRaw = bb.getInt()
    // 翻转sequenceNumber和acknowledgmentNumber的字节序
    val sequenceNumber = reverseBytes(sequenceNumberRaw)
    val acknowledgmentNumber = reverseBytes(acknowledgmentNumberRaw)
    val dataOffsetAndFlags = bb.getShort().toInt()
    val dataOffset = (dataOffsetAndFlags ushr 12) * 4
    val flags = dataOffsetAndFlags and 0x1FF
    val windowSize = bb.getShort().toInt() and 0xFFFF
    val checksum = bb.getShort().toInt() and 0xFFFF
    val urgentPointer = bb.getShort().toInt() and 0xFFFF

    // 提取TCP数据段，通常位于头部之后
    val dataPayload = tcpBytes.copyOfRange(dataOffset, tcpBytes.size)

    return TcpHeader(
        sourcePort = sourcePort,
        destinationPort = destPort,
        sequenceNumber = sequenceNumber,
        acknowledgmentNumber = acknowledgmentNumber,
        headerLength = dataOffset,
        flags = flags,
        windowSize = windowSize,
        checksum = checksum,
        urgentPointer = urgentPointer,
        dataPayload = dataPayload
    )
}

data class HttpRequest(
    val method: String,
    val uri: String,
    val httpVersion: String,
    val headers: Map<String, String>,
    val body: String
)

@OptIn(ExperimentalUnsignedTypes::class)
fun parseHttpMessage(dataPayload: UByteArray): HttpRequest? {
    val message = dataPayload.toByteArray().toString(StandardCharsets.UTF_8)
    val lines = message.split("\r\n")

    if (lines.isEmpty()) return null  // 确保至少有一行数据

    // 解析请求行
    val requestLine = lines[0].split(" ")
    if (requestLine.size < 3) return null  // 请求行应包含方法，URI和HTTP版本
    val method = requestLine[0]
    val uri = requestLine[1]
    val httpVersion = requestLine[2]

    // 解析头部字段
    val headers = mutableMapOf<String, String>()
    var i = 1
    while (i < lines.size && lines[i].isNotEmpty()) {
        val header = lines[i].split(":")
        if (header.size >= 2) {
            val headerName = header[0].trim()
            val headerValue = header.drop(1).joinToString(":").trim()  // 如果值中也包含冒号，重新连接它们
            headers[headerName] = headerValue
        }
        i++
    }

    // 查找消息体：跳过空行
    // 确保在寻找消息体之前，'i'没有超出lines的范围
    if (i < lines.size) i++  // 跳过头部与消息体之间的空行

    // 消息体提取
    val body = if (i < lines.size) lines.drop(i).joinToString("\r\n") else ""

    return HttpRequest(
        method = method,
        uri = uri,
        httpVersion = httpVersion,
        headers = headers,
        body = body
    )
}


// Message parser END ---------------------------------------------------------

// Frag Deal BEGIN ------------------------------------------------------------
fun processAndMergeEntries() {
    // 获取所有 eventId 不为 0 的记录
    val nonZeroEventIds = DatabaseManager.getInstance().findNonZeroEventIds()

    // 使用 HashSet 来存储已处理的 eventId，避免重复处理
    val processedEventIds = HashSet<Long>()

    // 遍历每个 (id, eventId) 对
    for ((id, eventId) in nonZeroEventIds) {
        // 如果 eventId 已经处理过，跳过以避免重复处理
        if (processedEventIds.contains(eventId)) {
            continue
        }
        // 处理日志条目，显式指定参数名以清晰传递参数
        DatabaseManager.getInstance().LogprocessEntries(1, id = id, eventId = eventId)

        // 将此 eventId 标记为已处理
        processedEventIds.add(eventId)
    }
}
// Frag Deal END ------------------------------------------------------------

fun getLogEntryByIndex(index: Int): MergedLogEntry? {
    var logEntries: List<MergedLogEntry> = DatabaseManager.getInstance().readLogEntriesDescendingOrder()
    return if (index in logEntries.indices) {
        logEntries[index]
    } else {
        null
    }
}

fun getAllTaskId(): List<Long>{
    return DatabaseManager.getInstance().getAllTaskIdsFromLogEntries()
}

// 获取所有不重复的 sAddr
fun getAllDistinctSAddrs(): List<String> {
    return DatabaseManager.getInstance().getAllDistinctSAddr()
}

// 获取所有不重复的 dAddr
fun getAllDistinctDAddrs(): List<String> {
    return DatabaseManager.getInstance().getAllDistinctDAddr()
}

// 获取所有不重复的 hDest
fun getAllDistinctHDests(): List<String> {
    return DatabaseManager.getInstance().getAllDistinctHDest()
}

// 获取所有不重复的 hSource
fun getAllDistinctHSources(): List<String> {
    return DatabaseManager.getInstance().getAllDistinctHSource()
}

// Rule Set ------------------------------------------------------------
enum class RuleField(val displayName: String, val dataType: String) {
    // MergedLogEntry fields
    TASK_ID("Task ID", "Long"),
    CURSOR("Cursor", "Long"),
    TIMESTAMP("Timestamp", "Long"),
    H_DEST("Destination MAC Address", "String"),
    H_SOURCE("Source MAC Address", "String"),
    H_PROTO("Ethernet Protocol", "String"),
    VERSION("IP Version", "UByte"),
    IHL("IP Header Length", "UByte"),
    TOTAL_LENGTH("IP Total Length", "UShort"),
    ID("IP Identifier", "UShort"),
    DF("Do Not Fragment", "UByte"),
    MF("More Fragments", "UByte"),
    FRAG_OFFSET("Fragment Offset", "UShort"),
    TTL("Time To Live", "UByte"),
    PROTOCOL("IP Protocol", "UByte"),
    S_ADDR("Source IP Address", "String"),
    D_ADDR("Destination IP Address", "String"),
    EVENT_ID("Event ID", "ULong"),
    EVENT_FRAG_INDEX("Event Fragment Index", "UShort"),
    PAYLOAD("Payload", "UByteArray"),

    // TcpHeader fields
    SOURCE_PORT("Source Port", "Int"),
    DESTINATION_PORT("Destination Port", "Int"),
    SEQUENCE_NUMBER("Sequence Number", "Long"),
    ACKNOWLEDGMENT_NUMBER("Acknowledgment Number", "Long"),
    HEADER_LENGTH("Header Length", "Int"),
    FLAGS("Flags", "Int"),
    WINDOW_SIZE("Window Size", "Int"),
    CHECKSUM("Checksum", "Int"),
    URGENT_POINTER("Urgent Pointer", "Int"),
    DATA_PAYLOAD("TCP Data Payload", "UByteArray"),

    // HttpRequest fields
    METHOD("HTTP Method", "String"),
    URI("URI", "String"),
    HTTP_VERSION("HTTP Version", "String"),
    HEADERS("Headers", "Map<String, String>"),
    BODY("Body", "String");

    override fun toString(): String = "$displayName ($dataType)"
}

fun isValidRegex(regex: String): Boolean {
    return try {
        Pattern.compile(regex)
        true // 正则表达式有效
    } catch (e: PatternSyntaxException) {
        false // 正则表达式语法有误
    }
}

// 尝试添加新规则并处理结果
fun tryAddRule(field: RuleField?, regex: String): Boolean {
    return if (field != null && regex.isNotEmpty()) {
        if (isValidRegex(regex)) {
            val newRule = DatabaseManager.Rule(objectField = field.displayName, regex = regex)
            DatabaseManager.getInstance().insertRule(newRule)
        } else {
            Log.e("RuleScreen", "Invalid regex syntax")
            false
        }
    } else {
        Log.e("RuleScreen", if (regex.isEmpty()) "Regex input is empty" else "No field selected")
        false
    }
}

fun getAllRules(): List<DatabaseManager.Rule>{
    return DatabaseManager.getInstance().getAllRules()
}

fun deleteRule(ruleId: Int): Boolean {
    return DatabaseManager.getInstance().deleteRule(ruleId)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun checkRuleAgainstEntry(rule: DatabaseManager.Rule, entry: MergedLogEntry): Boolean {
//     解析 TCP 头部，假设 payload 包含 TCP 数据
    val tcpHeader = parseTcpHeader(entry.payload)
//     解析 HTTP 消息，假设 TCP 数据有效
    val httpRequest = parseHttpMessage(tcpHeader.dataPayload)

//     从 entry, tcpHeader, 或 httpRequest 获取字段值
    val fieldValue: String = when (rule.objectField) {
        RuleField.TASK_ID.displayName -> entry.task_id.toString()
        RuleField.CURSOR.displayName -> entry.cursor.toString()
        RuleField.TIMESTAMP.displayName-> entry.timestamp.toString()
        RuleField.H_DEST.displayName -> entry.hDest
        RuleField.H_SOURCE.displayName -> entry.hSource
        RuleField.H_PROTO.displayName -> entry.hProto
        RuleField.VERSION.displayName -> entry.version.toString()
        RuleField.IHL.displayName -> entry.ihl.toString()
        RuleField.TOTAL_LENGTH.displayName -> entry.totLen.toString()
        RuleField.ID.displayName -> entry.id.toString()
        RuleField.DF.displayName -> entry.df.toString()
        RuleField.MF.displayName -> entry.mf.toString()
        RuleField.FRAG_OFFSET.displayName -> entry.fragOff.toString()
        RuleField.TTL.displayName -> entry.ttl.toString()
        RuleField.PROTOCOL.displayName -> entry.protocol.toString()
        RuleField.S_ADDR.displayName -> entry.sAddr
        RuleField.D_ADDR.displayName -> entry.dAddr
        RuleField.EVENT_ID.displayName -> entry.eventId.toString()
        RuleField.EVENT_FRAG_INDEX.displayName -> entry.eventFragIndex.toString()
        RuleField.PAYLOAD.displayName -> String(entry.payload.toByteArray(), StandardCharsets.UTF_8)
        RuleField.SOURCE_PORT.displayName -> tcpHeader.sourcePort.toString()
        RuleField.DESTINATION_PORT.displayName -> tcpHeader.destinationPort.toString()
        RuleField.SEQUENCE_NUMBER.displayName -> tcpHeader.sequenceNumber.toString()
        RuleField.ACKNOWLEDGMENT_NUMBER.displayName -> tcpHeader.acknowledgmentNumber.toString()
        RuleField.HEADER_LENGTH.displayName -> tcpHeader.headerLength.toString()
        RuleField.FLAGS.displayName -> tcpHeader.flags.toString()
        RuleField.WINDOW_SIZE.displayName -> tcpHeader.windowSize.toString()
        RuleField.CHECKSUM.displayName -> tcpHeader.checksum.toString()
        RuleField.URGENT_POINTER.displayName -> tcpHeader.urgentPointer.toString()
        RuleField.DATA_PAYLOAD.displayName -> String(tcpHeader.dataPayload.toByteArray(), StandardCharsets.UTF_8)
        RuleField.METHOD.displayName -> httpRequest?.method ?: ""
        RuleField.URI.displayName -> httpRequest?.uri ?: ""
        RuleField.HTTP_VERSION.displayName -> httpRequest?.httpVersion ?: ""
        RuleField.HEADERS.displayName -> httpRequest?.headers?.entries?.joinToString("; ") { "${it.key}: ${it.value}" } ?: ""
        RuleField.BODY.displayName -> httpRequest?.body ?: ""
        else -> return false  // 如果字段不匹配任何已知字段，返回 false
    }

    // 使用正则表达式检查字段值是否匹配
    return Regex(rule.regex).matches(fieldValue)
}

fun scanEntriesAndRecordHits() {
    // 从数据库获取所有日志条目和规则
    val entries = DatabaseManager.getInstance().readAllLogEntriesDescendingOrder()
    val rules = getAllRules()

    // 遍历每个日志条目
    entries.forEach { entry ->
        // 遍历每条规则
        rules.forEach { rule ->
            // 检查当前条目是否符合当前规则
            if (checkRuleAgainstEntry(rule, entry)) {
                // 如果匹配，创建 Hit 对象
                val hit = DatabaseManager.Hit(
                    taskId = entry.task_id.toInt(),
                    cursor = entry.cursor.toInt(),
                    ruleId = rule.ruleId ?: -1
                )
                // 将 Hit 插入数据库
                if (!DatabaseManager.getInstance().insertHit(hit)) {
                    println("Failed to insert hit: Task ID: ${hit.taskId}, Cursor: ${hit.cursor}, Rule ID: ${hit.ruleId}")
                }
            }
        }
    }
}

fun getLastNHits(n:Int): List<DatabaseManager.Hit> {
    return DatabaseManager.getInstance().getLastNHits(n)
}

fun getMergedLogEntry(taskId: Int, cursor: Int): MergedLogEntry? {
    return DatabaseManager.getInstance().getMergedLogEntry(taskId.toLong(),cursor.toLong())
}