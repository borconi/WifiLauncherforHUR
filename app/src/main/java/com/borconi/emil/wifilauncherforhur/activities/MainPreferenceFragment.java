package com.borconi.emil.wifilauncherforhur.activities;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.borconi.emil.wifilauncherforhur.EmptyListPreference;
import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.services.WifiService;

import java.util.Set;
import java.util.stream.Collectors;

public class MainPreferenceFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_ENABLE_BT = 90;
    private boolean alertDialogOpen;
    private Preference bluetoothDevicesPreference;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

    }

    @Override
    @SuppressWarnings("unchecked")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);


        PreferenceScreen preferenceScreen = getPreferenceScreen();
        bluetoothDevicesPreference = preferenceScreen.findPreference("selected_bluetooth_devices");

        if (bluetoothDevicesPreference != null) {
            bluetoothDevicesPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                EmptyListPreference bluetoothDevicesPref = (EmptyListPreference) preference;
                bluetoothDevicesPref.setValues((Set<String>) newValue);
                setBluetoothDevicesSummary(bluetoothDevicesPref);
                return true;
            });

            bluetoothDevicesPreference.setOnPreferenceClickListener(preference -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        return false;
                    }
                    else
                        tryPopulateBluetoothDevices((EmptyListPreference) preference);
                return true;

            });
        }


        Preference startServiceManuallyPreference = preferenceScreen.findPreference("start_service_manually");
        if (startServiceManuallyPreference != null) {
            startServiceManuallyPreference.setOnPreferenceClickListener(preference -> {
                Context context = getContext();
                Intent wifiServiceIntent = new Intent(context, WifiService.class);

                if (context != null) {
                    context.startForegroundService(wifiServiceIntent);
                }

                return true;
            });
        }


        if (!alertDialogOpen && !PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("nowarning",false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Base_Theme_MaterialComponents_Dialog);

            builder.setTitle(getActivity().getResources().getString(R.string.major_title));
            builder.setMessage(getActivity().getResources().getString(R.string.major_desc));
            builder.setPositiveButton(getString(R.string.save), (dialog, id) -> {

                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("nowarning",true).apply();
                dialog.dismiss();
            });
            builder.setNegativeButton(getString(R.string.close),((dialogInterface, i) -> {dialogInterface.dismiss();}));
            builder.setOnDismissListener(dialog -> alertDialogOpen = false);
            alertDialogOpen = true;
            builder.show();
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.POST_NOTIFICATIONS});
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT});

        return v;
    }


    public void requestDrawOverlays(int msg) {


        if (!alertDialogOpen) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Base_Theme_MaterialComponents_Dialog);

            builder.setTitle(getActivity().getResources().getString(R.string.alert_permission_denied_title));
            builder.setMessage(getActivity().getResources().getString(msg));
            builder.setPositiveButton("OK", (dialog, id) -> {

                Intent intent;
                if (msg == R.string.alert_need_draw_over_other_apps)
                    intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData(Uri.parse("package:" + getActivity().getPackageName()));
                else
                    intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(Uri.parse("package:" + getActivity().getPackageName()));


                getActivity().startActivityForResult(intent, msg);
                dialog.dismiss();
            });
            builder.setOnDismissListener(dialog -> alertDialogOpen = false);
            alertDialogOpen = true;
            builder.show();
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        PreferenceScreen preferenceScreen = getPreferenceScreen();

        MultiSelectListPreference bluetoothDevices = preferenceScreen.findPreference("selected_bluetooth_devices");
        setBluetoothDevicesSummary(bluetoothDevices);
        tryPopulateBluetoothDevices((EmptyListPreference) bluetoothDevicesPreference);
        updatePermissionsStatusPreference();


    }


    protected void updatePermissionsStatusPreference() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference permissionsStatusPreference = preferenceScreen.findPreference("permissions_status");
        if (permissionsStatusPreference != null) {

            if (getPermission(false)) {
                permissionsStatusPreference.setTitle(getString(R.string.status_all_permissions_granted));
                permissionsStatusPreference.setIcon(R.drawable.ic_green_done_24);
            } else {
                permissionsStatusPreference.setTitle(getString(R.string.status_denied_permissions));
                permissionsStatusPreference.setIcon(R.drawable.ic_red_report_problem_24);
                permissionsStatusPreference.setOnPreferenceClickListener(preference -> {
                    getPermission(true);
                    return true;
                });
                getPermission(true);
            }


        }
    }



    private ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permission -> {

                permission.entrySet().forEach(entry -> {
                    System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
                    if (!entry.getValue()) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), entry.getKey()))
                            ActivityCompat.requestPermissions(getActivity(),new String[]{entry.getKey()},2);

                        else {
                            if (!entry.getValue() && entry.getKey() == Manifest.permission.BLUETOOTH_CONNECT)
                                setBluetoothDevicesSummary(null);
                        }
                    }
                    else
                        tryPopulateBluetoothDevices((MultiSelectListPreference) bluetoothDevicesPreference);
                });

            });



    private boolean getPermission(boolean show) {

        if (Settings.canDrawOverlays(getContext()) && Settings.System.canWrite(getContext())
            //&& ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            //&& (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
        )
            return true;
        else if (!show)
            return false;
            // else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            //requestPermissionLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT});
            //   return false;
            //} else if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ////  requestPermissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
            //return false;
            //}
        else if (!Settings.canDrawOverlays(getContext())) {
            requestDrawOverlays(R.string.alert_need_draw_over_other_apps);
            return false;
        } else {
            requestDrawOverlays(R.string.System_settings_desc);
            return false;
        }

    }


    protected void tryPopulateBluetoothDevices(MultiSelectListPreference preference) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && !adapter.isEnabled()) {
            Intent intentOpenBluetoothSettings = new Intent();
            intentOpenBluetoothSettings.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intentOpenBluetoothSettings, REQUEST_ENABLE_BT);
            return ;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                //  startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
                return;
            }
        if (adapter.getBondedDevices().size()==0) {
            String[] entries = new String[1];
            String[] entryValues = new String[1];
            entries[0] = "NO DEVICES";
            entryValues[0]=null;
        }
        String[] entries = new String[adapter.getBondedDevices().size()];
        String[] entryValues = new String[adapter.getBondedDevices().size()];
        int i = 0;
        for (BluetoothDevice dev : adapter.getBondedDevices()) {
            if (dev.getName() == null || "".equalsIgnoreCase(dev.getName()))
                entries[i] = "UNKNOWN";
            else
                entries[i] = dev.getName();
            entryValues[i] = dev.getAddress();
            i++;
        }



        preference.setEntries(entries);
        preference.setEntryValues(entryValues);

    }

    protected void setBluetoothDevicesSummary(MultiSelectListPreference bluetoothDevices) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothDevices.setSummary(R.string.nobt);
                return;
            }

        if (adapter == null || !adapter.isEnabled()) {
            bluetoothDevices.setSummary(R.string.settings_bluetooth_selected_bluetooth_devices_turn_on);
            return;
        }
        if (bluetoothDevices != null) {
            Set<String> values = bluetoothDevices.getValues().stream()
                    .filter(v -> !v.equalsIgnoreCase("")).collect(Collectors.toSet());

            if (values.size() > 0) {
                bluetoothDevices.setSummary(values.stream()
                        .map(v -> {
                            int indexOfValue = bluetoothDevices.findIndexOfValue(v);
                            if (indexOfValue >= 0) {
                                return bluetoothDevices.getEntries()[indexOfValue];
                            }
                            return getString(R.string.settings_bluetooth_selected_bluetooth_devices_forgotten_device);
                        }).collect(Collectors.joining(", ")));
            } else {
                bluetoothDevices.setSummary(R.string.settings_bluetooth_selected_bluetooth_devices_description);
            }
        }
    }


    @Override
    public void onStop() {
        super.onStop();

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
}