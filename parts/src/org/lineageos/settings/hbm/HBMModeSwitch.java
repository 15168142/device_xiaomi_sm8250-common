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
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.lineageos.settings.hbm;

import android.provider.Settings;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.display.*;

public class HBMModeSwitch implements OnPreferenceChangeListener {
    private static final String HBM = "/sys/class/drm/card0/card0-DSI-1/disp_param";
    private static final String BACKLIGHT = "/sys/class/backlight/panel0-backlight/brightness";
    private static final String SAVED_BRIGHTNESS = "hbm_saved_brightness";
    private Context mContext;

    public HBMModeSwitch(Context context) {
        mContext = context;
    }

    public static String getHBM() {
        if (FileUtils.isFileWritable(HBM)) {
            return HBM;
        }
        return null;
    }

    public static String getBACKLIGHT() {
        if (FileUtils.isFileWritable(BACKLIGHT)) {
            return BACKLIGHT;
        }
        return null;
    }

    /**
     * hbm needs both the display parameter node and the backlight node to work
     */
    public static boolean isSupported() {
        return getHBM() != null && getBACKLIGHT() != null;
    }

    /**
     * turns hbm on or off, shared by the settings switch and the quick settings tile
     *
     * @return false if the node is missing, dc dimming is on or the write failed
     */
    public static boolean setEnabled(Context context, boolean enabled) {
        if (!isSupported()) return false;
        if (DcDimmingUtils.isEnabled(context)) return false;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (enabled) {
            if (!FileUtils.writeLine(getHBM(), "0x10000")) return false;
            final int current = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 255);
            prefs.edit().putInt(SAVED_BRIGHTNESS, current).apply();
            FileUtils.writeLine(getBACKLIGHT(), "2047");
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 255);
            return true;
        }

        if (!FileUtils.writeLine(getHBM(), "0xF0000")) return false;
        final int saved = prefs.getInt(SAVED_BRIGHTNESS, -1);
        if (saved >= 0) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, saved);
            FileUtils.writeLine(getBACKLIGHT(), String.valueOf(saved * 2047 / 255));
            prefs.edit().remove(SAVED_BRIGHTNESS).apply();
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return setEnabled(mContext, (Boolean) newValue);
    }
}
