package com.example.ebpf_network_bp

import DatabaseManager
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog

// Color -------------------------------------------------
object MorandiColors_set {
    val primary = Color(0xFFA8A8A8)  // 柔和灰
    val primaryVariant = Color(0xFF787878)  // 深柔和灰
    val secondary = Color(0xFFE0C0B1)  // 米色
    val secondaryVariant = Color(0xFFC0A091)  // 深米色
    val tertiary = Color(0xFFBDB4A7)  // 淡黄灰
    val tertiaryVariant = Color(0xFF9E9588)  // 深淡黄灰
    val background = Color(0xFFF0EAE2)  // 淡米色背景
    val surface = Color(0xFFEFEFEF)  // 表面色，用于卡片、对话框等元素

    // 修改文字颜色以提供更好的对比度和视觉效果
    val onPrimary = Color.White  // 在主色调上的文字颜色，改为白色以提高对比度
    val onSecondary = Color.DarkGray  // 在次级色调上的文字颜色，使用深灰色以提高可读性
    val onBackground = Color.DarkGray  // 在背景色上的文字颜色，同样选择深灰色
    val onSurface = Color.DarkGray  // 在表面色上的文字颜色，深灰色以保持一致性

    val error = Color(0xFFB00020)  // 明显的红色，用于错误提示
    val onError = Color.White  // 在错误颜色上的文字颜色，使用白色以提高可见性

    val accent = Color(0xFF46413a)  //
    val accentVariant = Color(0xFFB8860B)  // 深金色，用于强调按钮的激活状态
    val info = Color(0xFF3DA9FC)  // 信息蓝，用于提示和通知
    val onSuccess = Color.White  // 在成功提示色上的文字颜色，白色以增加可读性
    val success = Color(0xFF2E8B57)  // 成功绿，用于成功操作的提示
}

val MorandiColors = lightColors(
    primary = Color(0xFFA8A8A8), // 柔和灰
    primaryVariant = Color(0xFFD8C8B8), // 深一点的灰
    secondary = Color(0xFFE0C0B1), // 米色
    secondaryVariant = Color(0xFFB5BAC0), // 淡蓝灰
    background = Color(0xFFF6F2F1), // 背景色
    surface = Color(0xFFEFEFEF), // 表面色
    onPrimary = Color.Black, // 主色调上的文字颜色
    onSecondary = Color.Black, // 次色调上的文字颜色
    onBackground = Color.Black, // 背景色上的文字颜色
    onSurface = Color.Black, // 表面色上的文字颜色
    // 新增的色彩
    error = Color(0xFFB00020), // 错误颜色
    onError = Color.White, // 错误颜色上的文字颜色

)


@Composable
fun Morandi_Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MorandiColors,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}

// Color END -------------------------------------------------



// Navigation BEGIN ---------------------------------------------------
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "MainScreen") {
        composable("MainScreen") { MainScreen(navController) }
        composable("WarningListScreen/{taskid-cursor}") { backStackEntry ->
            val taskid_cursor = backStackEntry.arguments?.getString("taskid-cursor") ?: "0-0"
            val tmp = splitStringToInts(taskid_cursor)

            if (tmp != null) {
                WarningWatchUnit(navController, tmp.first, tmp.second)
            }

        }

        composable("Traffic") { TrafficScreen(navController) }
        composable("Traffic/TrafficWatch/{index}") { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            TrafficWatchUnit(navController, index)
        }

        composable("Rule") { RuleScreen(navController) }
    }
}

fun splitStringToInts(input: String): Pair<Int, Int>? {
    // 使用正则表达式匹配输入字符串
    val regex = Regex("(\\d+)-(\\d+)")
    val matchResult = regex.find(input)

    // 如果找到匹配项
    if (matchResult != null) {
        // 提取匹配的两个整数部分
        val (first, second) = matchResult.destructured
        // 将两个整数部分转换为整数并返回
        return Pair(first.toInt(), second.toInt())
    } else {
        // 如果未找到匹配项，返回null
        return Pair(0,0)
    }
}

// Navigation END   ---------------------------------------------------

// MainScreen BEGIN -------------------------------------------------
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MainScreen(navController: NavController) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()


    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { Main_Bar(scope, scaffoldState) },
        drawerContent = { DrawerContent(navController) },
        content = { MainContent(navController) }
    )
}

