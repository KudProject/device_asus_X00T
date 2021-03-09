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

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DeviceUtils {

    /* Builds List of actions of type ActionsArray
     * to show in gesture action selection dialog
     */
    public static ActionsArray buildActionsArray(Context context,
                                                 String[] valuesArray, String[] entriesArray) {
        if (valuesArray == null || entriesArray == null || context == null) {
            Log.e("DeviceUtils", "action entries/values is null!");
            return null;
        }
        List<String> actionEntries = new ArrayList<>();
        List<String> actionValues = new ArrayList<>();
        ActionsArray actionsArrayList =
                new ActionsArray();

        for (int i = 0; i < valuesArray.length; i++) {
            actionEntries.add(entriesArray[i]);
            actionValues.add(valuesArray[i]);
        }
        actionsArrayList.entries = actionEntries.toArray(new String[0]);
        actionsArrayList.values = actionValues.toArray(new String[0]);
        return actionsArrayList;
    }

    // Stores all actions related to gestures
    public static class ActionsArray {
        public String[] entries;
        public String[] values;
    }
}
