package com.borconi.emil.wifilauncherforhur.tethering;

public abstract class MyOnStartTetheringCallback {
    /**
     * Called when tethering has been successfully started.
     */
    public abstract void onTetheringStarted();

    /**
     * Called when starting tethering failed.
     */
    public abstract void onTetheringFailed();
}
