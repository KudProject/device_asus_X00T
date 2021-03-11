/*
 * Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.com/licenses/>.
 *
 */
package com.asus.zenparts.preferences.vibration;

import android.content.Context;
import android.util.AttributeSet;

import com.asus.zenparts.preferences.CustomSeekBarPreference;

public class VibrationStrengthUser extends CustomSeekBarPreference {

    private static final int mMinVal = 116;
    private static final int mMaxVal = 3596;
    private static final int mDefVal = mMaxVal - (mMaxVal - mMinVal) / 4;
    private final VibrationUtils vibrationUtils;

    public VibrationStrengthUser(Context context, AttributeSet attrs) {
        super(context, attrs);

        vibrationUtils = new VibrationUtils(context);

        mInterval = 10;
        mShowSign = false;
        mUnits = "";
        mContinuousUpdates = false;
        mMinValue = mMinVal;
        mMaxValue = mMaxVal;
        mDefaultValueExists = true;
        mDefaultValue = mDefVal;
        mValue = Integer.parseInt(vibrationUtils.getVibrationStrength(VibrationConstants.VIB_PATH_USER));

        setPersistent(false);
    }

    @Override
    protected void changeValue(int newValue) {
        vibrationUtils.setVibrationStrength(VibrationConstants.VIB_PATH_USER,
                String.valueOf(newValue), VibrationConstants.VIB_KEY_USER);
    }
}
