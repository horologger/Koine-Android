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
import com.koine.mesh.ConfigProtos.Config.BluetoothConfig
import com.koine.mesh.R
import com.koine.mesh.config
import com.koine.mesh.copy
import com.koine.mesh.ui.components.DropDownPreference
import com.koine.mesh.ui.components.EditTextPreference
import com.koine.mesh.ui.components.PreferenceCategory
import com.koine.mesh.ui.components.PreferenceFooter
import com.koine.mesh.ui.components.SwitchPreference
import com.koine.mesh.ui.radioconfig.RadioConfigViewModel

@Composable
fun BluetoothConfigScreen(
    viewModel: RadioConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()

    if (state.responseState.isWaiting()) {
        PacketResponseStateDialog(
            state = state.responseState,
            onDismiss = viewModel::clearPacketResponse,
        )
    }

    BluetoothConfigItemList(
        bluetoothConfig = state.radioConfig.bluetooth,
        enabled = state.connected,
        onSaveClicked = { bluetoothInput ->
            val config = config { bluetooth = bluetoothInput }
            viewModel.setConfig(config)
        }
    )
}

@Composable
fun BluetoothConfigItemList(
    bluetoothConfig: BluetoothConfig,
    enabled: Boolean,
    onSaveClicked: (BluetoothConfig) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var bluetoothInput by rememberSaveable { mutableStateOf(bluetoothConfig) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { PreferenceCategory(text = stringResource(R.string.bluetooth_config)) }

        item {
            SwitchPreference(
                title = stringResource(R.string.bluetooth_enabled),
                checked = bluetoothInput.enabled,
                enabled = enabled,
                onCheckedChange = { bluetoothInput = bluetoothInput.copy { this.enabled = it } }
            )
        }
        item { Divider() }

        item {
            DropDownPreference(
                title = stringResource(R.string.pairing_mode),
                enabled = enabled,
                items = BluetoothConfig.PairingMode.entries
                    .filter { it != BluetoothConfig.PairingMode.UNRECOGNIZED }
                    .map { it to it.name },
                selectedItem = bluetoothInput.mode,
                onItemSelected = { bluetoothInput = bluetoothInput.copy { mode = it } }
            )
        }
        item { Divider() }

        item {
            EditTextPreference(
                title = stringResource(R.string.fixed_pin),
                value = bluetoothInput.fixedPin,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = {
                    if (it.toString().length == 6) { // ensure 6 digits
                        bluetoothInput = bluetoothInput.copy { fixedPin = it }
                    }
                }
            )
        }

        item {
            PreferenceFooter(
                enabled = enabled && bluetoothInput != bluetoothConfig,
                onCancelClicked = {
                    focusManager.clearFocus()
                    bluetoothInput = bluetoothConfig
                },
                onSaveClicked = {
                    focusManager.clearFocus()
                    onSaveClicked(bluetoothInput)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BluetoothConfigPreview() {
    BluetoothConfigItemList(
        bluetoothConfig = BluetoothConfig.getDefaultInstance(),
        enabled = true,
        onSaveClicked = { },
    )
}
