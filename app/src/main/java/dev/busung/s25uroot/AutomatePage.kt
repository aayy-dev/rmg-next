package dev.busung.s25uroot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val ACTION_INSTALL = "dev.aayy.rmg.action.INSTALL"
private const val PACKAGE = "dev.aayy.rmg"

private enum class RunType { Payload, Universal }
private enum class PayloadSourceOption { Online, Local, Auto }
private enum class FallbackOption { Online, Local, None }

@Composable
internal fun AutomatePage(padding: PaddingValues) {
    val context = LocalContext.current
    var runType by remember { mutableStateOf(RunType.Payload) }
    var payloadSource by remember { mutableStateOf(PayloadSourceOption.Online) }
    var localPath by remember { mutableStateOf("") }
    var fallback by remember { mutableStateOf(FallbackOption.Local) }

    LazyColumn(
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + NAV_BAR_HEIGHT + 16.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.automate_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.automate_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        item {
            AdbCommandCard(
                runType = runType,
                onRunTypeChanged = { runType = it },
                payloadSource = payloadSource,
                onPayloadSourceChanged = { payloadSource = it },
                localPath = localPath,
                onLocalPathChanged = { localPath = it },
                fallback = fallback,
                onFallbackChanged = { fallback = it },
                context = context,
            )
        }

        item { TaskerGuideCard() }

        item { ExtrasReferenceCard() }
    }
}

@Composable
private fun AdbCommandCard(
    runType: RunType,
    onRunTypeChanged: (RunType) -> Unit,
    payloadSource: PayloadSourceOption,
    onPayloadSourceChanged: (PayloadSourceOption) -> Unit,
    localPath: String,
    onLocalPathChanged: (String) -> Unit,
    fallback: FallbackOption,
    onFallbackChanged: (FallbackOption) -> Unit,
    context: Context,
) {
    val command = buildCommand(runType, payloadSource, localPath, fallback)

    SectionCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.automate_adb_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Run type
            OptionRow(label = stringResource(R.string.automate_run_type)) {
                FilterChip(
                    selected = runType == RunType.Payload,
                    onClick = { onRunTypeChanged(RunType.Payload) },
                    label = { Text(stringResource(R.string.automate_payload_run)) },
                    colors = chipColors(),
                )
                FilterChip(
                    selected = runType == RunType.Universal,
                    onClick = { onRunTypeChanged(RunType.Universal) },
                    label = { Text(stringResource(R.string.automate_universal_run)) },
                    colors = chipColors(),
                )
            }

            // Payload source (only for payload runs)
            if (runType == RunType.Payload) {
                OptionRow(label = stringResource(R.string.automate_payload_source)) {
                    PayloadSourceOption.entries.forEach { option ->
                        FilterChip(
                            selected = payloadSource == option,
                            onClick = { onPayloadSourceChanged(option) },
                            label = { Text(option.label()) },
                            colors = chipColors(),
                        )
                    }
                }

                // Local path field
                AnimatedVisibility(
                    visible = payloadSource == PayloadSourceOption.Local ||
                        payloadSource == PayloadSourceOption.Auto,
                ) {
                    OutlinedTextField(
                        value = localPath,
                        onValueChange = onLocalPathChanged,
                        label = { Text(stringResource(R.string.automate_local_path)) },
                        placeholder = { Text("/data/local/tmp/exploit.so") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                }

                // Fallback
                OptionRow(label = stringResource(R.string.automate_fallback)) {
                    FallbackOption.entries.forEach { option ->
                        FilterChip(
                            selected = fallback == option,
                            onClick = { onFallbackChanged(option) },
                            label = { Text(option.label()) },
                            colors = chipColors(),
                        )
                    }
                }
            }

            // Command preview
            CommandPreview(command)

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", command))
                        Toast.makeText(context, R.string.automate_copied, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.automate_copy))
                }
                OutlinedButton(
                    onClick = { launchTestIntent(context, runType, payloadSource, localPath, fallback) },
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.automate_test))
                }
            }
        }
    }
}

