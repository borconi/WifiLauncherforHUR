package com.borconi.emil.wifilauncherforhur.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.borconi.emil.wifilauncherforhur.BuildConfig
import com.borconi.emil.wifilauncherforhur.R
import com.borconi.emil.wifilauncherforhur.services.WifiService
import com.borconi.emil.wifilauncherforhur.activities.ui.theme.WifiLauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel> {
        MainViewModel.Factory(this)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshRuntimePermissions(this)
        viewModel.refreshBluetoothDevices(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_WiFiLauncher)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WifiLauncherTheme {
                MainRoute(
                    viewModel = viewModel,
                    requestPermissions = { permissionLauncher.launch(it) },
                    openAppSettings = { openAppSettings() },
                    openOverlaySettings = { openOverlaySettings() },
                    openWriteSettings = { openWriteSettings() },
                    openBatterySettings = { openBatteryOptimizationSettings() },
                    startService = { startForegroundService(Intent(this, WifiService::class.java)) },
                    stopService = {
                        val stopIntent = Intent(this, WifiService::class.java)
                        stopIntent.action = WifiService.ACTION_FOREGROUND_STOP
                        startForegroundService(stopIntent)
                    },
                    enableBluetooth = { startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAll(this)
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun openWriteSettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun openBatteryOptimizationSettings() {
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        })
    }
}

data class PermissionItem(
    val title: String,
    val granted: Boolean,
    val action: PermissionAction,
)

enum class PermissionAction {
    REQUEST_RUNTIME,
    APP_SETTINGS,
    OVERLAY_SETTINGS,
    WRITE_SETTINGS,
    BATTERY_SETTINGS,
    ENABLE_BLUETOOTH,
}

data class BluetoothDeviceItem(
    val name: String,
    val address: String,
)

data class ConnectionModeItem(
    val value: String,
    val label: String,
)

data class MainUiState(
    val serviceRunning: Boolean = false,
    val permissionItems: List<PermissionItem> = emptyList(),
    val selectedBluetoothDevices: Set<String> = emptySet(),
    val bondedBluetoothDevices: List<BluetoothDeviceItem> = emptyList(),
    val connectionMode: String = "1",
    val hurP2pName: String = "HUR7",
    val keepRunning: Boolean = false,
    val ignoreBtDisconnect: Boolean = false,
    val showMajorChangesDialog: Boolean = false,
    val connectionModes: List<ConnectionModeItem> = emptyList(),
) {
    val allPermissionsGranted: Boolean get() = permissionItems.all { it.granted }
}

class MainViewModel(
    private val appContext: Context,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private var dismissedInSession = false

    private val _uiState = MutableStateFlow(
        MainUiState(
            connectionModes = listOf(
                ConnectionModeItem("1", appContext.getString(R.string.network_discovery)),
                ConnectionModeItem("2", appContext.getString(R.string.use_wifip2p)),
                ConnectionModeItem("3", appContext.getString(R.string.use_wifi_nearby)),
                ConnectionModeItem("4", appContext.getString(R.string.tether)),
                ConnectionModeItem("5", appContext.getString(R.string.client_mode)),
            )
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState

    fun refreshAll(context: Context) {
        val currentVersion = BuildConfig.VERSION_CODE
        val lastAcknowledgedVersion = prefs.getInt("last_acknowledged_version", 0)

        _uiState.update {
            it.copy(
                serviceRunning = WifiService.isRunning(),
                connectionMode = prefs.getString("connection_mode", "1") ?: "1",
                hurP2pName = prefs.getString("hur_p2p_name", "HUR7") ?: "HUR7",
                keepRunning = prefs.getBoolean("keep_running", false),
                ignoreBtDisconnect = prefs.getBoolean("ignore_bt_disconnect", false),
                selectedBluetoothDevices = prefs.getStringSet("selected_bluetooth_devices", emptySet()) ?: emptySet(),
                showMajorChangesDialog = !dismissedInSession && (lastAcknowledgedVersion < currentVersion),
                permissionItems = buildPermissionItems(context),
                bondedBluetoothDevices = loadBondedDevices(context),
            )
        }
    }

    fun refreshRuntimePermissions(context: Context) {
        _uiState.update { it.copy(permissionItems = buildPermissionItems(context)) }
    }

    fun refreshBluetoothDevices(context: Context) {
        _uiState.update {
            it.copy(
                bondedBluetoothDevices = loadBondedDevices(context),
                selectedBluetoothDevices = prefs.getStringSet("selected_bluetooth_devices", emptySet()) ?: emptySet(),
            )
        }
    }

    fun setConnectionMode(context: Context, value: String) {
        prefs.edit { putString("connection_mode", value) }
        if (value != _uiState.value.connectionMode) restartWifiServiceIfNeeded(context)
        refreshAll(context)
    }

    fun setHurP2pName(value: String) {
        prefs.edit { putString("hur_p2p_name", value) }
        _uiState.update { it.copy(hurP2pName = value) }
    }

    fun setKeepRunning(value: Boolean) {
        prefs.edit { putBoolean("keep_running", value) }
        _uiState.update { it.copy(keepRunning = value) }
    }

    fun setIgnoreBtDisconnect(value: Boolean) {
        prefs.edit {
            putBoolean("ignore_bt_disconnect", value)
            if (value) putBoolean("keep_running", false)
        }
        _uiState.update { it.copy(ignoreBtDisconnect = value, keepRunning = if (value) false else it.keepRunning) }
    }

    fun setSelectedBluetoothDevices(values: Set<String>) {
        prefs.edit { putStringSet("selected_bluetooth_devices", values) }
        _uiState.update { it.copy(selectedBluetoothDevices = values) }
    }

    fun dismissMajorDialog(forever: Boolean) {
        if (forever) {
            prefs.edit { putInt("last_acknowledged_version", BuildConfig.VERSION_CODE) }
        }
        dismissedInSession = true
        _uiState.update { it.copy(showMajorChangesDialog = false) }
    }

    private fun restartWifiServiceIfNeeded(context: Context) {
        if (!WifiService.isRunning()) return
        context.stopService(Intent(context, WifiService::class.java))
        context.startForegroundService(Intent(context, WifiService::class.java))
    }

    private fun buildPermissionItems(context: Context): List<PermissionItem> {
        val packageName = context.packageName
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val items = mutableListOf<PermissionItem>()

        val runtimeGranted = hasNearbyOrLocation(context)
        items += PermissionItem(
            title = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Nearby / Wi‑Fi permissions" else "Location permissions",
            granted = runtimeGranted,
            action = PermissionAction.REQUEST_RUNTIME,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            items += PermissionItem(
                title = "Bluetooth connect permissions",
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED,
                action = PermissionAction.REQUEST_RUNTIME,
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items += PermissionItem(
                title = "Notifications",
                granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
                action = PermissionAction.REQUEST_RUNTIME,
            )
        }

        items += PermissionItem(
            title = "Display over other apps",
            granted = Settings.canDrawOverlays(context),
            action = PermissionAction.OVERLAY_SETTINGS,
        )

        items += PermissionItem(
            title = "Modify system settings",
            granted = Settings.System.canWrite(context),
            action = PermissionAction.WRITE_SETTINGS,
        )

        items += PermissionItem(
            title = "Battery optimization disabled",
            granted = pm.isIgnoringBatteryOptimizations(packageName),
            action = PermissionAction.BATTERY_SETTINGS,
        )

        val adapter = BluetoothAdapter.getDefaultAdapter()
        items += PermissionItem(
            title = "Bluetooth enabled",
            granted = adapter?.isEnabled == true,
            action = PermissionAction.ENABLE_BLUETOOTH,
        )

        return items
    }

    private fun hasNearbyOrLocation(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val backgroundOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            fine && backgroundOk
        }
    }

    private fun loadBondedDevices(context: Context): List<BluetoothDeviceItem> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        return adapter.bondedDevices.orEmpty().map { device: BluetoothDevice ->
            BluetoothDeviceItem(
                name = device.name?.takeIf { it.isNotBlank() } ?: "Unknown device",
                address = device.address,
            )
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                context.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(context.applicationContext),
            ) as T
        }
    }
}

@Composable
private fun MainRoute(
    viewModel: MainViewModel,
    requestPermissions: (Array<String>) -> Unit,
    openAppSettings: () -> Unit,
    openOverlaySettings: () -> Unit,
    openWriteSettings: () -> Unit,
    openBatterySettings: () -> Unit,
    startService: () -> Unit,
    stopService: () -> Unit,
    enableBluetooth: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateCompat()

    LaunchedEffect(Unit) {
        while(true) {
            viewModel.refreshAll(context)
            delay(1500)
        }
    }

    MainScreen(
        state = state,
        onDismissMajorChanges = viewModel::dismissMajorDialog,
        onRequestPermissionFix = { action ->
            when (action) {
                PermissionAction.REQUEST_RUNTIME -> requestPermissions(runtimePermissions())
                PermissionAction.APP_SETTINGS -> openAppSettings()
                PermissionAction.OVERLAY_SETTINGS -> openOverlaySettings()
                PermissionAction.WRITE_SETTINGS -> openWriteSettings()
                PermissionAction.BATTERY_SETTINGS -> openBatterySettings()
                PermissionAction.ENABLE_BLUETOOTH -> enableBluetooth()
            }
        },
        onToggleService = {
            if (state.serviceRunning) stopService() else startService()
            viewModel.refreshAll(context)
        },
        onChangeConnectionMode = { viewModel.setConnectionMode(context, it) },
        onHurP2pNameChange = viewModel::setHurP2pName,
        onKeepRunningChange = viewModel::setKeepRunning,
        onIgnoreBtDisconnectChange = viewModel::setIgnoreBtDisconnect,
        onBluetoothSelectionChange = viewModel::setSelectedBluetoothDevices,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    state: MainUiState,
    onDismissMajorChanges: (Boolean) -> Unit,
    onRequestPermissionFix: (PermissionAction) -> Unit,
    onToggleService: () -> Unit,
    onChangeConnectionMode: (String) -> Unit,
    onHurP2pNameChange: (String) -> Unit,
    onKeepRunningChange: (Boolean) -> Unit,
    onIgnoreBtDisconnectChange: (Boolean) -> Unit,
    onBluetoothSelectionChange: (Set<String>) -> Unit,
) {
    var showBluetoothDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wi‑Fi Launcher", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Compose refresh",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    StatusPill(running = state.serviceRunning)
                }
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PermissionCard(
                    items = state.permissionItems,
                    onFix = onRequestPermissionFix,
                )
            }
            item {
                SectionCard(title = stringResource(R.string.bluetooth), icon = Icons.Rounded.Bluetooth) {
                    LabeledValue(
                        title = stringResource(R.string.settings_bluetooth_selected_bluetooth_devices_title),
                        value = selectedBluetoothSummary(state),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { showBluetoothDialog = true }) {
                        Text("Choose devices")
                    }
                }
            }
            item {
                WirelessSettingsCard(
                    state = state,
                    onChangeConnectionMode = onChangeConnectionMode,
                    onHurP2pNameChange = onHurP2pNameChange,
                    onKeepRunningChange = onKeepRunningChange,
                    onIgnoreBtDisconnectChange = onIgnoreBtDisconnectChange,
                )
            }
            item {
                SectionCard(title = "Tools", icon = Icons.Rounded.PowerSettingsNew) {
                    val containerColor by animateColorAsState(
                        if (state.serviceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        label = "buttonColor"
                    )
                    Button(
                        onClick = onToggleService,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = if (state.serviceRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (state.serviceRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (state.serviceRunning) "Stop Wi‑Fi service"
                            else stringResource(R.string.settings_advanced_options_start_service_manually_title)
                        )
                    }
                }
            }
        }
    }

    if (showBluetoothDialog) {
        BluetoothDevicePickerDialog(
            devices = state.bondedBluetoothDevices,
            selected = state.selectedBluetoothDevices,
            onDismiss = { showBluetoothDialog = false },
            onSave = {
                onBluetoothSelectionChange(it)
                showBluetoothDialog = false
            },
        )
    }

    if (state.showMajorChangesDialog) {
        AlertDialog(
            onDismissRequest = { onDismissMajorChanges(false) },
            title = { Text(stringResource(R.string.major_title)) },
            text = { Text(stringResource(R.string.major_desc)) },
            confirmButton = {
                TextButton(onClick = { onDismissMajorChanges(true) }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismissMajorChanges(false) }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun PermissionCard(items: List<PermissionItem>, onFix: (PermissionAction) -> Unit) {
    val allGranted = items.all { it.granted }
    var expanded by remember(allGranted) { mutableStateOf(!allGranted) }

    Card(shape = RoundedCornerShape(28.dp)) {
        Column(modifier = Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text(stringResource(R.string.status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (!expanded && allGranted) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { if (!item.granted) onFix(item.action) }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (item.granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (item.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Medium)
                                Text(
                                    if (item.granted) "Granted" else "Needs attention",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!item.granted) {
                                TextButton(onClick = { onFix(item.action) }) {
                                    Text("Fix")
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun WirelessSettingsCard(
    state: MainUiState,
    onChangeConnectionMode: (String) -> Unit,
    onHurP2pNameChange: (String) -> Unit,
    onKeepRunningChange: (Boolean) -> Unit,
    onIgnoreBtDisconnectChange: (Boolean) -> Unit,
) {
    SectionCard(title = stringResource(R.string.wireless), icon = Icons.Rounded.Wifi) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ConnectionModeSelector(
                modes = state.connectionModes,
                selected = state.connectionMode,
                onSelected = onChangeConnectionMode,
            )
            if (state.connectionMode == "2") {
                OutlinedTextField(
                    value = state.hurP2pName,
                    onValueChange = onHurP2pNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hur_p2p_name)) },
                    supportingText = { Text(stringResource(R.string.hur_p2p_name_desc)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                )
            }
            SwitchRow(
                title = stringResource(R.string.auto_reconnect),
                subtitle = stringResource(R.string.auto_reconnect_desc),
                checked = state.keepRunning,
                enabled = !state.ignoreBtDisconnect,
                onCheckedChange = onKeepRunningChange,
            )
            SwitchRow(
                title = stringResource(R.string.ignore_bt),
                subtitle = stringResource(R.string.ignore_bt_desc),
                checked = state.ignoreBtDisconnect,
                onCheckedChange = onIgnoreBtDisconnectChange,
            )
        }
    }
}

@Composable
private fun ConnectionModeSelector(
    modes: List<ConnectionModeItem>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val current = modes.firstOrNull { it.value == selected }

    OutlinedCard(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.wifi_connection_mode),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = current?.label.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.wifi_connection_mode)) },
            text = {
                Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                    modes.forEach { mode ->
                        val isSelected = mode.value == selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        onSelected(mode.value)
                                        showDialog = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun LabeledValue(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusPill(running: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(if (running) "Running" else "Idle") },
        leadingIcon = {
            Icon(
                imageVector = if (running) Icons.Rounded.CheckCircle else Icons.Rounded.Settings,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun BluetoothDevicePickerDialog(
    devices: List<BluetoothDeviceItem>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit,
) {
    val working = remember(selected, devices) { mutableStateListOf<String>().apply { addAll(selected) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bluetooth devices") },
        text = {
            if (devices.isEmpty()) {
                Text("No bonded devices available yet.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    devices.forEach { device ->
                        val checked = working.contains(device.address)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (checked) working.remove(device.address) else working.add(device.address)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = checked, onCheckedChange = {
                                if (it) working.add(device.address) else working.remove(device.address)
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(working.toSet()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun selectedBluetoothSummary(state: MainUiState): String {
    if (state.selectedBluetoothDevices.isEmpty()) return "No selected devices"
    val map = state.bondedBluetoothDevices.associateBy { it.address }
    return state.selectedBluetoothDevices.joinToString { map[it]?.name ?: "Forgotten device" }
}

private fun runtimePermissions(): Array<String> {
    val permissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.NEARBY_WIFI_DEVICES
        permissions += Manifest.permission.POST_NOTIFICATIONS
    } else {
        permissions += Manifest.permission.ACCESS_FINE_LOCATION
        permissions += Manifest.permission.ACCESS_COARSE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions += Manifest.permission.BLUETOOTH_CONNECT
        permissions += Manifest.permission.BLUETOOTH_SCAN
        permissions += Manifest.permission.BLUETOOTH_ADVERTISE
    }
    return permissions.distinct().toTypedArray()
}

@Composable
private fun <T> StateFlow<T>.collectAsStateCompat(): androidx.compose.runtime.State<T> =
    collectAsState(initial = value)
