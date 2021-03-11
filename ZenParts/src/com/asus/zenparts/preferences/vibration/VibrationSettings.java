package com.asus.zenparts.preferences.vibration;

import android.os.Bundle;

import androidx.preference.PreferenceFragment;

public class VibrationSettings extends PreferenceFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(com.asus.zenparts.R.xml.preferences_vibration_control, rootKey);
    }
}
