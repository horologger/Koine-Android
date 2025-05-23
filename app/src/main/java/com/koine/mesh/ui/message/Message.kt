/*
 * Copyright (c) 2025 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.koine.mesh.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koine.mesh.DataPacket
import com.koine.mesh.R
import com.koine.mesh.android.Logging
import com.koine.mesh.database.entity.QuickChatAction
import com.koine.mesh.model.Node
import com.koine.mesh.model.UIViewModel
import com.koine.mesh.model.getChannel
import com.koine.mesh.navigation.navigateToNavGraph
import com.koine.mesh.ui.components.BaseScaffold
import com.koine.mesh.ui.components.NodeKeyStatusIcon
import com.koine.mesh.ui.components.NodeMenuAction
import com.koine.mesh.ui.message.components.MessageList
import com.koine.mesh.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.util.Log

private const val MESSAGE_CHARACTER_LIMIT = 200

internal fun FragmentManager.navigateToMessages(contactKey: String, message: String = "") {
    val messagesFragment = MessagesFragment().apply {
        arguments = bundleOf("contactKey" to contactKey, "message" to message)
    }
    beginTransaction()
        .add(R.id.mainActivityLayout, messagesFragment)
        .addToBackStack(null)
        .commit()
}

@AndroidEntryPoint
class MessagesFragment : Fragment(), Logging {
    private val model: UIViewModel by activityViewModels()

    private fun navigateToMessages(node: Node) = node.user.let { user ->
        val hasPKC = model.ourNodeInfo.value?.hasPKC == true && node.hasPKC // TODO use meta.hasPKC
        val channel = if (hasPKC) DataPacket.PKC_CHANNEL_INDEX else node.channel
        val contactKey = "$channel${user.id}"
        info("calling MessagesFragment filter: $contactKey")
        parentFragmentManager.navigateToMessages(contactKey)
    }

    private fun navigateToNodeDetails(nodeNum: Int) {
        info("calling NodeDetails --> destNum: $nodeNum")
        parentFragmentManager.navigateToNavGraph(nodeNum, "NodeDetails")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contactKey = arguments?.getString("contactKey").toString()
        val message = arguments?.getString("message").toString()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    MessageScreen(
                        contactKey = contactKey,
                        message = message,
                        viewModel = model,
                        navigateToMessages = ::navigateToMessages,
                        navigateToNodeDetails = ::navigateToNodeDetails,
                    ) { parentFragmentManager.popBackStack() }
                }
            }
        }
    }
}

sealed class MessageMenuAction {
    data object ClipboardCopy : MessageMenuAction()
    data object Delete : MessageMenuAction()
    data object Dismiss : MessageMenuAction()
    data object SelectAll : MessageMenuAction()
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun MessageScreen(
    contactKey: String,
    message: String,
    viewModel: UIViewModel = hiltViewModel(),
    navigateToMessages: (Node) -> Unit,
    navigateToNodeDetails: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val channelIndex = contactKey[0].digitToIntOrNull()
    val nodeId = contactKey.substring(1)
    val channelName = channelIndex?.let { viewModel.channels.value.getChannel(it)?.name }
        ?: "Unknown Channel"

    val title = when (nodeId) {
        DataPacket.ID_BROADCAST -> channelName
        else -> viewModel.getUser(nodeId).longName
    }
    val mismatchKey =
        DataPacket.PKC_CHANNEL_INDEX == channelIndex && viewModel.getNode(nodeId).mismatchKey

//    if (channelIndex != DataPacket.PKC_CHANNEL_INDEX && nodeId != DataPacket.ID_BROADCAST) {
//        subtitle = "(ch: $channelIndex - $channelName)"
//    }

    val selectedIds = rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val inSelectionMode by remember { derivedStateOf { selectedIds.value.isNotEmpty() } }

    val connState by viewModel.connectionState.collectAsStateWithLifecycle()
    val quickChat by viewModel.quickChatActions.collectAsStateWithLifecycle()
    val messages by viewModel.getMessagesFrom(contactKey).collectAsStateWithLifecycle(listOf())

    val messageInput = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(message))
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteMessageDialog(
            size = selectedIds.value.size,
            onConfirm = {
                viewModel.deleteMessages(selectedIds.value.toList())
                selectedIds.value = emptySet()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    var showNFCPrompt by remember { mutableStateOf(false) }
    val nfcStatus by viewModel.nfcStatus.collectAsStateWithLifecycle()
    var processing by remember { mutableStateOf(false) }
    var pendingMessage by remember { mutableStateOf("") }

    val showPinPrompt by viewModel.showPinPrompt.collectAsStateWithLifecycle()
    val pinPromptMessage by viewModel.pinPromptMessage.collectAsStateWithLifecycle()

    if (showNFCPrompt) {
        NFCPromptDialog(
            onDismiss = {
                showNFCPrompt = false
                processing = false
                pendingMessage = ""
            },
            onScanClick = {
                processing = true
                viewModel.sendMessageWithCallback(
                    pendingMessage,
                    contactKey,
                    onDone = {
                        processing = false
                        showNFCPrompt = false
                        pendingMessage = ""
                    }
                )
            },
            status = nfcStatus,
            processing = processing
        )
    }

    if (showPinPrompt) {
        PinPromptDialog(
            onDismiss = { viewModel.handlePinEntry("") },
            onPinEntered = { viewModel.handlePinEntry(it) },
            error = null
        )
    }

    BaseScaffold(
        topBar = {
            if (inSelectionMode) {
                ActionModeTopBar(selectedIds.value) { action ->
                    when (action) {
                        MessageMenuAction.ClipboardCopy -> coroutineScope.launch {
                            val copiedText = messages
                                .filter { it.uuid in selectedIds.value }
                                .joinToString("\n") { it.text }

                            clipboardManager.setText(AnnotatedString(copiedText))
                            selectedIds.value = emptySet()
                        }

                        MessageMenuAction.Delete -> {
                            showDeleteDialog = true
                        }

                        MessageMenuAction.Dismiss -> selectedIds.value = emptySet()
                        MessageMenuAction.SelectAll -> {
                            if (selectedIds.value.size == messages.size) {
                                selectedIds.value = emptySet()
                            } else {
                                selectedIds.value = messages.map { it.uuid }.toSet()
                            }
                        }
                    }
                }
            } else {
                MessageTopBar(title, channelIndex, mismatchKey, onNavigateBack)
            }
        },
        bottomBar = {
            val isConnected = connState.isConnected()
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
            ) {
                QuickChatRow(isConnected, quickChat) { action ->
                    if (action.mode == QuickChatAction.Mode.Append) {
                        val originalText = messageInput.value.text
                        if (!originalText.contains(action.message)) {
                            val needsSpace =
                                !originalText.endsWith(' ') && originalText.isNotEmpty()
                            val newText = buildString {
                                append(originalText)
                                if (needsSpace) append(' ')
                                append(action.message)
                            }.take(MESSAGE_CHARACTER_LIMIT)
                            messageInput.value = TextFieldValue(newText, TextRange(newText.length))
                        }
                    } else {
                        viewModel.sendMessage(action.message, contactKey)
                    }
                }
                TextInput(
                    enabled = isConnected,
                    message = messageInput,
                    onShowNFCPrompt = {
                        pendingMessage = it
                        showNFCPrompt = true
                    }
                ) { viewModel.sendMessage(it, contactKey) }
            }
        }
    ) {
        if (messages.isNotEmpty()) {
            MessageList(
                messages = messages,
                selectedIds = selectedIds,
                onUnreadChanged = { viewModel.clearUnreadCount(contactKey, it) },
                onSendReaction = { emoji, id -> viewModel.sendReaction(emoji, id, contactKey) },
            ) { action ->
                when (action) {
                    is NodeMenuAction.Remove -> viewModel.removeNode(action.node.num)
                    is NodeMenuAction.Ignore -> viewModel.ignoreNode(action.node)
                    is NodeMenuAction.Favorite -> viewModel.favoriteNode(action.node)
                    is NodeMenuAction.DirectMessage -> navigateToMessages(action.node)
                    is NodeMenuAction.RequestUserInfo -> viewModel.requestUserInfo(action.node.num)
                    is NodeMenuAction.RequestPosition -> viewModel.requestPosition(action.node.num)
                    is NodeMenuAction.TraceRoute -> viewModel.requestTraceroute(action.node.num)
                    is NodeMenuAction.MoreDetails -> navigateToNodeDetails(action.node.num)
                }
            }
        }
    }
}

@Composable
private fun DeleteMessageDialog(
    size: Int,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val deleteMessagesString = pluralStringResource(R.plurals.delete_messages, size, size)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.background,
        text = {
            Text(
                text = deleteMessagesString,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ActionModeTopBar(
    selectedList: Set<Long>,
    onAction: (MessageMenuAction) -> Unit,
) = TopAppBar(
    title = { Text(text = selectedList.size.toString()) },
    navigationIcon = {
        IconButton(onClick = { onAction(MessageMenuAction.Dismiss) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.clear),
            )
        }
    },
    actions = {
        IconButton(onClick = { onAction(MessageMenuAction.ClipboardCopy) }) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(id = R.string.copy)
            )
        }
        IconButton(onClick = { onAction(MessageMenuAction.Delete) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(id = R.string.delete)
            )
        }
        IconButton(onClick = { onAction(MessageMenuAction.SelectAll) }) {
            Icon(
                imageVector = Icons.Default.SelectAll,
                contentDescription = stringResource(id = R.string.select_all)
            )
        }
    },
    backgroundColor = MaterialTheme.colors.primary,
)

@Composable
private fun MessageTopBar(
    title: String,
    channelIndex: Int?,
    mismatchKey: Boolean = false,
    onNavigateBack: () -> Unit
) = TopAppBar(
    title = { Text(text = title) },
    navigationIcon = {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.navigate_back),
            )
        }
    },
    actions = {
        if (channelIndex == DataPacket.PKC_CHANNEL_INDEX) {
            NodeKeyStatusIcon(hasPKC = true, mismatchKey = mismatchKey)
        }
    }
)

@Composable
private fun QuickChatRow(
    enabled: Boolean,
    actions: List<QuickChatAction>,
    modifier: Modifier = Modifier,
    onClick: (QuickChatAction) -> Unit
) {
    val alertAction = QuickChatAction(
        name = "🔔",
        message = "🔔 ${stringResource(R.string.alert_bell_text)} \u0007",
        mode = QuickChatAction.Mode.Append,
        position = -1
    )

    LazyRow(
        modifier = modifier,
    ) {
        items(listOf(alertAction) + actions, key = { it.uuid }) { action ->
            Button(
                onClick = { onClick(action) },
                modifier = Modifier.padding(horizontal = 4.dp),
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(id = R.color.colorMyMsg),
                )
            ) {
                Text(
                    text = action.name,
                )
            }
        }
    }
}

@Composable
private fun NFCPromptDialog(
    onDismiss: () -> Unit,
    onScanClick: () -> Unit,
    status: String = "",
    processing: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.nfc_prompt_title)) },
        text = {
            Column {
                Text(stringResource(id = R.string.nfc_prompt_message))
                if (status.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onScanClick,
                enabled = !processing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                if (processing) {
                    androidx.compose.material.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colors.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(id = R.string.scan_card),
                    color = MaterialTheme.colors.onPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun PinPromptDialog(
    onDismiss: () -> Unit,
    onPinEntered: (String) -> Unit,
    error: String? = null
) {
    var isProcessing by remember { mutableStateOf(false) }
    val hardcodedPin = "qqqq"

    AlertDialog(
        onDismissRequest = {
            if (!isProcessing) {
                onDismiss()
            }
        },
        title = { Text("Enter PIN") },
        text = {
            Column {
                Text("Using hard-coded PIN: $hardcodedPin")
                Spacer(modifier = Modifier.height(8.dp))
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessing = true
                    Log.d("PinPromptDialog", "OK button clicked, using hard-coded PIN: '$hardcodedPin'")
                    onPinEntered(hardcodedPin)
                },
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colors.onPrimary
                    )
                } else {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                enabled = !isProcessing
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TextInput(
    enabled: Boolean,
    message: MutableState<TextFieldValue>,
    onShowNFCPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxSize: Int = MESSAGE_CHARACTER_LIMIT,
    onClick: (String) -> Unit = {}
) = Column(modifier) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = message.value,
            onValueChange = {
                if (it.text.toByteArray().size <= maxSize) {
                    message.value = it
                }
            },
            modifier = Modifier
                .weight(1f)
                .onFocusEvent { isFocused = it.isFocused },
            enabled = enabled,
            placeholder = { Text(stringResource(id = R.string.send_text)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            maxLines = 3,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = {
                    focusManager.clearFocus()
                onShowNFCPrompt(message.value.text)
            },
            enabled = enabled && message.value.text.isNotEmpty(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(id = R.string.send),
                tint = if (enabled && message.value.text.isNotEmpty()) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TextInputPreview() {
    AppTheme {
        TextInput(
            enabled = true,
            message = remember { mutableStateOf(TextFieldValue("")) },
            onShowNFCPrompt = { _ -> }
        )
    }
}