@Composable
private fun TaskerGuideCard() {
    val context = LocalContext.current
    SectionCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.automate_tasker_title),
                style = MaterialTheme.typography.titleMedium,
            )
            data class TaskerStep(val label: String, val copyValue: String? = null)
            val steps = listOf(
                TaskerStep("Create a new Task"),
                TaskerStep("Add Action → System → Send Intent"),
                TaskerStep("Action:", ACTION_INSTALL),
                TaskerStep("Package:", "dev.aayy.rmg"),
                TaskerStep("Class:", "dev.busung.s25uroot.InstallActivity"),
                TaskerStep("Target:", "Activity"),
                TaskerStep("Add extras as key=value pairs (see reference below)"),
            )
            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(24.dp),
                    )
                    if (step.copyValue != null) {
                        Text(
                            text = "${step.label} ",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        CopyableField(context, step.copyValue)
                    } else {
                        Text(
                            text = step.label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyableField(context: Context, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", value))
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ExtrasReferenceCard() {
    var expanded by remember { mutableStateOf(false) }
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron",
    )

    SectionCard {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.automate_extras_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    extras.forEachIndexed { index, extra ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                        ExtraRow(extra)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraRow(extra: ExtraInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = extra.name,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${extra.type} — ${extra.description}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (extra.values.isNotEmpty()) {
            Text(
                text = "Values: ${extra.values.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun CommandPreview(command: String) {
    Text(
        text = command,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        ),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
    )
}

@Composable
private fun OptionRow(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun PayloadSourceOption.label(): String = when (this) {
    PayloadSourceOption.Online -> stringResource(R.string.automate_source_online)
    PayloadSourceOption.Local -> stringResource(R.string.automate_source_local)
    PayloadSourceOption.Auto -> stringResource(R.string.automate_source_auto)
}

@Composable
private fun FallbackOption.label(): String = when (this) {
    FallbackOption.Online -> stringResource(R.string.automate_source_online)
    FallbackOption.Local -> stringResource(R.string.automate_source_local)
    FallbackOption.None -> stringResource(R.string.automate_fallback_none)
}

private fun buildCommand(
    runType: RunType,
    payloadSource: PayloadSourceOption,
    localPath: String,
    fallback: FallbackOption,
): String = buildString {
    append("adb shell am start")
    append(" \\\n  -a ").append(ACTION_INSTALL)
    append(" \\\n  -p ").append(PACKAGE)
    if (runType == RunType.Universal) {
        append(" \\\n  --ez universal_root true")
    } else {
        append(" \\\n  --es payload_source \"").append(payloadSource.name.lowercase()).append('"')
        if ((payloadSource == PayloadSourceOption.Local || payloadSource == PayloadSourceOption.Auto) &&
            localPath.isNotBlank()
        ) {
            append(" \\\n  --es local_payload_path \"").append(localPath).append('"')
        }
        append(" \\\n  --es fallback_source \"").append(fallback.name.lowercase()).append('"')
    }
}

private fun launchTestIntent(
    context: Context,
    runType: RunType,
    payloadSource: PayloadSourceOption,
    localPath: String,
    fallback: FallbackOption,
) {
    val intent = Intent(ACTION_INSTALL).apply {
        setClassName(context.packageName, "dev.busung.s25uroot.InstallActivity")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runType == RunType.Universal) {
            putExtra("universal_root", true)
        } else {
            putExtra("payload_source", payloadSource.name.lowercase())
            if ((payloadSource == PayloadSourceOption.Local || payloadSource == PayloadSourceOption.Auto) &&
                localPath.isNotBlank()
            ) {
                putExtra("local_payload_path", localPath)
            }
            putExtra("fallback_source", fallback.name.lowercase())
        }
    }
    context.startActivity(intent)
}

private data class ExtraInfo(
    val name: String,
    val type: String,
    val description: String,
    val values: List<String> = emptyList(),
)

private val extras = listOf(
    ExtraInfo(
        name = "install_request_id",
        type = "String",
        description = "UUID to guard against duplicate launches. Auto-generated if omitted.",
    ),
    ExtraInfo(
        name = "profile_id",
        type = "String",
        description = "Payload profile to use. Omit for automatic detection.",
    ),
    ExtraInfo(
        name = "payload_source",
        type = "String",
        description = "Where to get the payload.",
        values = listOf("online", "local", "auto"),
    ),
    ExtraInfo(
        name = "local_payload_path",
        type = "String",
        description = "Absolute path to a .so file on the device.",
    ),
    ExtraInfo(
        name = "fallback_source",
        type = "String",
        description = "What to try if the primary source fails.",
        values = listOf("online", "local", "none"),
    ),
    ExtraInfo(
        name = "universal_root",
        type = "Boolean",
        description = "Trigger the universal root flow instead of a payload run.",
    ),
    ExtraInfo(
        name = "universal_root_flavor",
        type = "String",
        description = "Which KernelSU to install for a universal run.",
    ),
    ExtraInfo(
        name = "universal_root_tier",
        type = "String",
        description = "Which payload tier the universal run stages.",
    ),
)
