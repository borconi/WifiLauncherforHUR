package com.borconi.emil.wifilauncherforhur;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;

public class EmptyListPreference extends MultiSelectListPreference {
    public EmptyListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public EmptyListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EmptyListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyListPreference(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onClick() {

        if (getEntries() == null)
            return;

        super.onClick();
    }
}
