/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package org.lineageos.settings.touchsampling;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

public final class TouchSamplingUtils {

    private static final String TAG = "TouchSamplingUtils";
    public static final String HTSR_FILE = "/sys/devices/virtual/touch/touch_dev/bump_sample_rate";
    public static final String PREF_HTSR_ENABLE = "htsr_enable";
    private static final String HTSR_PREFS = "SHAREDHTSR";

    public static boolean isSupported() {
        return FileUtils.fileExists(HTSR_FILE);
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_HTSR_ENABLE, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        FileUtils.writeLine(HTSR_FILE, enabled ? "1" : "0");
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(PREF_HTSR_ENABLE, enabled).apply();
        context.getSharedPreferences(HTSR_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(HTSR_PREFS, enabled ? 1 : 0).apply();
    }

    public static void restoreSamplingValue(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(HTSR_PREFS,
                Context.MODE_PRIVATE);
        FileUtils.writeLine(HTSR_FILE, String.valueOf(sharedPref.getInt(HTSR_PREFS, 0)));
    }
}
