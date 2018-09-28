/*
 * Copyright (C) 2018 The LineageOS Project
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

package org.lineageos.hardware;

import org.lineageos.internal.util.FileUtils;

import lineageos.hardware.TouchscreenGesture;

import java.util.Properties;
import android.util.Log;
import java.util.Arrays;

/**
 * Touchscreen gestures API
 *
 * A device may implement several touchscreen gestures for use while
 * the display is turned off, such as drawing alphabets and shapes.
 * These gestures can be interpreted by userspace to activate certain
 * actions and launch certain apps, such as to skip music tracks,
 * to turn on the flashlight, or to launch the camera app.
 *
 * This *should always* be supported by the hardware directly.
 * A lot of recent touch controllers have a firmware option for this.
 *
 * This API provides support for enumerating the gestures
 * supported by the touchscreen.
 */
public class TouchscreenGestures {

    private static final String GESTURE_PATH =
            "/sys/kernel/touchpanel/gesture_node";

    private static final String PROP_GESTURE_TYPE =
            "persist.asus.gesture.type";

    private static final int[] gestures_support = {
        1,
        1,
        1,
        1,
        1,
        1,
        1
    };

    // Id, name, keycode
    private static final TouchscreenGesture[] TOUCHSCREEN_GESTURES = {
        new TouchscreenGesture(0, "Letter C", 249),
        new TouchscreenGesture(1, "Letter e", 250),
        new TouchscreenGesture(2, "Letter S", 251),
        new TouchscreenGesture(3, "Letter V", 252),
        new TouchscreenGesture(4, "Letter W", 253),
        new TouchscreenGesture(5, "Letter Z", 254),
        new TouchscreenGesture(6, "Swipe Up", 248),
    };

    public static final int[] ALL_GESTURE_MASKS = {
        0x110, // c gesture mask
        0x120, // e gesture mask
        0x140, // s gesture mask
        0x104, // v gesture mask
        0x180, // w gesture mask
        0x108, // z gesture mask
	0x102, //slide up gesture mask
    };

    /**
     * Whether device supports touchscreen gestures
     *
     * @return boolean Supported devices must return always true
     */
    public static boolean isSupported() {

/*
	Note : use proper true/false
*/
	if(FileUtils.isFileWritable(GESTURE_PATH) &&
                FileUtils.isFileReadable(GESTURE_PATH)) {
	        Log.i("Gestures Init : ", "True");
	} else {
	        Log.i("Gestures Init : ", "False");
	}
	return true;
    }

    /*
     * Get the list of available gestures. A mode has an integer
     * identifier and a string name.
     *
     * It is the responsibility of the upper layers to
     * map the name to a human-readable format or perform translation.
     */
    public static TouchscreenGesture[] getAvailableGestures() {
        return TOUCHSCREEN_GESTURES;
    }

    /**
     * This method allows to set the activation status of a gesture
     *
     * @param gesture The gesture to be activated
     *        state   The new activation status of the gesture
     * @return boolean Must be false if gesture is not supported
     *         or the operation failed; true in any other case.
     */
    public static boolean setGestureEnabled(
            final TouchscreenGesture gesture, final boolean state) {
        Properties props = new Properties();
        int gestures_off = 1;
        if (state) {
            FileUtils.writeLine(GESTURE_PATH, "1");
            gestures_support[gesture.id] = 1;
            props.setProperty(PROP_GESTURE_TYPE, Arrays.toString(gestures_support));
        } else {
            gestures_support[gesture.id] = 0;
            props.setProperty(PROP_GESTURE_TYPE, Arrays.toString(gestures_support));
            for (int is_on : gestures_support) {
                if ( is_on == 1 ) {
                    gestures_off = 0;
                }
            }
            if (gestures_off == 1) {
                FileUtils.writeLine(GESTURE_PATH, "0");
            }
        }
        return true;
    }
}
