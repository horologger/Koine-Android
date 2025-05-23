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

package com.koine.mesh.ui.radioconfig.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koine.mesh.ModuleConfigProtos.ModuleConfig.RangeTestConfig
import com.koine.mesh.R
import com.koine.mesh.copy
import com.koine.mesh.moduleConfig
import com.koine.mesh.ui.components.EditTextPreference
import com.koine.mesh.ui.components.PreferenceCategory
import com.koine.mesh.ui.components.PreferenceFooter
import com.koine.mesh.ui.components.SwitchPreference
import com.koine.mesh.ui.radioconfig.RadioConfigViewModel

@Composable
fun RangeTestConfigScreen(
    viewModel: RadioConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()

    if (state.responseState.isWaiting()) {
        PacketResponseStateDialog(
            state = state.responseState,
            onDismiss = viewModel::clearPacketResponse,
        )
    }

    RangeTestConfigItemList(
        rangeTestConfig = state.moduleConfig.rangeTest,
        enabled = state.connected,
        onSaveClicked = { rangeTestInput ->
            val config = moduleConfig { rangeTest = rangeTestInput }
            viewModel.setModuleConfig(config)
        }
    )
}

@Composable
fun RangeTestConfigItemList(
    rangeTestConfig: RangeTestConfig,
    enabled: Boolean,
    onSaveClicked: (RangeTestConfig) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var rangeTestInput by rememberSaveable { mutableStateOf(rangeTestConfig) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { PreferenceCategory(text = stringResource(R.string.range_test_config)) }

        item {
            SwitchPreference(
                title = stringResource(R.string.range_test_enabled),
                checked = rangeTestInput.enabled,
                enabled = enabled,
                onCheckedChange = { rangeTestInput = rangeTestInput.copy { this.enabled = it } }
            )
        }
        item { Divider() }

        item {
            EditTextPreference(
                title = stringResource(R.string.sender_message_interval_seconds),
                value = rangeTestInput.sender,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = { rangeTestInput = rangeTestInput.copy { sender = it } }
            )
        }

        item {
            SwitchPreference(
                title = stringResource(R.string.save_csv_in_storage_esp32_only),
                checked = rangeTestInput.save,
                enabled = enabled,
                onCheckedChange = { rangeTestInput = rangeTestInput.copy { save = it } }
            )
        }
        item { Divider() }

        item {
            PreferenceFooter(
                enabled = enabled && rangeTestInput != rangeTestConfig,
                onCancelClicked = {
                    focusManager.clearFocus()
                    rangeTestInput = rangeTestConfig
                },
                onSaveClicked = {
                    focusManager.clearFocus()
                    onSaveClicked(rangeTestInput)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RangeTestConfig() {
    RangeTestConfigItemList(
        rangeTestConfig = RangeTestConfig.getDefaultInstance(),
        enabled = true,
        onSaveClicked = { },
    )
}
