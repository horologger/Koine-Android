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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.koine.mesh.MeshProtos
import com.koine.mesh.R
import com.koine.mesh.copy
import com.koine.mesh.ui.components.EditTextPreference
import com.koine.mesh.ui.components.PreferenceCategory
import com.koine.mesh.ui.components.PreferenceFooter
import com.koine.mesh.ui.components.RegularPreference
import com.koine.mesh.ui.components.SwitchPreference
import com.koine.mesh.ui.radioconfig.RadioConfigViewModel
import com.koine.mesh.user

@Composable
fun UserConfigScreen(
    viewModel: RadioConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()

    if (state.responseState.isWaiting()) {
        PacketResponseStateDialog(
            state = state.responseState,
            onDismiss = viewModel::clearPacketResponse,
        )
    }

    UserConfigItemList(
        userConfig = state.userConfig,
        enabled = true,
        onSaveClicked = viewModel::setOwner,
    )
}

@Composable
fun UserConfigItemList(
    userConfig: MeshProtos.User,
    enabled: Boolean,
    onSaveClicked: (MeshProtos.User) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var userInput by rememberSaveable { mutableStateOf(userConfig) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { PreferenceCategory(text = stringResource(R.string.user_config)) }

        item {
            RegularPreference(
                title = stringResource(R.string.node_id),
                subtitle = userInput.id,
                onClick = {}
            )
        }
        item { Divider() }

        item {
            EditTextPreference(
                title = stringResource(R.string.long_name),
                value = userInput.longName,
                maxSize = 39, // long_name max_size:40
                enabled = enabled,
                isError = userInput.longName.isEmpty(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = {
                    userInput = userInput.copy { longName = it }
                }
            )
        }

        item {
            EditTextPreference(
                title = stringResource(R.string.short_name),
                value = userInput.shortName,
                maxSize = 4, // short_name max_size:5
                enabled = enabled,
                isError = userInput.shortName.isEmpty(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                onValueChanged = { userInput = userInput.copy { shortName = it } }
            )
        }

        item {
            RegularPreference(
                title = stringResource(R.string.hardware_model),
                subtitle = userInput.hwModel.name,
                onClick = {}
            )
        }
        item { Divider() }

        item {
            SwitchPreference(
                title = stringResource(R.string.licensed_amateur_radio),
                checked = userInput.isLicensed,
                enabled = enabled,
                onCheckedChange = { userInput = userInput.copy { isLicensed = it } }
            )
        }
        item { Divider() }

        item {
            PreferenceFooter(
                enabled = enabled && userInput != userConfig,
                onCancelClicked = {
                    focusManager.clearFocus()
                    userInput = userConfig
                }, onSaveClicked = {
                    focusManager.clearFocus()
                    onSaveClicked(userInput)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserConfigPreview() {
    UserConfigItemList(
        userConfig = user {
            id = "!a280d9c8"
            longName = "Meshtastic d9c8"
            shortName = "d9c8"
            hwModel = MeshProtos.HardwareModel.RAK4631
            isLicensed = false
        },
        enabled = true,
        onSaveClicked = { },
    )
}
