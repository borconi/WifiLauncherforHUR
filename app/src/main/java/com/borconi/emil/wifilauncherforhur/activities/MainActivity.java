package com.borconi.emil.wifilauncherforhur.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.WiFiLauncherServiceWidget;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_WiFiLauncher);
        super.onCreate(savedInstanceState);


        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void onWidgetClick(View view) {

        Intent intent = new Intent(WiFiLauncherServiceWidget.WIDGET_ACTION);
        sendBroadcast(intent);
    }
}
