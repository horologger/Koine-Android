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

package com.koine.mesh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChargingStation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.koine.mesh.R
import com.koine.mesh.model.DeviceHardware
import com.koine.mesh.model.MetricsState
import com.koine.mesh.model.MetricsViewModel
import com.koine.mesh.model.Node
import com.koine.mesh.navigation.Route
import com.koine.mesh.ui.components.PreferenceCategory
import com.koine.mesh.ui.preview.NodePreviewParameterProvider
import com.koine.mesh.ui.radioconfig.NavCard
import com.koine.mesh.ui.theme.AppTheme
import com.koine.mesh.util.UnitConversions.calculateDewPoint
import com.koine.mesh.util.UnitConversions.toTempString
import com.koine.mesh.util.formatAgo
import com.koine.mesh.util.formatUptime
import com.koine.mesh.util.thenIf
import com.koine.mesh.util.toSpeedString

private enum class LogsType(
    val titleRes: Int,
    val icon: ImageVector,
    val route: Route
) {
    DEVICE(R.string.device_metrics_log, Icons.Default.ChargingStation, Route.DeviceMetrics),
    NODE_MAP(R.string.node_map, Icons.Default.Map, Route.NodeMap),
    POSITIONS(R.string.position_log, Icons.Default.LocationOn, Route.PositionLog),
    ENVIRONMENT(R.string.env_metrics_log, Icons.Default.Thermostat, Route.EnvironmentMetrics),
    SIGNAL(R.string.sig_metrics_log, Icons.Default.SignalCellularAlt, Route.SignalMetrics),
    POWER(R.string.power_metrics_log, Icons.Default.Power, Route.PowerMetrics),
    TRACEROUTE(R.string.traceroute_log, Icons.Default.Route, Route.TracerouteLog)
}

