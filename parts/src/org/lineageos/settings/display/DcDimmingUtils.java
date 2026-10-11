/*
 * Copyright (C) 2026 The LineageOS Project
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

package org.lineageos.settings.display;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

import java.io.File;

public final class DcDimmingUtils {

    public static final String DC_DIMMING_ENABLE_KEY = "dc_dimming_enable";

    private static final String DC_DIMMING_NODE =
            "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/dimlayer_exposure";
    private static final String HBM_NODE = "/sys/class/drm/card0/card0-DSI-1/disp_param";
    private static final String HBM_KEY = "hbm";

    private DcDimmingUtils() {
    }

    public static boolean isSupported() {
        return FileUtils.fileExists(DC_DIMMING_NODE);
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(DC_DIMMING_ENABLE_KEY, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        FileUtils.writeLine(DC_DIMMING_NODE, enabled ? "1" : "0");
        if (enabled) {
            disableHbm(context);
        } else {
            new File(HBM_NODE).setWritable(true);
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(DC_DIMMING_ENABLE_KEY, enabled).apply();
    }

    public static void restore(Context context) {
        setEnabled(context, isEnabled(context));
    }

    public static void disableHbm(Context context) {
        // Disable HBM mode
        FileUtils.writeLine(HBM_NODE, "0xF0000");
        // Make HBM mode path read-only, DC dimming and HBM can't be used together
        new File(HBM_NODE).setReadOnly();
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(HBM_KEY, false).apply();
        // Update HBM mode tile
        Intent intent = new Intent("org.lineageos.settings.hbm.UPDATE_TILE");
        intent.putExtra("enabled", false);
        context.sendBroadcast(intent);
    }
}
