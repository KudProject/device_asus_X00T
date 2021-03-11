package com.asus.zenparts.preferences.vibration;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;

import androidx.preference.PreferenceManager;

import com.asus.zenparts.Utils;

public class VibrationUtils {

    private static final long[] testVibrationPattern = {0, 250};
    private static final String defaultVibrationStrength = "2276";
    private final Context mContext;
    private final Vibrator mVibrator;

    public VibrationUtils(Context context) {
        mContext = context;
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public boolean isSupported(String vibrationPath) {
        return Utils.fileWritable(vibrationPath);
    }

    public void restore(String vibrationPath, String vibrationKey) {
        if (!isSupported(vibrationPath))
            return;

        String storedValue = PreferenceManager.getDefaultSharedPreferences(mContext).getString(vibrationKey, defaultVibrationStrength);
        Utils.writeValue(vibrationPath, storedValue);
    }

    public String getVibrationStrength(String vibrationPath) {
        return Utils.getFileValue(vibrationPath, defaultVibrationStrength);
    }

    void setVibrationStrength(String vibrationPath, String newValue, String vibrationKey) {
        Utils.writeValue(vibrationPath, newValue);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putString(vibrationKey, newValue);
        editor.apply();
        mVibrator.vibrate(testVibrationPattern, -1);
    }
}