@Composable
fun Main_Bar(scope: CoroutineScope, scaffoldState: ScaffoldState) {
    TopAppBar(
        title = { Text("Submarine") },
        backgroundColor = MorandiColors.secondaryVariant,  // 你可以设置为你希望的颜色
        contentColor = MorandiColors.onPrimary,  // 设置内容颜色，如标题和图标颜色
        actions = {
            IconButton(onClick = {
                scope.launch {
                    scaffoldState.drawerState.open()
                }
            }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
    )
}

@Composable
fun DrawerContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth() // 确保Column占满抽屉的宽度
            .padding(horizontal = 8.dp) // 根据需要调整这个值以确保对齐
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Navigation",
            modifier = Modifier
                .fillMaxWidth() // 使Text占满Column的宽度
                .padding(1.dp), // 设置内边距，可以根据需要进一步调整
            style = TextStyle(
                fontSize = 25.sp, // 设置字体大小为18sp
                color = MorandiColors_set.accent, // 设置字体颜色为蓝色
                fontWeight = FontWeight.Bold // 可选: 设置字体加粗
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Statistical: Filter and Read",
            modifier = Modifier
                .fillMaxWidth() // 使Text占满Column的宽度
                .padding(1.dp) // 设置内边距，可以根据需要进一步调整
                .clickable { navController.navigate("Traffic") },
            style = TextStyle(
                fontSize = 20.sp, // 设置字体大小为18sp
                color = MorandiColors_set.onBackground, // 设置字体颜色为蓝色
                fontWeight = FontWeight.Black // 可选: 设置字体加粗
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Rule Set ",
            modifier = Modifier
                .fillMaxWidth() // 使Text占满Column的宽度
                .padding(1.dp) // 设置内边距，可以根据需要进一步调整
                .clickable { navController.navigate("Rule") },
            style = TextStyle(
                fontSize = 20.sp, // 设置字体大小为18sp
                color = MorandiColors_set.onBackground, // 设置字体颜色为蓝色
                fontWeight = FontWeight.Black // 可选: 设置字体加粗
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

}


@OptIn(ExperimentalUnsignedTypes::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainContent(navController: NavController) {
    Box(modifier = Modifier.padding(16.dp)) {
        Column {
            ChartA()
            WarningList(navController)
        }
    }
}


@Composable
fun WarningList(navController: NavController) {
    val hits = remember { getLastNHits(n = 100) }
    LazyColumn(
        modifier = Modifier.padding(8.dp)
    ) {
        items(hits.size) { index ->
            val hit = hits[index]
            val logEntry = getMergedLogEntry(taskId = hit.taskId, cursor = hit.cursor)
            val taskid = hit.taskId
            val cursor = hit.cursor
            if (logEntry != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { navController.navigate("WarningListScreen/$taskid-$cursor") },
                    shape = RoundedCornerShape(8.dp),
                    elevation = 4.dp
                ) {
                    WarningListUnit(logEntry = logEntry)
                }
            }
        }
    }
}


// MainScreen END ------------------------------------------------------

// ItemsList Service BEGIN ------------------------------------------------------
@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun WarningListUnit(logEntry: MergedLogEntry) {
    val tcpHeader = parseTcpHeader(logEntry.payload)
    val httpMessage = parseHttpMessage(tcpHeader.dataPayload)

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Source IP: ${logEntry.sAddr}")
        Text("Destination IP: ${logEntry.dAddr}")
        Text("Source Port: ${tcpHeader.sourcePort}")
        Text("Destination Port: ${tcpHeader.destinationPort}")
        if (httpMessage != null) {
            Text("HTTP Request: ${httpMessage.method} ${httpMessage.uri} ${httpMessage.httpVersion}")
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun WarningWatchUnit(navController: NavController, taskId: Int,cursor:Int) {
    val logEntry = getMergedLogEntry(taskId = taskId, cursor = cursor)



    val tcpHeader = logEntry?.let { parseTcpHeader(it.payload) }
    val httpMessage = tcpHeader?.let { parseHttpMessage(it.dataPayload) }

    var EthExpanded by remember { mutableStateOf(false) }
    var ipExpanded by remember { mutableStateOf(false) }
    var tcpExpanded by remember { mutableStateOf(false) }
    var httpExpanded by remember { mutableStateOf(false) }
    var dataExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MorandiColors.background)
    ) {
        // 顶部栏
        TopAppBar(
            title = { Text("Warning Watch") },
            backgroundColor = MorandiColors.secondaryVariant,  // 你可以设置为你希望的颜色
            contentColor = MorandiColors.onPrimary,
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // 滑动列表
        Column(modifier = Modifier.weight(1f)) {
            // 以太网帧 报文头
            CollapsibleSection(
                title = "Ethernet frame Header",
                expanded = EthExpanded,
                onToggle = { EthExpanded = !EthExpanded }
            ) {
                logEntry?.let {
                    Text("Source MAC: ${it.hSource}", fontWeight = FontWeight.Bold)
                    Text("Destination MAC: ${it.hDest}", fontWeight = FontWeight.Bold)
                    Text("Version: ${it.version}", fontWeight = FontWeight.Bold)
                }
            }
            // IP 报文头
            CollapsibleSection(
                title = "IP Header",
                expanded = ipExpanded,
                onToggle = { ipExpanded = !ipExpanded }
            ) {
                logEntry?.let {
                    Text("Header Length: ${it.ihl}", fontWeight = FontWeight.Bold)
                    Text("Total Length: ${it.totLen}", fontWeight = FontWeight.Bold)
                    Text("Identifier: ${it.id}", fontWeight = FontWeight.Bold)
                    Text("Do Not Fragment: ${it.df}", fontWeight = FontWeight.Bold)
                    Text("More Fragments: ${it.mf}", fontWeight = FontWeight.Bold)
                    Text("Fragment Offset: ${it.fragOff}", fontWeight = FontWeight.Bold)
                    Text("Time to Live: ${it.ttl}", fontWeight = FontWeight.Bold)
                    Text("Protocol: ${it.protocol}", fontWeight = FontWeight.Bold)
                    Text("Source IP: ${it.sAddr}", fontWeight = FontWeight.Bold)
                    Text("Destination IP: ${it.dAddr}", fontWeight = FontWeight.Bold)
                }
            }

            // TCP 报文头
            CollapsibleSection(
                title = "TCP Header",
                expanded = tcpExpanded,
                onToggle = { tcpExpanded = !tcpExpanded }
            ) {
                tcpHeader?.let {
                    Text("Source Port: ${it.sourcePort}", fontWeight = FontWeight.Bold)
                    Text("Destination Port: ${it.destinationPort}", fontWeight = FontWeight.Bold)
                    Text("Sequence Number: ${it.sequenceNumber}", fontWeight = FontWeight.Bold)
                    Text("Acknowledgment Number: ${it.acknowledgmentNumber}", fontWeight = FontWeight.Bold)
                    Text("Header Length: ${it.headerLength}", fontWeight = FontWeight.Bold)
                    Text("Flags: ${it.flags}", fontWeight = FontWeight.Bold)
                    Text("Window Size: ${it.windowSize}", fontWeight = FontWeight.Bold)
                    Text("Checksum: ${it.checksum}", fontWeight = FontWeight.Bold)
                    Text("Urgent Pointer: ${it.urgentPointer}", fontWeight = FontWeight.Bold)
                    // 如果需要显示数据段，可以在此处添加相应的代码
                }
            }

            // HTTP 报文头
            CollapsibleSection(
                title = "HTTP Header",
                expanded = httpExpanded,
                onToggle = { httpExpanded = !httpExpanded }
            ) {
                httpMessage?.let {
                    Text("HTTP Request: ${it.method} ${it.uri} ${it.httpVersion}", fontWeight = FontWeight.Bold)
                    // 如果需要显示头部信息，可以在此处添加相应的代码
                }
            }

            // HTTP 数据段
            CollapsibleSection(
                title = "HTTP Data",
                expanded = dataExpanded,
                onToggle = { dataExpanded = !dataExpanded }
            ) {
                httpMessage?.let {
                    Text("${it.body}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
// ItemsList Service END ------------------------------------------------------

// Traffic Service BEGIN ------------------------------------------------------

data class SelectionState(
    val selectedTaskId: MutableState<Long?>,
    val selectedSAddr: MutableState<String?>,
    val selectedDAddr: MutableState<String?>,
    val selectedHDest: MutableState<String?>,
    val selectedHSource: MutableState<String?>
)


@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun TrafficScreen(navController: NavController) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) } // 控制弹窗的显示

    val selectionState = remember {
        SelectionState(
            selectedTaskId = mutableStateOf(null),
            selectedSAddr = mutableStateOf(null),
            selectedDAddr = mutableStateOf(null),
            selectedHDest = mutableStateOf(null),
            selectedHSource = mutableStateOf(null)
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { Traffic_Bar(scope, scaffoldState, navController) { showDialog = true } }, // 传递函数以控制弹窗
        content = { TrafficContent(navController,selectionState) }
    )

    if (showDialog) {
        TrafficDialog( onDismiss = { showDialog = false },selectionState) // 弹窗组件
    }
}

@Composable
fun Traffic_Bar(scope: CoroutineScope, scaffoldState: ScaffoldState, navController: NavController, onMenuClick: () -> Unit) {
    TopAppBar(
        title = { Text("Statistical") },
        backgroundColor = MorandiColors.secondaryVariant,
        contentColor = MorandiColors.onPrimary,
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Go Back")
            }
        },
        actions = {
            IconButton(onClick = onMenuClick) { // 点击打开弹窗
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
    )
}

@Composable
fun TrafficDialog(onDismiss: () -> Unit,selectionState: SelectionState) {
    Dialog(onDismissRequest = onDismiss,
    ) {
        Card(
            backgroundColor = Color.White, // 可以修改为你喜欢的颜色
            shape = RoundedCornerShape(8.dp), // 圆角边框
            elevation = 8.dp // 阴影高度
        ) {
            ParameterSelectionScreen(selectionState)
        }
    }
}

@Composable
fun TrafficContent(navController: NavController, selectionState: SelectionState) {
    val refreshTrigger = remember { mutableIntStateOf(0) }

    // 每当过滤条件变化时，修改refreshTrigger的值，从而触发LazyColumn的刷新
    LaunchedEffect(selectionState) {
        refreshTrigger.intValue++
    }

    LazyColumn {
        items((0 until 100).toList()) { index ->
            if (isEntryValid(index, selectionState)) {
                ClickableTrafficUnit(index,navController)
            }
        }
    }
}

@Composable
fun ClickableTrafficUnit(index: Int, navController: NavController) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { navController.navigate("Traffic/TrafficWatch/$index") }
    ) {
        TrafficUnit(index)
    }
}

fun isEntryValid(index: Int, selectionState: SelectionState): Boolean {
    val logEntry = getLogEntryByIndex(index)
    if (logEntry != null) {
        with(selectionState) {
            val selectedValues = listOf(
                selectedTaskId.value,
                selectedSAddr.value,
                selectedDAddr.value,
                selectedHDest.value,
                selectedHSource.value
            )
            val entryValues = listOf(
                logEntry.task_id,
                logEntry.sAddr,
                logEntry.dAddr,
                logEntry.hDest,
                logEntry.hSource
            )
            return selectedValues.zip(entryValues)
                .all { (selected, entry) ->
                    selected == null || entry == null || selected == entry
                }
        }
    } else {
        return false
    }
}



@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun TrafficUnit(index: Int) {
    val logEntry = getLogEntryByIndex(index)
    if (logEntry != null) {
        val tcpHeader = parseTcpHeader(logEntry.payload)
        val httpMessage = parseHttpMessage(tcpHeader.dataPayload)

        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Source IP: ${logEntry.sAddr}")
            Text("Destination IP: ${logEntry.dAddr}")
            Text("Source Port: ${tcpHeader.sourcePort}")
            Text("Destination Port: ${tcpHeader.destinationPort}")
            if (httpMessage != null) {
                Text("HTTP Request: ${httpMessage.method} ${httpMessage.uri} ${httpMessage.httpVersion}")
            }
        }
    }
}

@Composable
fun ParameterSelectionScreen(selectionState: SelectionState) {
    // 获取所有不重复的值
    val taskIds = getAllTaskId()
    val sAddrs = getAllDistinctSAddrs()
    val dAddrs = getAllDistinctDAddrs()
    val hDests = getAllDistinctHDests()
    val hSources = getAllDistinctHSources()

    // 界面布局
    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DropdownSelect("Task ID", taskIds, selectionState.selectedTaskId, { it.toString() })
        DropdownSelect("sAddr", sAddrs, selectionState.selectedSAddr)
        DropdownSelect("dAddr", dAddrs, selectionState.selectedDAddr)
        DropdownSelect("hDest", hDests, selectionState.selectedHDest)
        DropdownSelect("hSource", hSources, selectionState.selectedHSource)
    }
}


@Composable
fun <T> DropdownSelect(
    label: String,
    items: List<T>, // 项目列表
    selectedItem: MutableState<T?>, // 当前选中的项目
    itemToString: (T?) -> String = { it?.toString() ?: "None" } // 如何将项目转换为字符串显示，默认是调用toString
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${label}: ${selectedItem.value?.let(itemToString) ?: "Any"}", style = MaterialTheme.typography.h6)
        IconButton(onClick = { isMenuExpanded = true }) {
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Expand Dropdown")
        }
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier = Modifier.width(200.dp)
        ) {
            DropdownMenuItem(onClick = {
                selectedItem.value = null // 设置为 None
                isMenuExpanded = false
            }) {
                Text("Any")
            }
            items.forEach { item ->
                DropdownMenuItem(onClick = {
                    selectedItem.value = item
                    isMenuExpanded = false
                }) {
                    Text(itemToString(item))
                }
            }
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun TrafficWatchUnit(navController: NavController, index: Int) {
    val logEntry = getLogEntryByIndex(index)
    val tcpHeader = logEntry?.let { parseTcpHeader(it.payload) }
    val httpMessage = tcpHeader?.let { parseHttpMessage(it.dataPayload) }

    var EthExpanded by remember { mutableStateOf(false) }
    var ipExpanded by remember { mutableStateOf(false) }
    var tcpExpanded by remember { mutableStateOf(false) }
    var httpExpanded by remember { mutableStateOf(false) }
    var dataExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MorandiColors.background)
    ) {
        // 顶部栏
        TopAppBar(
            title = { Text("Traffic Watch") },
            backgroundColor = MorandiColors.secondaryVariant,  // 你可以设置为你希望的颜色
            contentColor = MorandiColors.onPrimary,
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // 滑动列表
        Column(modifier = Modifier.weight(1f)) {
            // 以太网帧 报文头
            CollapsibleSection(
                title = "Ethernet frame Header",
                expanded = EthExpanded,
                onToggle = { EthExpanded = !EthExpanded }
            ) {
                logEntry?.let {
                    Text("Source MAC: ${it.hSource}", fontWeight = FontWeight.Bold)
                    Text("Destination MAC: ${it.hDest}", fontWeight = FontWeight.Bold)
                    Text("Version: ${it.version}", fontWeight = FontWeight.Bold)
                }
            }
            // IP 报文头
            CollapsibleSection(
                title = "IP Header",
                expanded = ipExpanded,
                onToggle = { ipExpanded = !ipExpanded }
            ) {
                logEntry?.let {
                    Text("Header Length: ${it.ihl}", fontWeight = FontWeight.Bold)
                    Text("Total Length: ${it.totLen}", fontWeight = FontWeight.Bold)
                    Text("Identifier: ${it.id}", fontWeight = FontWeight.Bold)
                    Text("Do Not Fragment: ${it.df}", fontWeight = FontWeight.Bold)
                    Text("More Fragments: ${it.mf}", fontWeight = FontWeight.Bold)
                    Text("Fragment Offset: ${it.fragOff}", fontWeight = FontWeight.Bold)
                    Text("Time to Live: ${it.ttl}", fontWeight = FontWeight.Bold)
                    Text("Protocol: ${it.protocol}", fontWeight = FontWeight.Bold)
                    Text("Source IP: ${it.sAddr}", fontWeight = FontWeight.Bold)
                    Text("Destination IP: ${it.dAddr}", fontWeight = FontWeight.Bold)
                }
            }

            // TCP 报文头
            CollapsibleSection(
                title = "TCP Header",
                expanded = tcpExpanded,
                onToggle = { tcpExpanded = !tcpExpanded }
            ) {
                tcpHeader?.let {
                    Text("Source Port: ${it.sourcePort}", fontWeight = FontWeight.Bold)
                    Text("Destination Port: ${it.destinationPort}", fontWeight = FontWeight.Bold)
                    Text("Sequence Number: ${it.sequenceNumber}", fontWeight = FontWeight.Bold)
                    Text("Acknowledgment Number: ${it.acknowledgmentNumber}", fontWeight = FontWeight.Bold)
                    Text("Header Length: ${it.headerLength}", fontWeight = FontWeight.Bold)
                    Text("Flags: ${it.flags}", fontWeight = FontWeight.Bold)
                    Text("Window Size: ${it.windowSize}", fontWeight = FontWeight.Bold)
                    Text("Checksum: ${it.checksum}", fontWeight = FontWeight.Bold)
                    Text("Urgent Pointer: ${it.urgentPointer}", fontWeight = FontWeight.Bold)
                    // 如果需要显示数据段，可以在此处添加相应的代码
                }
            }

            // HTTP 报文头
            CollapsibleSection(
                title = "HTTP Header",
                expanded = httpExpanded,
                onToggle = { httpExpanded = !httpExpanded }
            ) {
                httpMessage?.let {
                    Text("HTTP Request: ${it.method} ${it.uri} ${it.httpVersion}", fontWeight = FontWeight.Bold)
                    // 如果需要显示头部信息，可以在此处添加相应的代码
                }
            }

            // HTTP 数据段
            CollapsibleSection(
                title = "HTTP Data",
                expanded = dataExpanded,
                onToggle = { dataExpanded = !dataExpanded }
            ) {
                httpMessage?.let {
                    Text("${it.body}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ){
                Spacer(modifier = Modifier.width(26.dp))
                content()
            }
        }
    }
}

// Traffic Service END ------------------------------------------------------

// Rule Set BEGIN -----------------------------------------------------------

@Composable
fun RuleScreen(navController: NavController) {
    val ruleFields = RuleField.values().toList()
    val selectedRuleField = remember { mutableStateOf<RuleField?>(null) }
    val regexInput = remember { mutableStateOf("") }
    val showDialog = remember { mutableStateOf(false) }
    val dialogMessage = remember { mutableStateOf("") }
    val rulesList = remember { mutableStateOf(getAllRules()) }  // 初始加载所有规则

    // 规则提交函数
    val ruleSubmit = {
        if (tryAddRule(selectedRuleField.value, regexInput.value)) {
            regexInput.value = ""
            selectedRuleField.value = null
            dialogMessage.value = "Rule successfully added."
            showDialog.value = true
            rulesList.value = getAllRules()  // 重新获取所有规则以更新列表
        } else {
            dialogMessage.value = "Failed to add rule. Please check the input."
            showDialog.value = true
        }
    }

    // 删除规则的逻辑
    fun deleteRuleAndRefresh(ruleId: Int) {
        if (deleteRule(ruleId)) {
            rulesList.value = getAllRules()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rule Set") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)) {
            DropdownSelect(
                label = "Select Field",
                items = ruleFields,
                selectedItem = selectedRuleField,
                itemToString = { field -> field?.displayName + " (" + field?.dataType + ")" }
            )

            OutlinedTextField(
                value = regexInput.value,
                onValueChange = { regexInput.value = it },
                label = { Text("Enter Regex") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            Button(
                onClick = ruleSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Submit Rule")
            }

            if (showDialog.value) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog.value = false
                    },
                    title = { Text("Submission Status") },
                    text = { Text(dialogMessage.value) },
                    confirmButton = {
                        Button(onClick = { showDialog.value = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)) {
                items(rulesList.value) { rule ->
                    RuleItem(rule = rule, onDelete = { ruleId -> deleteRuleAndRefresh(ruleId) })
                }
            }
        }
    }
}

@Composable
fun RuleItem(rule: DatabaseManager.Rule, onDelete: (Int) -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp), elevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {

                    Text("Field: ${rule.objectField}", style = MaterialTheme.typography.subtitle1)
                    Text("Regex: ${rule.regex}", style = MaterialTheme.typography.body1)

                    IconButton(onClick = { rule.ruleId?.let { onDelete(it) } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Rule")
                    }

            }
        }
    }
}
