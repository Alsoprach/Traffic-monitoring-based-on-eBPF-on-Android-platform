package com.example.ebpf_network_bp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.annotation.RequiresApi
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            ApiTestApp()
//        }
//    }
//}
//
//@Composable
//fun ApiTestApp() {
//    var response by remember { mutableStateOf("Response will appear here...") }
//
//    Column(modifier = Modifier.padding(16.dp)) {
//        Button(onClick = {
//            CoroutineScope(Dispatchers.IO).launch {
//                response = ApiService.getTasks()
//            }
//        }) {
//            Text("Get Tasks")
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//
//
//        Button(onClick = {
//            CoroutineScope(Dispatchers.IO).launch {
//                response = ApiService.getTaskLog(1, 0, 10)
//            }
//        }) {
//            Text("Get Task Log")
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Button(onClick = {
//            CoroutineScope(Dispatchers.IO).launch {
//                response = ApiService.pauseTask(1)
//            }
//        }) {
//            Text("Pause Task")
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Button(onClick = {
//            CoroutineScope(Dispatchers.IO).launch {
//                response = ApiService.resumeTask(1)
//            }
//        }) {
//            Text("Resume Task")
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Text(text = response, style = MaterialTheme.typography.body1)
//    }
//}

//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.Button
//import androidx.compose.material.Text
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import android.content.Context
//import android.database.sqlite.SQLiteDatabase
//import java.io.File
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            Column(modifier = Modifier.padding(16.dp)) {
//                val message = remember { mutableStateOf("") }
//
//                Button(onClick = {
//                    message.value = tryToOpenDatabase()
//                }) {
//                    Text(text = "检查数据库文件")
//                }
//
//                Text(text = message.value)
//            }
//        }
//    }
//
//    private fun tryToOpenDatabase(): String {
//        val databaseName = "my_database.db"
//        val context: Context = applicationContext
//
//        // 获取应用专有目录下的数据库文件路径
//        val dbFile = File(context.getExternalFilesDir(null), databaseName)
//
//        return try {
//            // 创建或打开数据库连接
//            val database = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
//
//            // 如果需要，可以在这里执行一些数据库操作
//
//            database.close()
//
//            "数据库文件访问成功！路径：${dbFile.absolutePath}"
//        } catch (e: Exception) {
//            "访问数据库文件失败：${e.localizedMessage}"
//        }
//    }
//}

//import android.os.Build
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.annotation.RequiresApi
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.text.BasicText
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun TestLogParsing() {
//    val jsonData = """
//        [
//            {"cursor":0,"log":{"log":"06:10:05  [82,85,10,0,0,0] [2,21,178,0,0,0] 2048 4 5 60 57342  1      0      0        64     6        167772688 3232267116 0 0                [147,238,30,210,23,205,239,157,0,0,0,0,160,2,255,255,72,83,0,0,2,4,5,180,4,2,8,10,190,69,106,130,0,0,0,0,1,3,3,6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]","timestamp":1714371005,"log_type":"plain"}},
//            {"cursor":1,"log":{"log":"06:10:05  [82,85,10,0,0,0] [2,21,178,0,0,0] 2048 4 5 40 57343  1      0      0        64     6        167772688 3232267116 0 0                [147,238,30,210,23,205,239,158,163,215,126,2,80,16,255,255,72,63,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]","timestamp":1714371005,"log_type":"plain"}}
//        ]
//    """.trimIndent()
//
//    val logEntries = parseLogEntries(jsonData)
//    Column(modifier = Modifier.padding(16.dp)) {
//        logEntries.forEachIndexed { index, entry ->
//            Text("Entry $index:")
//            Text("Cursor: ${entry.cursor}")
//            Text("Log: ${entry.log}")
//        }
//    }
//}
//
//class MainActivity : ComponentActivity() {
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            TestLogParsing()
//        }
//    }
//}




//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        DatabaseManager.initialize(applicationContext)
//        val dbPath = DatabaseManager.getInstance().getDatabasePath(applicationContext)  // 获取数据库路径
//        setContent {
//            MyApp(databasePath = dbPath)  // 传递数据库路径到 Composable
//        }
//    }
//}


//@Composable
//fun MyApp(databasePath: String) {
//    var tasks by remember { mutableStateOf(listOf<com.example.ebpf_network_bp.Task>()) }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Button(onClick = {
//            DatabaseManager.getInstance().insertTask(com.example.ebpf_network_bp.Task(1, "Active", "Test Task"))
//            tasks = DatabaseManager.getInstance().queryTasks()  // 获取任务列表
//        }) {
//            Text(text = "添加并显示任务")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        if (tasks.isNotEmpty()) {
//            tasks.forEach { task ->
//                Text(text = "任务 ID: ${task.id}, 状态: ${task.status}, 名称: ${task.name}")
//            }
//        } else {
//            Text(text = "无任务显示")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // 显示数据库文件路径
//        Text(text = "数据库路径: $databasePath")
//    }
//}


//import androidx.activity.compose.setContent
//import androidx.annotation.RequiresApi
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.Button
//import androidx.compose.material.Text
//import androidx.compose.runtime.*
//
//
//import androidx.compose.ui.unit.dp

//@Composable
//fun Global_Init(){
//
//}
//
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DatabaseManager.initialize(applicationContext) // 数据库初始化
        setContent {
            Morandi_Theme {
                // Init BEGIN -------
                val logViewModel: LogViewModel = viewModel()
                TaskInit(logViewModel)
                // Init END ---------

                AppNavigation()
//                MyApp()
            }
        }
    }
}
//
////
//@OptIn(ExperimentalUnsignedTypes::class)
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun MyApp() {
//    // Init BEGIN -------
//    val logViewModel: LogViewModel = viewModel()
//    TaskInit(logViewModel)
//    // Init END ---------
//
//
//    var logEntries by remember { mutableStateOf<List<MergedLogEntry>>(emptyList()) }
//    var errorMessage by remember { mutableStateOf("") }
////    val jsonData = """[{"cursor":1,"log":{"log":"04:39:03  [82,85,10,0,0,0] [82,84,0,18,0,0] 2048 4 5 1236 49233 0     0      0        64     6        167772687 664551954 1  1                [167,172,0,80,83,171,46,178,158,74,118,2,80,24,255,255,122,131,0,0,71,69,84,32,47,32,72,84,84,80,47,49,46,49,13,10,72,111,115,116,58,32,119,119,119,46,98,97,105,100,117,46,99,111,109,13,10,67,111,110,110,101,99,116]","timestamp":1714624743,"log_type":"plain"}},{"cursor":2,"log":{"log":"04:39:03  [82,85,10,0,0,0] [82,84,0,18,0,0] 2048 4 5 1236 49233 0     0      0        64     6        167772687 664551954 1  2                [105,111,110,58,32,107,101,101,112,45,97,108,105,118,101,13,10,85,112,103,114,97,100,101,45,73,110,115,101,99,117,114,101,45,82,101,113,117,101,115,116,115,58,32,49,13,10,85,115,101,114,45,65,103,101,110,116,58,32,77,111,122,105,108]","timestamp":1714624743,"log_type":"plain"}},{"cursor":3,"log":{"log":"04:39:03  [82,85,10,0,0,0] [82,84,0,18,0,0] 2048 4 5 1236 49233 0     0      0        64     6        167772687 664551954 1  3                [108,97,47,53,46,48,32,40,76,105,110,117,120,59,32,65,110,100,114,111,105,100,32,49,48,59,32,75,41,32,65,112,112,108,101,87,101,98,75,105,116,47,53,51,55,46,51,54,32,40,75,72,84,77,76,44,32,108,105,107,101,32,71,101]","timestamp":1714624743,"log_type":"plain"}}]"""
////    val entries = parseLogEntries(1,jsonData)
////    var logdata : String = ""
////    var response by remember { mutableStateOf("Response will appear here...") }
//
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Button(onClick = {
//        try {
//
////                DatabaseManager.getInstance().insertLogEntries(entries)
////                DatabaseManager.getInstance().LogprocessEntries(1,1,1)
//            logEntries = DatabaseManager.getInstance().queryLogEntries(1, 3)
//        }catch (e: Exception) {
//            errorMessage = "Error：${e.localizedMessage}"
//        }
//        }) {
//            Text(text = "Load and Display Log Entries")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Text(text = errorMessage)
//
//
////        Text(text = "${entries.first().payload.toHexString()}")
//        Text(text = "${logViewModel.cursor}")
//    }
//
//}



//class MainActivity : ComponentActivity() {
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        DatabaseManager.initialize(applicationContext) // 数据库初始化
//        setContent {
//            Morandi_Theme {
//                AppNavigation()
//            }
//        }
//    }
//}

