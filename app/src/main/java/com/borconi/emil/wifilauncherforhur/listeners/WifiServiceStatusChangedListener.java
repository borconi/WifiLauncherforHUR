package com.borconi.emil.wifilauncherforhur.listeners;

public interface WifiServiceStatusChangedListener {
    void OnStatusChanged(boolean isRunning, boolean isConnected);
}
