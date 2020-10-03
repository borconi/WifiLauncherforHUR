package com.borconi.emil.wifilauncherforhur.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.services.PermissionService;

public class MainActivity extends AppCompatActivity {

    private PermissionService permissionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_WiFiLauncher);
        super.onCreate(savedInstanceState);

        permissionService = new PermissionService(this);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        permissionService.requestAllPermissions();
    }
}
