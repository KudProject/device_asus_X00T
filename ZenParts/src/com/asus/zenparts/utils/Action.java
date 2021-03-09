/*
 * Copyright (C) 2014 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asus.zenparts.utils;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.session.MediaSessionLegacyHelper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;

import java.net.URISyntaxException;

public class Action {

    private static boolean sTorchEnabled = false;

    public static void processAction(Context context, String action) {

        if (action == null || action.equals(ActionConstants.ACTION_NULL))
            return;

        // process the actions
        switch (action) {
            case ActionConstants.ACTION_TORCH:
                try {
                    CameraManager cameraManager = (CameraManager)
                            context.getSystemService(Context.CAMERA_SERVICE);
                    for (final String cameraId : cameraManager.getCameraIdList()) {
                        CameraCharacteristics characteristics =
                                cameraManager.getCameraCharacteristics(cameraId);
                        Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                        int orient = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (flashAvailable != null && flashAvailable && orient == CameraCharacteristics.LENS_FACING_BACK) {
                            cameraManager.setTorchMode(cameraId, !sTorchEnabled);
                            sTorchEnabled = !sTorchEnabled;
                            break;
                        }
                    }
                } catch (CameraAccessException ignored) {
                }
                break;
            case ActionConstants.ACTION_VIB: {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null && ActivityManagerNative.isSystemReady()) {
                    if (am.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
                        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                        if (vib != null) {
                            vib.vibrate(50);
                        }
                    } else {
                        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        ToneGenerator tg = new ToneGenerator(
                                AudioManager.STREAM_NOTIFICATION,
                                (int) (ToneGenerator.MAX_VOLUME * 0.85));
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
                break;
            }
            case ActionConstants.ACTION_SILENT: {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null && ActivityManagerNative.isSystemReady()) {
                    if (am.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    } else {
                        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        ToneGenerator tg = new ToneGenerator(
                                AudioManager.STREAM_NOTIFICATION,
                                (int) (ToneGenerator.MAX_VOLUME * 0.85));
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
                break;
            }
            case ActionConstants.ACTION_VIB_SILENT: {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null && ActivityManagerNative.isSystemReady()) {
                    if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                        Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                        if (vib != null) {
                            vib.vibrate(50);
                        }
                    } else if (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    } else {
                        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        ToneGenerator tg = new ToneGenerator(
                                AudioManager.STREAM_NOTIFICATION,
                                (int) (ToneGenerator.MAX_VOLUME * 0.85));
                        tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    }
                }
                break;
            }
            case ActionConstants.ACTION_MEDIA_PREVIOUS:
                dispatchMediaKeyWithWakeLock(KeyEvent.KEYCODE_MEDIA_PREVIOUS, context);
                break;
            case ActionConstants.ACTION_MEDIA_NEXT:
                dispatchMediaKeyWithWakeLock(KeyEvent.KEYCODE_MEDIA_NEXT, context);
                break;
            case ActionConstants.ACTION_MEDIA_PLAY_PAUSE:
                dispatchMediaKeyWithWakeLock(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, context);
                break;
            case ActionConstants.ACTION_WAKE_DEVICE:
                PowerManager powerManager =
                        (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (!powerManager.isScreenOn()) {
                    powerManager.wakeUp(SystemClock.uptimeMillis());
                }
                break;
            default: {
                // we must have a custom uri
                Intent intent;
                try {
                    intent = Intent.parseUri(action, 0);
                } catch (URISyntaxException e) {
                    Log.e("aospActions:", "URISyntaxException: [" + action + "]");
                    return;
                }
                startActivity(context, intent);
                break;
            }
        }

    }

    public static void startActivity(Context context, Intent intent) {
        if (intent == null)
            return;

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivityAsUser(intent,
                new UserHandle(UserHandle.USER_CURRENT));
    }

    private static void dispatchMediaKeyWithWakeLock(int keycode, Context context) {
        if (ActivityManagerNative.isSystemReady()) {
            KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
            MediaSessionLegacyHelper.getHelper(context).sendMediaButtonEvent(event, true);
            event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
            MediaSessionLegacyHelper.getHelper(context).sendMediaButtonEvent(event, true);
        }
    }
}
