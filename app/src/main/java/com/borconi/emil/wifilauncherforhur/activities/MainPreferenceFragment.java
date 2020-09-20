package com.borconi.emil.wifilauncherforhur.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.services.WifiService;

import java.util.Set;
import java.util.stream.Collectors;

public class MainPreferenceFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_ENABLE_BT = 90;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference bluetoothDevicesPreference = preferenceScreen.findPreference("selected_bluetooth_devices");

        if (bluetoothDevicesPreference != null) {
            bluetoothDevicesPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                MultiSelectListPreference bluetoothDevicesPref = (MultiSelectListPreference) preference;
                bluetoothDevicesPref.setValues((Set<String>) newValue);
                setBluetoothDevicesSummary(bluetoothDevicesPref);
                return true;
            });

            bluetoothDevicesPreference.setOnPreferenceClickListener(preference -> {
                tryPopulateBluetoothDevices((MultiSelectListPreference) preference);
                return true;
            });
        }

        WifiService.addStatusChangedListener((isRunning, isConnected) -> {
            // Checks if activity is available because this method can be called anytime (because of WifiService listener).
            // If it's not on screen this fragment will not be attached.
            if (getActivity() == null) {
                return;
            };
            getActivity().runOnUiThread(this::updateServiceStatusPreference);
        });

        tryPopulateBluetoothDevices((MultiSelectListPreference) bluetoothDevicesPreference);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        updateServiceStatusPreference();

        MultiSelectListPreference bluetoothDevices = preferenceScreen.findPreference("selected_bluetooth_devices");
        setBluetoothDevicesSummary(bluetoothDevices);
        //Preference permissionsStatusPreference = preferenceScreen.findPreference("permissions_status");
    }

    protected void updateServiceStatusPreference() {

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference serviceStatusPreference = preferenceScreen.findPreference("service_status");
        if (serviceStatusPreference != null) {
            if (WifiService.isRunning()) {
                if (WifiService.isConnected()) {
                    serviceStatusPreference.setTitle(getString(R.string.service_running_connected));
                    serviceStatusPreference.setIcon(R.drawable.ic_green_done_24);
                } else {
                    serviceStatusPreference.setTitle(getString(R.string.service_running_looking_hur));
                    serviceStatusPreference.setIcon(R.drawable.ic_black_sync_24);
                }
            } else {
                serviceStatusPreference.setTitle(getString(R.string.service_not_running));
                serviceStatusPreference.setIcon(R.drawable.ic_red_report_problem_24);
            }
        }
    }

    protected void tryPopulateBluetoothDevices(MultiSelectListPreference preference) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && !adapter.isEnabled()) {
            Intent intentOpenBluetoothSettings = new Intent();
            intentOpenBluetoothSettings.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intentOpenBluetoothSettings, REQUEST_ENABLE_BT);
        }
        String[] entries = new String[]{adapter != null && adapter.isEnabled()
                ? getString(R.string.settings_bluetooth_selected_bluetooth_devices_no_devices)
                : getString(R.string.settings_bluetooth_selected_bluetooth_devices_bt_off)};
        String[] entryValues = new String[]{""};
        if (adapter != null && adapter.getBondedDevices().size() > 0) {
            entries = adapter.getBondedDevices().stream().map(BluetoothDevice::getName).toArray(String[]::new);
            entryValues = adapter.getBondedDevices().stream().map(BluetoothDevice::getAddress).toArray(String[]::new);
        }

        preference.setEntries(entries);
        preference.setEntryValues(entryValues);
    }

    protected void setBluetoothDevicesSummary(MultiSelectListPreference bluetoothDevices) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            bluetoothDevices.setSummary(R.string.settings_bluetooth_selected_bluetooth_devices_turn_on);
            return;
        }
        if (bluetoothDevices != null) {
            if (bluetoothDevices.getValues() != null &&
                    bluetoothDevices.getEntries() != null && bluetoothDevices.getValues().size() > 0) {
                bluetoothDevices.setSummary(bluetoothDevices.getValues().stream()
                        .map(v -> bluetoothDevices.getEntries()[bluetoothDevices.findIndexOfValue(v)]).collect(Collectors.joining(", ")));
            } else {
                bluetoothDevices.setSummary(R.string.settings_bluetooth_selected_bluetooth_devices_description);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            MultiSelectListPreference bluetoothDevices = preferenceScreen.findPreference("selected_bluetooth_devices");
            tryPopulateBluetoothDevices(bluetoothDevices);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}