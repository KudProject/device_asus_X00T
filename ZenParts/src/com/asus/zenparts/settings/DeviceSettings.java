/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asus.zenparts.settings;

import android.content.res.Resources;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.content.Intent;
import android.os.Bundle;
import com.asus.zenparts.KernelControl;
import com.asus.zenparts.R;
import com.asus.zenparts.utils.FileUtils;
import android.util.Log;
import android.text.TextUtils;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;


public class DeviceSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_SRGB_SWITCH = "srgb";
    public static final String KEY_DCI_SWITCH = "dci";
    private static final String KEY_CATEGORY_GRAPHICS = "graphics";
    public static final String SLIDER_SWAP_NODE = "/proc/s1302/key_rep";
    public static final String KEYCODE_SLIDER_TOP = "slider_top";
    public static final String KEYCODE_SLIDER_MIDDLE = "slider_middle";
    public static final String KEYCODE_SLIDER_BOTTOM = "slider_bottom";
    public static final String BUTTON_EXTRA_KEY_MAPPING = "/sys/devices/virtual/switch/tri-state-key/state";
    public static final String SLIDER_DEFAULT_VALUE = "5,1,0";
    final String KEY_DEVICE_DOZE = "device_doze";
    final String KEY_DEVICE_DOZE_PACKAGE_NAME = "org.lineageos.settings.doze";
    public static final String KEY_PROXI_SWITCH = "proxi";

    private TwoStatePreference mSliderSwap;
    private ListPreference mSliderModeTop;
    private ListPreference mSliderModeCenter;
    private ListPreference mSliderModeBottom;
    private TwoStatePreference mSRGBModeSwitch;
    private TwoStatePreference mDCIModeSwitch;
    private TwoStatePreference mProxiSwitch;

@Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_asus_parts, rootKey);

        mSliderSwap = (TwoStatePreference) findPreference("button_swap");
        mSliderSwap.setOnPreferenceChangeListener(this);

        mSliderModeTop = (ListPreference) findPreference(KEYCODE_SLIDER_TOP);
        mSliderModeTop.setOnPreferenceChangeListener(this);
        int sliderModeTop = getSliderAction(0);
        int valueIndex = mSliderModeTop.findIndexOfValue(String.valueOf(sliderModeTop));
        mSliderModeTop.setValueIndex(valueIndex);
        mSliderModeTop.setSummary(mSliderModeTop.getEntries()[valueIndex]);

        mSliderModeCenter = (ListPreference) findPreference(KEYCODE_SLIDER_MIDDLE);
        mSliderModeCenter.setOnPreferenceChangeListener(this);
        int sliderModeCenter = getSliderAction(1);
        valueIndex = mSliderModeCenter.findIndexOfValue(String.valueOf(sliderModeCenter));
        mSliderModeCenter.setValueIndex(valueIndex);
        mSliderModeCenter.setSummary(mSliderModeCenter.getEntries()[valueIndex]);

        mSliderModeBottom = (ListPreference) findPreference(KEYCODE_SLIDER_BOTTOM);
        mSliderModeBottom.setOnPreferenceChangeListener(this);
        int sliderModeBottom = getSliderAction(2);
        valueIndex = mSliderModeBottom.findIndexOfValue(String.valueOf(sliderModeBottom));
        mSliderModeBottom.setValueIndex(valueIndex);
        mSliderModeBottom.setSummary(mSliderModeBottom.getEntries()[valueIndex]);

        mProxiSwitch = (TwoStatePreference) findPreference(KEY_PROXI_SWITCH);
        mProxiSwitch.setChecked(Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.CUSTOM_DEVICE_PROXI_CHECK_ENABLED, 1) != 0);
    }

    private void setSummary(ListPreference preference, String file) {
        String keyCode;
        if ((keyCode = FileUtils.readOneLine(file)) != null) {
            preference.setValue(keyCode);
            preference.setSummary(preference.getEntry());
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mProxiSwitch) {
            Settings.System.putInt(getContext().getContentResolver(),
                    Settings.System.CUSTOM_DEVICE_PROXI_CHECK_ENABLED, mProxiSwitch.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference == mSliderSwap) {
           Boolean value = (Boolean) newValue;
          FileUtils.writeLine(KernelControl.SLIDER_SWAP_NODE, value ? "1" : "0");
         }

        if (preference == mSliderModeTop) {
            String value = (String) newValue;
            int sliderMode = Integer.valueOf(value);
            setSliderAction(0, sliderMode);
            int valueIndex = mSliderModeTop.findIndexOfValue(value);
            mSliderModeTop.setSummary(mSliderModeTop.getEntries()[valueIndex]);
        } else if (preference == mSliderModeCenter) {
            String value = (String) newValue;
            int sliderMode = Integer.valueOf(value);
            setSliderAction(1, sliderMode);
            int valueIndex = mSliderModeCenter.findIndexOfValue(value);
            mSliderModeCenter.setSummary(mSliderModeCenter.getEntries()[valueIndex]);
        } else if (preference == mSliderModeBottom) {
            String value = (String) newValue;
            int sliderMode = Integer.valueOf(value);
            setSliderAction(2, sliderMode);
            int valueIndex = mSliderModeBottom.findIndexOfValue(value);
            mSliderModeBottom.setSummary(mSliderModeBottom.getEntries()[valueIndex]);
        }
        return true;
   }

    private int getSliderAction(int position) {
        String value = Settings.System.getString(getContext().getContentResolver(),
                    BUTTON_EXTRA_KEY_MAPPING);
        final String defaultValue = SLIDER_DEFAULT_VALUE;

        if (value == null) {
            value = defaultValue;
        } else if (value.indexOf(",") == -1) {
            value = defaultValue;
        }
        try {
            String[] parts = value.split(",");
            return Integer.valueOf(parts[position]);
        } catch (Exception e) {
        }
        return 0;
    }

    private void setSliderAction(int position, int action) {
        String value = Settings.System.getString(getContext().getContentResolver(),
                    BUTTON_EXTRA_KEY_MAPPING);
        final String defaultValue = SLIDER_DEFAULT_VALUE;

        if (value == null) {
            value = defaultValue;
        } else if (value.indexOf(",") == -1) {
            value = defaultValue;
        }
        try {
            String[] parts = value.split(",");
            parts[position] = String.valueOf(action);
            String newValue = TextUtils.join(",", parts);
            Settings.System.putString(getContext().getContentResolver(),
                    BUTTON_EXTRA_KEY_MAPPING, newValue);
        } catch (Exception e) {
     }
  }
}
