package com.semstress.mobile.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.semstress.mobile.common.Flag
import com.semstress.mobile.common.MutableFeatureFlags
import javax.inject.Inject

private const val DEBUG_MOVES_STEP = 5

class RealDebugMenuHost @Inject constructor() : DebugMenuHost {
    override val isAvailable: Boolean = true

    @Composable
    override fun Menu(
        state: DebugMenuState,
        actions: DebugMenuActions,
        featureFlags: MutableFeatureFlags,
        onDismiss: () -> Unit
    ) {
        if (!state.visible) {
            return
        }

        var seedText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Debug menu") },
            text = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { actions.onAddMoves(DEBUG_MOVES_STEP) }) {
                        Text("+$DEBUG_MOVES_STEP moves")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = actions.onForceWin) {
                        Text("Skip stage (force win)")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedTextField(
                        value = seedText,
                        onValueChange = { seedText = it.filter { char -> char.isDigit() } },
                        label = { Text("Board seed") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { seedText.toLongOrNull()?.let(actions.onReshuffleWithSeed) },
                        enabled = seedText.toLongOrNull() != null
                    ) {
                        Text("Reshuffle with seed")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Flag.entries.forEach { flag -> FlagRow(flag, featureFlags) }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun FlagRow(flag: Flag, featureFlags: MutableFeatureFlags) {
    var enabled by remember { mutableStateOf(featureFlags.isEnabled(flag)) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = flag.name, modifier = Modifier.padding(end = 8.dp))
        Switch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                featureFlags.setOverride(flag, it)
            }
        )
    }
}
