package com.example.ebpf_network_bp

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.composed.plus
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.composed.ComposedChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entriesOf
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChartA() {
    Chart(
        chart = lineChart(),
        model = createChartEntryModel(generateLogEntryData()),
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun generateLogEntryData(): List<Pair<Number, Number>> {
    val currentTime = getMaxTimestamp()
    val twelveHoursAgo = currentTime.minus(9, ChronoUnit.HOURS)

    val entries = mutableListOf<Pair<Number, Number>>()

    var endTime = currentTime
    while (endTime.isAfter(twelveHoursAgo)) {
        val startTime = endTime.minus(1, ChronoUnit.HOURS)
        val midPoint = startTime.plus(30, ChronoUnit.MINUTES)
        val midHour = midPoint.atZone(ZoneOffset.UTC).hour  // 获取中位数小时

        val count = DatabaseManager.getInstance().countLogEntriesBetween(startTime.epochSecond, endTime.epochSecond).toLong()
        entries.add(Pair(midHour, count))

        endTime = startTime
    }

    return entries.reversed() // 将列表反转，使得最新的数据在列表前面
}

@RequiresApi(Build.VERSION_CODES.O)
fun getMaxTimestamp(): Instant {
    val maxTimestamp = DatabaseManager.getInstance().getMaxTimestamp() // 假设这个函数从数据库中获取最大时间戳
    return if (maxTimestamp != -1L) {
        Instant.ofEpochMilli(maxTimestamp)
    } else {
        Instant.now()
    }
}

fun createChartEntryModel(entries: List<Pair<Number, Number>>): ChartEntryModel {
    return entryModelOf(*entries.toTypedArray())
}




