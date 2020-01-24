package com.borconi.emil.wifilauncherforhur;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.scottyab.rootbeer.RootBeer;

public class MainActivity extends PreferenceActivity  {


    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 12345;
    private static AlertDialog errorwindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkpermission();
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();



    }
    @Override
    protected void onResume(){
        super.onResume();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.permission_denied));
                builder.setMessage(getResources().getString(R.string.need_draw_over_other_apps));
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
                        dialog.dismiss();
                    }
                });
                if (errorwindow==null || !errorwindow.isShowing())
                    errorwindow=builder.show();



            }
        }
    }

    protected void checkpermission() {
        String [] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, permissions, 200);



       /*
       Intent intent1=new Intent(this,WifiListener.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent1);
        }
        else
            startService(intent1);
            */

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 200) {
            // for each permission check if the user granted/denied them
            // you may want to group the rationale in a single dialog,
            // this is just an example
            for (int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission
                    boolean showRationale = shouldShowRequestPermissionRationale( permission );
                    if (! showRationale) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getResources().getString(R.string.permission_denied));
                        builder.setMessage(getResources().getString(R.string.permission_denied_loc));
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                checkpermission();
                                dialog.dismiss();
                            }
                        });

                        builder.show();


                        // user also CHECKED "never ask again"
                        // you can either enable some fall back,
                        // disable features of your app
                        // or open another dialog explaining
                        // again the permission and directing to
                        // the app setting
                    } else if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
                        checkpermission();
                        // user did NOT check "never ask again"
                        // this is a good place to explain the user
                        // why you need the permission and ask if he wants
                        // to accept it (the rationale)
                    }
                }
            }
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment     {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);

            ConstraintLayout ll = new ConstraintLayout(getActivity());
            ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            FloatingActionButton fab = new FloatingActionButton(getActivity());
            fab.setImageResource(R.drawable.ic_exit_to_app_black_24dp);
            fab.setSize(FloatingActionButton.SIZE_NORMAL);
            fab.setId(0);
            fab.setTag("fab");
            ll.setId(1);
            ll.addView(fab);
            fab.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(ll);

            constraintSet.connect(fab.getId(), ConstraintSet.END, ll.getId(), ConstraintSet.END, 75);
            constraintSet.connect(fab.getId(), ConstraintSet.BOTTOM, ll.getId(), ConstraintSet.BOTTOM, 75);
            constraintSet.applyTo(ll);
            ViewGroup viewGroup = (ViewGroup) ((ViewGroup) getActivity()
                    .findViewById(android.R.id.content)).getChildAt(0);
            v.addView(ll);
            return v;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);


            MultiSelectListPreference ms = (MultiSelectListPreference) findPreference("mac");
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            String[] entries=null;
            String[] entriesValues=null;
            if (adapter != null) {
                entries=new String[adapter.getBondedDevices().size()];
                entriesValues=new String[adapter.getBondedDevices().size()];
                int i=0;
                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    Log.d("BT",device.toString());
                   entriesValues[i]=device.getAddress();
                   entries[i]=device.getName();
                   i++;
                }
            }

            if (adapter == null || adapter.getBondedDevices().size() <= 0) {
                entries=new String[1];
                entriesValues=new String[1];
                entriesValues[0]="";
                entries[0]="No paired BT device";
            }
            ms.setEntries(entries);
            ms.setEntryValues(entriesValues);

        }

        @Override
        public void onResume(){
            super.onResume();
            SwitchPreference headunitserver=(SwitchPreference) findPreference("startserver");
            RootBeer rootBeer = new RootBeer(getActivity());
            if (!rootBeer.isRootedWithoutBusyBoxCheck())
                try {
                    headunitserver.setEnabled(false);
                }
            catch (Exception e){}

            SwitchPreference tether = (SwitchPreference) findPreference("tether");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    Log.d("HU", "Pref Version");
                    PreferenceScreen screen = getPreferenceScreen();
                    screen.removePreference(tether);
                    screen.removePreference(headunitserver);
                }
                catch (Exception e){}
            }
        }




    }

  /*  @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> x = wifi.getConfiguredNetworks();
        List<String> savednetwork=new ArrayList<>();

        for (WifiConfiguration a:x){
            if (a.SSID.startsWith("\"") && a.SSID.endsWith("\""))
                a.SSID = a.SSID.substring(1, a.SSID.length() - 1);
            savednetwork.add(a.SSID);

        }
        ArrayAdapter<String> awifidapter = new ArrayAdapter<String>
                (this,android.R.layout.select_dialog_item,savednetwork);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        ArrayList<BluetoothListPreference> devicelist = new ArrayList<>();
        if (adapter != null) {
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                devicelist.add(new BluetoothListPreference(device.getAddress(),device.getName()));
            }
        }

        if (adapter == null || adapter.getBondedDevices().size() <= 0) {
            devicelist.add(new BluetoothListPreference("","No device available"));
        }

        ArrayAdapter<BluetoothListPreference> arrayAdapter = new ArrayAdapter<BluetoothListPreference>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, devicelist);
        Spinner btspinner=findViewById(R.id.bluetooth_mac);
        btspinner.setAdapter(arrayAdapter);
        btspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                ((TextView) view).setTextColor(Color.RED);
                BluetoothListPreference btdevice = (BluetoothListPreference) parent.getSelectedItem();
                Log.d("AA-GAteWay",btdevice.toString());

                if (!btdevice.toString().equalsIgnoreCase("No device available"))
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("mac",btdevice.getMac()).commit();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        btspinner.setSelection(arrayAdapter.getPosition(new BluetoothListPreference(PreferenceManager.getDefaultSharedPreferences(this).getString("mac",""),"")));//Optional to set the selected item.


        AutoCompleteTextView et=findViewById(R.id.ssid);
        et.setThreshold(1);
        et.setAdapter(awifidapter);
        et.setText(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("ssid",""));
        et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("ssid",((EditText)view).getText().toString()).commit();
                return false;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        String [] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, permissions, 200);

       /* Intent intent1=new Intent(this,WifiListener.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent1);
        }
        else
            startService(intent1);

    }
    public void exit(View view) {
        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("ssid",((EditText)findViewById(R.id.ssid)).getText().toString()).commit();
        finish();
    }*/
}
