package au.com.rangerai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.rangerai.bluetooth.ObdViewModel
import au.com.rangerai.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val role: String,
    val content: String,
    val isLoading: Boolean = false,
    val sources: List<AiSource> = emptyList()
)

private const val KEY_AI_BACKEND_URL = "ai_backend_url"
private const val KEY_AI_CLIENT_TOKEN = "ai_client_token"

@Composable
fun ChatScreen(obdViewModel: ObdViewModel = viewModel()) {
    val context = LocalContext.current
    val vehicleState by obdViewModel.vehicleState.collectAsState()
    val isConnected by obdViewModel.isConnected.collectAsState()
    val diagnosticTroubleCodes by obdViewModel.diagnosticTroubleCodes.collectAsState()
    val supportedMode01Pids by obdViewModel.supportedMode01Pids.collectAsState()
    val prefs = remember { context.getSharedPreferences(ObdViewModel.PREFS_NAME, 0) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var backendUrl by remember { mutableStateOf(prefs.getString(KEY_AI_BACKEND_URL, "") ?: "") }
    var clientToken by remember { mutableStateOf(prefs.getString(KEY_AI_CLIENT_TOKEN, "") ?: "") }
    var showServerDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "assistant",
                "Hi! I’m Ranger Brain. I combine your fresh live readings and DTCs with your Ranger’s history, observed baselines, indexed manuals and Ford/Haynes reference sources. Experimental Ford Mode 22 data stays clearly labelled until it is independently verified on this vehicle."
            )
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().background(DarkBackground)) {
        Row(
            Modifier.fillMaxWidth().background(SurfaceCard).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(IceBlueGlow.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SmartToy, null, tint = IceBlueGlow, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("AI Assistant", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        backendUrl.isBlank() -> "Secure AI server not configured"
                        isConnected -> "Vehicle connected • ${vehicleState.freshParameters().size} fresh readings"
                        else -> "AI server configured • no vehicle connected"
                    },
                    color = if (backendUrl.isNotBlank()) AccentGreen else AccentYellow,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = { showServerDialog = true }) {
                Icon(
                    Icons.Filled.Dns,
                    contentDescription = "AI server settings",
                    tint = if (backendUrl.isNotBlank()) IceBlueGlow else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { ChatBubble(it) }
            if (messages.size == 1) {
                item {
                    Text("Quick questions:", color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Compare my live readings with this Ranger’s previous logs",
                        "Diagnose my intermittent turbo squeal using current data",
                        "What should I check for frequent DPF regenerations?",
                        "Look up the Ford/Haynes evidence relevant to this symptom"
                    ).forEach { suggestion ->
                        SuggestionChip(
                            onClick = { inputText = suggestion },
                            label = { Text(suggestion, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = SurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = IceBlueGlow.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        Surface(color = SurfaceCard, shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about your vehicle…", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlueGlow,
                        unfocusedBorderColor = SurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = IceBlueGlow,
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated
                    ),
                    maxLines = 4,
                    enabled = !isLoading
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (inputText.isBlank() || isLoading) return@FloatingActionButton
                        val userMessage = inputText.trim()
                        val historySnapshot = messages.filter { !it.isLoading }.toList()
                        inputText = ""
                        messages += ChatMessage("user", userMessage)
                        messages += ChatMessage("assistant", "", isLoading = true)
                        isLoading = true

                        scope.launch {
                            val answer = AiClient.ask(
                                backendUrl = backendUrl,
                                clientToken = clientToken,
                                userMessage = userMessage,
                                history = historySnapshot,
                                vehicleState = vehicleState,
                                isConnected = isConnected,
                                pidRegistry = obdViewModel.pidRegistry,
                                diagnosticTroubleCodes = diagnosticTroubleCodes,
                                supportedMode01Pids = supportedMode01Pids
                            )
                            messages.removeLastOrNull()
                            messages += ChatMessage("assistant", answer.text, sources = answer.sources)
                            isLoading = false
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (inputText.isNotBlank() && !isLoading) IceBlueGlow else SurfaceElevated,
                    contentColor = if (inputText.isNotBlank() && !isLoading) Color.Black else TextMuted,
                    shape = CircleShape
                ) {
                    if (isLoading) CircularProgressIndicator(color = IceBlueGlow, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showServerDialog) {
        var tempUrl by remember(showServerDialog) { mutableStateOf(backendUrl) }
        var tempToken by remember(showServerDialog) { mutableStateOf(clientToken) }
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Secure AI Server", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "The OpenAI key stays on your backend server and is never stored in the APK. Ranger Brain can use your private indexed manuals/history plus Ford/Haynes official-source lookup. Enter the HTTPS address of the included backend.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("https://your-server.example", color = TextMuted) },
                        singleLine = true,
                        colors = serverFieldColors()
                    )
                    OutlinedTextField(
                        value = tempToken,
                        onValueChange = { tempToken = it },
                        label = { Text("Optional app token", color = TextMuted) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = serverFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    backendUrl = tempUrl.trim().trimEnd('/')
                    clientToken = tempToken.trim()
                    prefs.edit()
                        .putString(KEY_AI_BACKEND_URL, backendUrl)
                        .putString(KEY_AI_CLIENT_TOKEN, clientToken)
                        .remove("openai_api_key")
                        .apply()
                    showServerDialog = false
                }) { Text("Save", color = IceBlueGlow) }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun serverFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = IceBlueGlow,
    unfocusedBorderColor = SurfaceElevated,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(IceBlueGlow.copy(alpha = 0.2f)).align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SmartToy, null, tint = IceBlueGlow, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = if (isUser) IceBlueGlow.copy(alpha = 0.15f) else SurfaceCard)
        ) {
            if (message.isLoading) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { Box(Modifier.size(6.dp).clip(CircleShape).background(TextMuted)) }
                }
            } else {
                val uriHandler = LocalUriHandler.current
                Column(Modifier.padding(12.dp)) {
                    Text(
                        message.content,
                        color = if (isUser) IceBlueLight else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    if (!isUser && message.sources.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Sources",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        message.sources.take(6).forEach { source ->
                            if (source.url != null) {
                                TextButton(
                                    onClick = { runCatching { uriHandler.openUri(source.url) } },
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        source.title,
                                        color = IceBlueGlow,
                                        fontSize = 11.sp,
                                        maxLines = 2
                                    )
                                }
                            } else {
                                Text(
                                    "• ${source.title}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(IceBlueGlow.copy(alpha = 0.3f)).align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, tint = IceBlueGlow, modifier = Modifier.size(16.dp))
            }
        }
    }
}
