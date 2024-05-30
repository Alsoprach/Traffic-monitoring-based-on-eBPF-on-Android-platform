package com.example.ebpf_network_bp

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TaskService {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineScope.launch {
            repeat(Int.MAX_VALUE) {
                    block()
                }
            }
    }

    fun cancelAll() {
        coroutineScope.coroutineContext.cancelChildren()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun TaskInit(logViewModel:LogViewModel){
    TaskService.launch {
//        runWithExceptionHandling(this) {
            logViewModel.fetchAndStoreLogs()
//        }
            delay(500)
//        runWithExceptionHandling(this) {
            processAndMergeEntries()
//        }
            delay(500)
            scanEntriesAndRecordHits()
            delay(500)
    }
}

