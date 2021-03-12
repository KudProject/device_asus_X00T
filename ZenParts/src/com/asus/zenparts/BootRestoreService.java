package com.asus.zenparts;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.asus.zenparts.kcal.KcalConstants;
import com.asus.zenparts.preferences.vibration.VibrationConstants;
import com.asus.zenparts.preferences.vibration.VibrationUtils;
import com.asus.zenparts.settings.ScreenOffGesture;

public class BootRestoreService extends Service implements KcalConstants {

    private static final String TAG = "ZenParts: BootRestoreService";
    // vars
    private SharedPreferences sharedPrefs;
    private VibrationUtils vibrationUtils;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand: " + "Called");

        vibrationUtils = new VibrationUtils(this);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        restorePreferences();

        return super.onStartCommand(intent, flags, startId);
    }

    private void restorePreferences() {
        // Restore all preferences
        restoreGestureControl();
        restoreVibrationStrength();
        restoreKCal();
        restoreMiscPrefs();

        stopSelf();
    }

    private void restoreGestureControl() {
        GestureNodeControl.enableGestures(
                sharedPrefs.getBoolean(ScreenOffGesture.PREF_GESTURE_ENABLE, true));
    }

    private void restoreVibrationStrength() {
        for (int i = 0; i <= VibrationConstants.vibrationPaths.length - 1; i++) {
            vibrationUtils.restore(VibrationConstants.vibrationPaths[i], VibrationConstants.vibrationKeys[i]);
        }
    }

    private void restoreKCal() {
        // KCal disabled, return
        if (Settings.Secure.getInt(getContentResolver(), PREF_ENABLED, 0) != 1)
            return;


        FileUtils.setValue(KCAL_ENABLE, Settings.Secure.getInt(getContentResolver(),
                PREF_ENABLED, 0));

        String rgbValue = Settings.Secure.getInt(getContentResolver(),
                PREF_RED, RED_DEFAULT) + " " +
                Settings.Secure.getInt(getContentResolver(), PREF_GREEN,
                        GREEN_DEFAULT) + " " +
                Settings.Secure.getInt(getContentResolver(), PREF_BLUE,
                        BLUE_DEFAULT);

        FileUtils.setValue(KCAL_RGB, rgbValue);
        FileUtils.setValue(KCAL_MIN, Settings.Secure.getInt(getContentResolver(),
                PREF_MINIMUM, MINIMUM_DEFAULT));
        FileUtils.setValue(KCAL_SAT, Settings.Secure.getInt(getContentResolver(),
                PREF_GRAYSCALE, 0) == 1 ? 128 :
                Settings.Secure.getInt(getContentResolver(),
                        PREF_SATURATION, SATURATION_DEFAULT) + SATURATION_OFFSET);
        FileUtils.setValue(KCAL_VAL, Settings.Secure.getInt(getContentResolver(),
                PREF_VALUE, VALUE_DEFAULT) + VALUE_OFFSET);
        FileUtils.setValue(KCAL_CONT, Settings.Secure.getInt(getContentResolver(),
                PREF_CONTRAST, CONTRAST_DEFAULT) + CONTRAST_OFFSET);
        FileUtils.setValue(KCAL_HUE, Settings.Secure.getInt(getContentResolver(),
                PREF_HUE, HUE_DEFAULT));
    }

    private void restoreMiscPrefs() {
        // Paths
        String TORCH_1_BRIGHTNESS_PATH = "/sys/devices/soc/800f000.qcom,spmi/spmi-0/" +
                "spmi0-03/800f000.qcom,spmi:qcom,pm660l@3:qcom,leds@d300/leds/led:torch_0/" +
                "max_brightness";
        String TORCH_2_BRIGHTNESS_PATH = "/sys/devices/soc/800f000.qcom,spmi/spmi-0/" +
                "spmi0-03/800f000.qcom,spmi:qcom,pm660l@3:qcom,leds@d300/leds/led:torch_1/" +
                "max_brightness";
        String HEADPHONE_GAIN_PATH = "/sys/kernel/sound_control/headphone_gain";
        String MICROPHONE_GAIN_PATH = "/sys/kernel/sound_control/mic_gain";

        // Restore preferences
        FileUtils.setValue(TORCH_1_BRIGHTNESS_PATH,
                Settings.Secure.getInt(getContentResolver(),
                        DeviceSettings.PREF_TORCH_BRIGHTNESS, 100));
        FileUtils.setValue(TORCH_2_BRIGHTNESS_PATH,
                Settings.Secure.getInt(getContentResolver(),
                        DeviceSettings.PREF_TORCH_BRIGHTNESS, 100));
        int gain = Settings.Secure.getInt(getContentResolver(),
                DeviceSettings.PREF_HEADPHONE_GAIN, 0);
        FileUtils.setValue(HEADPHONE_GAIN_PATH, gain + " " + gain);
        FileUtils.setValue(MICROPHONE_GAIN_PATH, Settings.Secure.getInt(getContentResolver(),
                DeviceSettings.PREF_MICROPHONE_GAIN, 0));
        FileUtils.setValue(DeviceSettings.BACKLIGHT_DIMMER_PATH, Settings.Secure.getInt(getContentResolver(),
                DeviceSettings.PREF_BACKLIGHT_DIMMER, 0));

        boolean enabled = sharedPrefs.getBoolean(DeviceSettings.PREF_KEY_FPS_INFO, false);
        if (enabled)
            startService(new Intent(this, FPSInfoService.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

