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
import com.koine.mesh.ModuleConfigProtos
import com.koine.mesh.R
import com.koine.mesh.copy
import com.koine.mesh.moduleConfig
import com.koine.mesh.ui.components.EditTextPreference
import com.koine.mesh.ui.components.PreferenceCategory
import com.koine.mesh.ui.components.PreferenceFooter
import com.koine.mesh.ui.components.SwitchPreference
import com.koine.mesh.ui.radioconfig.RadioConfigViewModel

@Composable
fun PaxcounterConfigScreen(
    viewModel: RadioConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()

    if (state.responseState.isWaiting()) {
        PacketResponseStateDialog(
            state = state.responseState,
            onDismiss = viewModel::clearPacketResponse,
        )
    }

    PaxcounterConfigItemList(
        paxcounterConfig = state.moduleConfig.paxcounter,
        enabled = state.connected,
        onSaveClicked = { paxcounterConfigInput ->
            val config = moduleConfig { paxcounter = paxcounterConfigInput }
            viewModel.setModuleConfig(config)
        }
    )
}

@Suppress("LongMethod")
@Composable
fun PaxcounterConfigItemList(
    paxcounterConfig: ModuleConfigProtos.ModuleConfig.PaxcounterConfig,
    enabled: Boolean,
    onSaveClicked: (ModuleConfigProtos.ModuleConfig.PaxcounterConfig) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var paxcounterInput by rememberSaveable { mutableStateOf(paxcounterConfig) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { PreferenceCategory(text = stringResource(R.string.paxcounter_config)) }

        item {
            SwitchPreference(
                title = stringResource(R.string.paxcounter_enabled),
                checked = paxcounterInput.enabled,
                enabled = enabled,
                onCheckedChange = {
                    paxcounterInput = paxcounterInput.copy { this.enabled = it }
                }
            )
        }
        item { Divider() }

        item {
            EditTextPreference(
                title = stringResource(R.string.update_interval_seconds),
                value = paxcounterInput.paxcounterUpdateInterval,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = {
                    paxcounterInput = paxcounterInput.copy { paxcounterUpdateInterval = it }
                }
            )
        }

        item {
            EditTextPreference(
                title = stringResource(R.string.wifi_rssi_threshold_defaults_to_80),
                value = paxcounterInput.wifiThreshold,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = {
                    paxcounterInput = paxcounterInput.copy { wifiThreshold = it }
                }
            )
        }

        item {
            EditTextPreference(
                title = stringResource(R.string.ble_rssi_threshold_defaults_to_80),
                value = paxcounterInput.bleThreshold,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = {
                    paxcounterInput = paxcounterInput.copy { bleThreshold = it }
                }
            )
        }

        item {
            PreferenceFooter(
                enabled = enabled && paxcounterInput != paxcounterConfig,
                onCancelClicked = {
                    focusManager.clearFocus()
                    paxcounterInput = paxcounterConfig
                },
                onSaveClicked = {
                    focusManager.clearFocus()
                    onSaveClicked(paxcounterInput)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PaxcounterConfigPreview() {
    PaxcounterConfigItemList(
        paxcounterConfig = ModuleConfigProtos.ModuleConfig.PaxcounterConfig.getDefaultInstance(),
        enabled = true,
        onSaveClicked = { },
    )
}
