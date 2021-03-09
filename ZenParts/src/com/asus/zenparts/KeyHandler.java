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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ArrayUtils;
import com.asus.zenparts.settings.ScreenOffGesture;
import com.asus.zenparts.utils.Action;
import com.asus.zenparts.utils.ActionConstants;

public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();
    private static final int GESTURE_REQUEST = 1;

    // Supported key codes
    private static final int GESTURE_C_KEYCODE = 748;
    private static final int GESTURE_E_KEYCODE = 749;
    private static final int GESTURE_S_KEYCODE = 752;
    private static final int GESTURE_V_KEYCODE = 753;
    private static final int GESTURE_W_KEYCODE = 754;
    private static final int GESTURE_Z_KEYCODE = 755;
    private static final int GESTURE_SWIPE_UP = 756;
    private static final int GESTURE_SWIPE_DOWN = 757;
    private static final int GESTURE_SWIPE_LEFT = 758;
    private static final int GESTURE_SWIPE_RIGHT = 759;
    private static final int GESTURE_DOUBLE_TAP = 260;

    // Supported gestures
    private static final int[] sSupportedGestures = new int[]{
            GESTURE_C_KEYCODE,
            GESTURE_E_KEYCODE,
            GESTURE_V_KEYCODE,
            GESTURE_W_KEYCODE,
            GESTURE_S_KEYCODE,
            GESTURE_Z_KEYCODE,
            GESTURE_SWIPE_UP,
            GESTURE_SWIPE_DOWN,
            GESTURE_SWIPE_LEFT,
            GESTURE_SWIPE_RIGHT,
            GESTURE_DOUBLE_TAP,
    };

    // vars
    private final Context mContext;
    private final EventHandler mEventHandler;
    private Context mGestureContext = null;
    private Vibrator mVibrator;

    public KeyHandler(Context context) {
        mContext = context;
        mEventHandler = new EventHandler();

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null || !mVibrator.hasVibrator())
            mVibrator = null;

        try {
            mGestureContext = mContext.createPackageContext(
                    "com.asus.zenparts", Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException ignored) {
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

    // Invoked by DeviceKeyHandler
    public KeyEvent handleKeyEvent(KeyEvent event) {
        Log.i(TAG, " handleKeyEvent called");

        // Return if action key is not completed, released yet
        if (event.getAction() != KeyEvent.ACTION_UP)
            return event;

        int scanCode = event.getScanCode();
        boolean isKeySupported = ArrayUtils.contains(sSupportedGestures, scanCode);
        if (isKeySupported && !mEventHandler.hasMessages(GESTURE_REQUEST)) {
            Message msg = getMessageForKeyEvent(event);
            mEventHandler.sendMessage(msg);
        }
        return event;
    }

    private Message getMessageForKeyEvent(KeyEvent keyEvent) {
        Message msg = mEventHandler.obtainMessage(GESTURE_REQUEST);
        msg.obj = keyEvent;
        return msg;
    }

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            KeyEvent event = (KeyEvent) msg.obj;
            String action = null;
            switch (event.getScanCode()) {
                case GESTURE_C_KEYCODE:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_C,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_E_KEYCODE:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_E,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_V_KEYCODE:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_V,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_W_KEYCODE:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_W,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_S_KEYCODE:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_S,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_Z_KEYCODE:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_Z,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_SWIPE_UP:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_UP,
                                    ActionConstants.ACTION_WAKE_DEVICE);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_SWIPE_DOWN:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_DOWN,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_SWIPE_LEFT:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_LEFT,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_SWIPE_RIGHT:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_RIGHT,
                                    ActionConstants.ACTION_NULL);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
                case GESTURE_DOUBLE_TAP:
                    action = getGestureSharedPreferences()
                            .getString(ScreenOffGesture.PREF_GESTURE_DOUBLE_TAP,
                                    ActionConstants.ACTION_WAKE_DEVICE);
                    if (!action.equals(ActionConstants.ACTION_NULL))
                        doHapticFeedback();
                    break;
            }

            // Wakeup and launch camera if action is ACTION_CAMERA
            if (action != null && action.equals(ActionConstants.ACTION_CAMERA)) {
                Log.i(TAG, "Got ACTION_CAMERA");
                Action.processAction(mContext, ActionConstants.ACTION_WAKE_DEVICE);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // ignore - crashing
                // mContext.startActivity(intent);
            }

            // Gesture not supported
            if (action == null)
                return;

            // Process gesture action, other than camera
            Action.processAction(mContext, action);
        }
    }
}
