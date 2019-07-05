/*
 * Copyright (C) 2014 Slimroms
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
package com.asus.zenparts;

import android.database.ContentObserver;
import android.content.BroadcastReceiver;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;
import android.service.notification.ZenModeConfig;
import com.asus.zenparts.settings.DeviceSettings;
import com.asus.zenparts.settings.ScreenOffGesture;
import android.os.UserHandle;
import com.android.internal.os.AlternativeDeviceKeyHandler;
import com.android.internal.util.ArrayUtils;
import com.asus.zenparts.utils.ActionConstants;
import com.asus.zenparts.utils.Action;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

public class KeyHandler implements AlternativeDeviceKeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();
    private static final int GESTURE_REQUEST = 1;
    private static final int GESTURE_WAKELOCK_DURATION = 2000;
    private static final boolean DEBUG = true;
    private static final String KEY_CONTROL_PATH = "/proc/s1302/virtual_key";
    private static final String FPC_CONTROL_PATH = "/sys/devices/soc/soc:fpc_fpc1020/proximity_state";

    // Supported scancodes
    private static final int GESTURE_C_SCANCODE = 249;
    private static final int GESTURE_E_SCANCODE = 250;
    private static final int GESTURE_S_SCANCODE = 251;
    private static final int GESTURE_V_SCANCODE = 252;
    private static final int GESTURE_W_SCANCODE = 253;
    private static final int GESTURE_Z_SCANCODE = 254;
    private static final int GESTURE_SWIPE_UP = 255;
    private static final int GESTURE_SWIPE_DOWN = 256;
    private static final int GESTURE_SWIPE_LEFT = 257;
    private static final int GESTURE_SWIPE_RIGHT = 258;
    private static final int GESTURE_DOUBLE_TAP = 260;

    // Slider
     private static final int KEYCODE_SLIDER_TOP = 601;
     private static final int KEYCODE_SLIDER_MIDDLE = 602;
     private static final int KEYCODE_SLIDER_BOTTOM = 603;
     private static final int KEY_DOUBLE_TAP = 143;
     private static final int[] sSupportedGestures = new int[]{
        GESTURE_C_SCANCODE,
        GESTURE_E_SCANCODE,
        GESTURE_V_SCANCODE,
        GESTURE_W_SCANCODE,
        GESTURE_S_SCANCODE,
        GESTURE_Z_SCANCODE,
	GESTURE_SWIPE_UP,
	GESTURE_SWIPE_DOWN,
	GESTURE_SWIPE_LEFT,
	GESTURE_SWIPE_RIGHT,
        GESTURE_DOUBLE_TAP,
        KEYCODE_SLIDER_TOP,
        KEYCODE_SLIDER_MIDDLE,
        KEYCODE_SLIDER_BOTTOM
    };


     private static final int[] sProxiCheckedGestures = new int[]{
        GESTURE_C_SCANCODE,
        GESTURE_E_SCANCODE,
        GESTURE_V_SCANCODE,
        GESTURE_W_SCANCODE,
        GESTURE_S_SCANCODE,
        GESTURE_Z_SCANCODE,
	GESTURE_SWIPE_UP,
	GESTURE_SWIPE_DOWN,
	GESTURE_SWIPE_LEFT,
	GESTURE_SWIPE_RIGHT,
        GESTURE_DOUBLE_TAP,
        KEY_DOUBLE_TAP
     };


    private static final int[] sHandledGestures = new int[]{
        KEYCODE_SLIDER_TOP,
        KEYCODE_SLIDER_MIDDLE,
        KEYCODE_SLIDER_BOTTOM
      };

    private final Context mContext;
    private final AudioManager mAudioManager;
    private final PowerManager mPowerManager;
    private final NotificationManager mNoMan;
    private Context mGestureContext = null;
    private EventHandler mEventHandler;
    private SensorManager mSensorManager;
    private Vibrator mVibrator;
    WakeLock mProximityWakeLock;
    private WakeLock mGestureWakeLock;
    private Handler mHandler;
    private int mCurrentPosition;
    private boolean mProxyIsNear;
    private boolean mUseProxiCheck;
    private Sensor mSensor;
    private SettingsObserver mSettingsObserver;

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                 onDisplayOn();
             } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                 onDisplayOff();
             }
         }
   };
 
   private Intent createIntent(String value) {
        ComponentName componentName = ComponentName.unflattenFromString(value);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(componentName);
        return intent;
}

    public KeyHandler(Context context) {
        mContext = context;
        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();
        mEventHandler = new EventHandler();
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mNoMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);
        mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ProximityWakeLock");
        mHandler = new Handler(); 
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GestureWakeLock");

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        try {
            mGestureContext = mContext.createPackageContext(
                    "com.asus.zenparts", Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
        }
    }

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            KeyEvent event = (KeyEvent) msg.obj;
            String action = null;
            switch(event.getScanCode()) {
            case GESTURE_C_SCANCODE:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_C,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
            case GESTURE_E_SCANCODE:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_E,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
            case GESTURE_V_SCANCODE:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_V,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
            case GESTURE_W_SCANCODE:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_W,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
            case GESTURE_S_SCANCODE:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_S,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
            case GESTURE_Z_SCANCODE:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_Z,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
			case GESTURE_SWIPE_UP:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_UP,
                        ActionConstants.ACTION_WAKE_DEVICE);
                        doHapticFeedback();
                break;
            case GESTURE_SWIPE_DOWN:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_DOWN,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
            case GESTURE_SWIPE_LEFT:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_LEFT,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
            case GESTURE_SWIPE_RIGHT:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_RIGHT,
                        ActionConstants.ACTION_NULL);
                        doHapticFeedback();
                break;
            case GESTURE_DOUBLE_TAP:
                action = getGestureSharedPreferences()
                        .getString(ScreenOffGesture.PREF_GESTURE_DOUBLE_TAP,
                        ActionConstants.ACTION_WAKE_DEVICE);
                        doHapticFeedback();
                break;
    }

            if (action == null || action != null && action.equals(ActionConstants.ACTION_NULL)) {
                return;
            }
            if (action.equals(ActionConstants.ACTION_CAMERA)
                    || !action.startsWith("**")) {
                Action.processAction(mContext, ActionConstants.ACTION_WAKE_DEVICE, false);
            }
            Action.processAction(mContext, action, false);
        }
    }

     private SensorEventListener mProximitySensor = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            mProxyIsNear = event.values[0] < mSensor.getMaximumRange();
            if (DEBUG) Log.d(TAG, "mProxyIsNear = " + mProxyIsNear);
            if(Utils.fileWritable(FPC_CONTROL_PATH)) {
                Utils.writeValue(FPC_CONTROL_PATH, mProxyIsNear ? "1" : "0");
        }
     }

    @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
      }
    };

    private void onDisplayOn() {
        if (mUseProxiCheck) {
            if (DEBUG) Log.d(TAG, "Display on");
            mSensorManager.unregisterListener(mProximitySensor, mSensor);
        }
    }

    private void onDisplayOff() {
        if (mUseProxiCheck) {
            if (DEBUG) Log.d(TAG, "Display off");
            mSensorManager.registerListener(mProximitySensor, mSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
        }
   }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(
                    Settings.System.CUSTOM_DEVICE_PROXI_CHECK_ENABLED),
                    false, this);
            update();
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        public void update() {
            mUseProxiCheck = Settings.System.getIntForUser(
                    mContext.getContentResolver(), Settings.System.CUSTOM_DEVICE_PROXI_CHECK_ENABLED, 1,
                    UserHandle.USER_CURRENT) == 1;
        }
}

    private void doHapticFeedback() {
        if (mVibrator == null) {
            return;
        }
            mVibrator.vibrate(50);
    }

    private SharedPreferences getGestureSharedPreferences() {
        return mGestureContext.getSharedPreferences(
                ScreenOffGesture.GESTURE_SETTINGS,
                Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
    }

    public boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        int scanCode = event.getScanCode();
        boolean isKeySupported = ArrayUtils.contains(sSupportedGestures, scanCode);
        boolean isSliderModeSupported = ArrayUtils.contains(sHandledGestures, event.getScanCode());
        if (isKeySupported && !mEventHandler.hasMessages(GESTURE_REQUEST)) {
            Message msg =  getMessageForKeyEvent(event);
            if (scanCode < KEYCODE_SLIDER_TOP && mProximitySensor != null) {
                mEventHandler.sendMessageDelayed(msg, 200);
            } else if (isSliderModeSupported) {
            if (DEBUG) Log.i(TAG, "scanCode=" + event.getScanCode());
           switch(event.getScanCode()) {
        case KEYCODE_SLIDER_TOP:
            mCurrentPosition = KEYCODE_SLIDER_TOP; 
            Log.i(TAG, "KEYCODE_SLIDER_TOP");
            mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentPosition != KEYCODE_SLIDER_TOP) return;
                    }
                }, 250);
            return true;
        case KEYCODE_SLIDER_MIDDLE:
            mCurrentPosition = KEYCODE_SLIDER_MIDDLE; 
           Log.i(TAG, "KEYCODE_SLIDER_MIDDLE");
            mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentPosition != KEYCODE_SLIDER_MIDDLE) return;
                    }
                }, 50);
            return true;
        case KEYCODE_SLIDER_BOTTOM:
            mCurrentPosition = KEYCODE_SLIDER_BOTTOM; 
           Log.i(TAG, "KEYCODE_SLIDER_BOTTOM");
            mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentPosition != KEYCODE_SLIDER_BOTTOM) return;
                    }
                }, 50);
            return true;
    }
            mEventHandler.removeMessages(GESTURE_REQUEST);
            mEventHandler.sendMessage(msg);
        } else {
            mEventHandler.sendMessage(msg);
         }
     }
     return isKeySupported;
  }

    private Message getMessageForKeyEvent(KeyEvent keyEvent) {
        Message msg = mEventHandler.obtainMessage(GESTURE_REQUEST);
        msg.obj = keyEvent;
        return msg;
    }


  @Override
    public boolean isDisabledKeyEvent(KeyEvent event) {
        boolean isProxyCheckRequired = mUseProxiCheck &&
                ArrayUtils.contains(sProxiCheckedGestures, event.getScanCode());
        if (mProxyIsNear && isProxyCheckRequired) {
            if (DEBUG) Log.i(TAG, "isDisabledKeyEvent: blocked by proxi sensor - scanCode=" + event.getScanCode());
            return true;
        }
        return false;
    }

  @Override
    public boolean isWakeEvent(KeyEvent event){
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        return event.getScanCode() == KEY_DOUBLE_TAP;
    }

    @Override
    public boolean isCameraLaunchEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        return event.getScanCode() == GESTURE_C_SCANCODE;
   }

    @Override
    public boolean canHandleKeyEvent(KeyEvent event) {
        return ArrayUtils.contains(sSupportedGestures, event.getScanCode());
    }



        @Override
    public Intent isActivityLaunchEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return null;
        }
        return null;
    }
}