@Composable
fun NodeDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: MetricsViewModel = hiltViewModel(),
    onNavigate: (Route) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val environmentState by viewModel.environmentState.collectAsStateWithLifecycle()

    /* The order is with respect to the enum above: LogsType */
    val availabilities = remember(key1 = state, key2 = environmentState) {
        booleanArrayOf(
            state.hasDeviceMetrics(),
            state.hasPositionLogs(),
            state.hasPositionLogs(),
            environmentState.hasEnvironmentMetrics(),
            state.hasSignalMetrics(),
            state.hasPowerMetrics(),
            state.hasTracerouteLogs())
    }

    if (state.node != null) {
        val node = state.node ?: return
        NodeDetailList(
            node = node,
            metricsState = state,
            onNavigate = onNavigate,
            modifier = modifier,
            metricsAvailability = availabilities
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun NodeDetailList(
    modifier: Modifier = Modifier,
    node: Node,
    metricsState: MetricsState,
    onNavigate: (Route) -> Unit = {},
    metricsAvailability: BooleanArray
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (metricsState.deviceHardware != null) {
            item {
                PreferenceCategory(stringResource(R.string.device)) {
                    DeviceDetailsContent(metricsState)
                }
            }
        }
        item {
            PreferenceCategory(stringResource(R.string.details)) {
                NodeDetailsContent(node)
            }
        }

        if (node.hasEnvironmentMetrics) {
            item {
                PreferenceCategory(stringResource(R.string.environment))
                EnvironmentMetrics(node, metricsState.isFahrenheit)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (node.hasPowerMetrics) {
            item {
                PreferenceCategory(stringResource(R.string.power))
                PowerMetrics(node)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        /* Metric Logs Navigation */
        item {
            PreferenceCategory(stringResource(id = R.string.logs))
            for (type in LogsType.entries) {
                NavCard(
                    title = stringResource(type.titleRes),
                    icon = type.icon,
                    enabled = metricsAvailability[type.ordinal]
                ) {
                    onNavigate(type.route)
                }
            }
        }

        if (!metricsState.isManaged) {
            item {
                PreferenceCategory(stringResource(id = R.string.administration))
                NavCard(
                    title = stringResource(id = R.string.remote_admin),
                    icon = Icons.Default.Settings,
                    enabled = true
                ) {
                    onNavigate(Route.RadioConfig(node.num))
                }
            }
        }
    }
}

@Composable
private fun NodeDetailRow(
    label: String,
    icon: ImageVector,
    value: String,
    iconTint: Color = MaterialTheme.colors.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
        Text(label)
        Spacer(modifier = Modifier.weight(1f))
        Text(textAlign = TextAlign.End, text = value)
    }
}

@Composable
private fun DeviceDetailsContent(
    state: MetricsState,
) {
    val node = state.node ?: return
    val deviceHardware = state.deviceHardware ?: return
    val hwModelName = deviceHardware.displayName
    val isSupported = deviceHardware.activelySupported
    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                color = Color(node.colors.second).copy(alpha = .5f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        DeviceHardwareImage(
            deviceHardware = deviceHardware,
            modifier = Modifier
                .size(100.dp)
        )
    }
    NodeDetailRow(
        label = stringResource(R.string.hardware),
        icon = Icons.Default.Router,
        value = hwModelName
    )
    if (isSupported) {
        NodeDetailRow(
            label = stringResource(R.string.supported),
            icon = Icons.Default.Verified,
            value = "",
            iconTint = Color.Green
        )
    }
}

@Composable
fun DeviceHardwareImage(
    deviceHardware: DeviceHardware,
    modifier: Modifier = Modifier,
) {
    val hwImg = deviceHardware.images?.lastOrNull()
    if (hwImg != null) {
        val imageUrl = "file:///android_asset/device_hardware/$hwImg"
        AsyncImage(
            model = imageUrl,
            contentScale = ContentScale.Inside,
            contentDescription = deviceHardware.displayName,
            placeholder = painterResource(R.drawable.hw_unknown),
            error = painterResource(R.drawable.hw_unknown),
            fallback = painterResource(R.drawable.hw_unknown),
            modifier = modifier
                .padding(16.dp)
        )
    }
}

@Composable
private fun NodeDetailsContent(
    node: Node,
) {
    if (node.mismatchKey) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.KeyOff,
                contentDescription = stringResource(id = R.string.encryption_error),
                tint = Color.Red,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.encryption_error),
                style = MaterialTheme.typography.h6.copy(color = Color.Red),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.encryption_error_text),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
    }
    NodeDetailRow(
        label = stringResource(R.string.node_number),
        icon = Icons.Default.Numbers,
        value = node.num.toUInt().toString()
    )
    NodeDetailRow(
        label = stringResource(R.string.user_id),
        icon = Icons.Default.Person,
        value = node.user.id
    )
    NodeDetailRow(
        label = stringResource(R.string.role),
        icon = Icons.Default.Work,
        value = node.user.role.name
    )
    if (node.deviceMetrics.uptimeSeconds > 0) {
        NodeDetailRow(
            label = stringResource(R.string.uptime),
            icon = Icons.Default.CheckCircle,
            value = formatUptime(node.deviceMetrics.uptimeSeconds)
        )
    }
    if (node.metadata != null) {
        NodeDetailRow(
            label = stringResource(R.string.firmware_version),
            icon = Icons.Default.Memory,
            value = node.metadata.firmwareVersion.substringBeforeLast(".")
        )
    }
    NodeDetailRow(
        label = stringResource(R.string.node_sort_last_heard),
        icon = Icons.Default.History,
        value = formatAgo(node.lastHeard)
    )
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    text: String,
    value: String,
    rotateIcon: Float = 0f,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier
            .padding(4.dp)
            .widthIn(min = 100.dp, max = 150.dp)
            .heightIn(min = 100.dp, max = 150.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier
                    .size(24.dp)
                    .thenIf(rotateIcon != 0f) { rotate(rotateIcon) },
            )
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.subtitle2
            )
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (value.length < 7) {
                    MaterialTheme.typography.h5
                } else {
                    MaterialTheme.typography.h6
                },
            )
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun EnvironmentMetrics(
    node: Node,
    isFahrenheit: Boolean = false,
) = with(node.environmentMetrics) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (hasTemperature()) {
            InfoCard(
                icon = Icons.Default.Thermostat,
                text = stringResource(R.string.temperature),
                value = temperature.toTempString(isFahrenheit)
            )
        }
        if (hasRelativeHumidity()) {
            InfoCard(
                icon = Icons.Default.WaterDrop,
                text = stringResource(R.string.humidity),
                value = "%.0f%%".format(relativeHumidity)
            )
        }
        if (hasTemperature() && hasRelativeHumidity()) {
            val dewPoint = calculateDewPoint(temperature, relativeHumidity)
            InfoCard(
                icon = ImageVector.vectorResource(R.drawable.ic_outlined_dew_point_24),
                text = stringResource(R.string.dew_point),
                value = dewPoint.toTempString(isFahrenheit)
            )
        }
        if (hasBarometricPressure()) {
            InfoCard(
                icon = Icons.Default.Speed,
                text = stringResource(R.string.pressure),
                value = "%.0f hPa".format(barometricPressure)
            )
        }
        if (hasGasResistance()) {
            InfoCard(
                icon = Icons.Default.BlurOn,
                text = stringResource(R.string.gas_resistance),
                value = "%.0f MΩ".format(gasResistance)
            )
        }
        if (hasVoltage()) {
            InfoCard(
                icon = Icons.Default.Bolt,
                text = stringResource(R.string.voltage),
                value = "%.2fV".format(voltage)
            )
        }
        if (hasCurrent()) {
            InfoCard(
                icon = Icons.Default.Power,
                text = stringResource(R.string.current),
                value = "%.1fmA".format(current)
            )
        }
        if (hasIaq()) {
            InfoCard(
                icon = Icons.Default.Air,
                text = stringResource(R.string.iaq),
                value = iaq.toString()
            )
        }
        if (hasDistance()) {
            InfoCard(
                icon = Icons.Default.Height,
                text = stringResource(R.string.distance),
                value = "%.0f mm".format(distance)
            )
        }
        if (hasLux()) {
            InfoCard(
                icon = Icons.Default.LightMode,
                text = stringResource(R.string.lux),
                value = "%.0f lx".format(lux)
            )
        }
        if (hasWindSpeed()) {
            @Suppress("MagicNumber")
            val normalizedBearing = (windDirection % 360 + 360) % 360
            InfoCard(
                icon = Icons.Outlined.Navigation,
                text = stringResource(R.string.wind),
                value = windSpeed.toSpeedString(),
                rotateIcon = normalizedBearing.toFloat(),
            )
        }
        if (hasWeight()) {
            InfoCard(
                icon = Icons.Default.Scale,
                text = stringResource(R.string.weight),
                value = "%.2f kg".format(weight)
            )
        }
        if (hasRadiation()) {
            InfoCard(
                icon = ImageVector.vectorResource(R.drawable.ic_filled_radioactive_24),
                text = stringResource(R.string.radiation),
                value = "%.1f µR/h".format(radiation)
            )
        }
    }
}

