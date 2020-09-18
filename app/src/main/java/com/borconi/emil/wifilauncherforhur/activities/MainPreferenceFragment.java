package com.borconi.emil.wifilauncherforhur.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.borconi.emil.wifilauncherforhur.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainPreferenceFragment extends PreferenceFragmentCompat {
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
        ll.setId(ViewCompat.generateViewId());
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
    public void onDisplayPreferenceDialog(Preference preference) {
        super.onDisplayPreferenceDialog(preference);

        switch (preference.getKey()) {
            case "mac":
                MultiSelectListPreference mslp = (MultiSelectListPreference)preference;
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
                mslp.setEntries(entries);
                mslp.setEntryValues(entriesValues);
            default:
                // do nothing for now.
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}