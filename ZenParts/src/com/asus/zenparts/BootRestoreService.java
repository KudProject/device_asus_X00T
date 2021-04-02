package com.asus.zenparts;

import android.content.Context;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.asus.zenparts.preferences.vibration.VibrationConstants;
import com.asus.zenparts.preferences.vibration.VibrationUtils;
import com.asus.zenparts.settings.ScreenOffGesture;

public class BootRestoreService extends Service {

    private static final String TAG = "ZenParts: BootRestoreService";
    // vars
    private SharedPreferences sharedPrefs, screenOffGesturesPref;
    private VibrationUtils vibrationUtils;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand: " + "Called");

        vibrationUtils = new VibrationUtils(this);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        screenOffGesturesPref = getSharedPreferences(
                ScreenOffGesture.GESTURE_SETTINGS, Context.MODE_PRIVATE); // Must be same as in SceenOffGestures
        restorePreferences();

        return super.onStartCommand(intent, flags, startId);
    }

    private void restorePreferences() {
        // Restore all preferences
        restoreGestureControl();
        restoreVibrationStrength();
        restoreMiscPrefs();

        stopSelf();
    }

    private void restoreGestureControl() {
        GestureNodeControl.enableGestures(
                screenOffGesturesPref.getBoolean(ScreenOffGesture.PREF_GESTURE_ENABLE, true));
    }

    private void restoreVibrationStrength() {
        for (int i = 0; i <= VibrationConstants.vibrationPaths.length - 1; i++) {
            vibrationUtils.restore(VibrationConstants.vibrationPaths[i], VibrationConstants.vibrationKeys[i]);
        }
    }

    private void restoreMiscPrefs() {
	// Restore sound control
        int gain = sharedPrefs.getInt(DeviceSettings.PREF_HEADPHONE_GAIN, 0);
        FileUtils.setValue(DeviceSettings.HEADPHONE_GAIN_PATH, gain + " " + gain);
        FileUtils.setValue(DeviceSettings.MICROPHONE_GAIN_PATH, sharedPrefs.getInt(DeviceSettings.PREF_MICROPHONE_GAIN, 0));

	// Restore fps info
        boolean enabled = sharedPrefs.getBoolean(DeviceSettings.PREF_KEY_FPS_INFO, false);
        if (enabled)
            startService(new Intent(this, FPSInfoService.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

