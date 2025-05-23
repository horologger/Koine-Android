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
fun AmbientLightingConfigScreen(
    viewModel: RadioConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()

    if (state.responseState.isWaiting()) {
        PacketResponseStateDialog(
            state = state.responseState,
            onDismiss = viewModel::clearPacketResponse,
        )
    }

    AmbientLightingConfigItemList(
        ambientLightingConfig = state.moduleConfig.ambientLighting,
        enabled = state.connected,
        onSaveClicked = { ambientLightingInput ->
            val config = moduleConfig { ambientLighting = ambientLightingInput }
            viewModel.setModuleConfig(config)
        }
    )
}

@Composable
fun AmbientLightingConfigItemList(
    ambientLightingConfig: ModuleConfigProtos.ModuleConfig.AmbientLightingConfig,
    enabled: Boolean,
    onSaveClicked: (ModuleConfigProtos.ModuleConfig.AmbientLightingConfig) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var ambientLightingInput by rememberSaveable { mutableStateOf(ambientLightingConfig) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { PreferenceCategory(text = stringResource(R.string.ambient_lighting_config)) }

        item {
            SwitchPreference(
                title = stringResource(R.string.led_state),
                checked = ambientLightingInput.ledState,
                enabled = enabled,
                onCheckedChange = {
                    ambientLightingInput = ambientLightingInput.copy { ledState = it }
                }
            )
        }
        item { Divider() }

        item {
            EditTextPreference(
                title = stringResource(R.string.current),
                value = ambientLightingInput.current,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = {
                    ambientLightingInput = ambientLightingInput.copy { current = it }
                }
            )
        }

        item {
            EditTextPreference(
                title = stringResource(R.string.red),
                value = ambientLightingInput.red,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = { ambientLightingInput = ambientLightingInput.copy { red = it } }
            )
        }

        item {
            EditTextPreference(
                title = stringResource(R.string.green),
                value = ambientLightingInput.green,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = { ambientLightingInput = ambientLightingInput.copy { green = it } }
            )
        }

        item {
            EditTextPreference(
                title = stringResource(R.string.blue),
                value = ambientLightingInput.blue,
                enabled = enabled,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = { ambientLightingInput = ambientLightingInput.copy { blue = it } }
            )
        }

        item {
            PreferenceFooter(
                enabled = enabled && ambientLightingInput != ambientLightingConfig,
                onCancelClicked = {
                    focusManager.clearFocus()
                    ambientLightingInput = ambientLightingConfig
                },
                onSaveClicked = {
                    focusManager.clearFocus()
                    onSaveClicked(ambientLightingInput)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AmbientLightingConfigPreview() {
    AmbientLightingConfigItemList(
        ambientLightingConfig = ModuleConfigProtos.ModuleConfig.AmbientLightingConfig.getDefaultInstance(),
        enabled = true,
        onSaveClicked = { },
    )
}