@Composable
private fun PowerMetrics(node: Node) = with(node.powerMetrics) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (ch1Voltage != 0f) {
            Column {
                InfoCard(
                    icon = Icons.Default.Bolt,
                    text = stringResource(R.string.channel_1),
                    value = "%.2fV".format(ch1Voltage)
                )
                InfoCard(
                    icon = Icons.Default.Power,
                    text = stringResource(R.string.channel_1),
                    value = "%.1fmA".format(ch1Current)
                )
            }
        }
        if (ch2Voltage != 0f) {
            Column {
                InfoCard(
                    icon = Icons.Default.Bolt,
                    text = stringResource(R.string.channel_2),
                    value = "%.2fV".format(ch2Voltage)
                )
                InfoCard(
                    icon = Icons.Default.Power,
                    text = stringResource(R.string.channel_2),
                    value = "%.1fmA".format(ch2Current)
                )
            }
        }
        if (ch3Voltage != 0f) {
            Column {
                InfoCard(
                    icon = Icons.Default.Bolt,
                    text = stringResource(R.string.channel_3),
                    value = "%.2fV".format(ch3Voltage)
                )
                InfoCard(
                    icon = Icons.Default.Power,
                    text = stringResource(R.string.channel_3),
                    value = "%.1fmA".format(ch3Current)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NodeDetailsPreview(
    @PreviewParameter(NodePreviewParameterProvider::class)
    node: Node
) {
    AppTheme {
        NodeDetailList(
            node = node,
            metricsState = MetricsState.Empty,
            metricsAvailability = BooleanArray(LogsType.entries.size) { false }
        )
    }
}
