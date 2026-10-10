/*
* Copyright (C) 2016 The OmniROM Project
* Copyright (C) 2018-2021 crDroid Android Project
* Copyright (C) 2019-2022 Evolution X Project
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.CustomSeekBarPreference;
import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.R;

public class HBMFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = HBMFragment.class.getSimpleName();

    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_AUTO_HBM_SWITCH = "auto_hbm";
    public static final String KEY_AUTO_HBM_THRESHOLD = "auto_hbm_threshold";
    public static final String KEY_HBM_DISABLE_TIME = "hbm_disable_time";

    private static final int THRESHOLD_MIN = 0;
    private static final int THRESHOLD_MAX = 60000;
    private static final int THRESHOLD_DEFAULT = 7000;
    private static final int THRESHOLD_STEP = 1000;
    private static final int DISABLE_TIME_MIN = 1;
    private static final int DISABLE_TIME_MAX = 10;
    private static final int DISABLE_TIME_DEFAULT = 1;

    private TwoStatePreference mHBMModeSwitch;
    private TwoStatePreference mAutoHBMSwitch;
    private CustomSeekBarPreference mThresholdPreference;
    private CustomSeekBarPreference mTimePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.hbm_settings, rootKey);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // HBM
        mHBMModeSwitch = (TwoStatePreference) findPreference(KEY_HBM_SWITCH);
        mHBMModeSwitch.setOnPreferenceChangeListener(new HBMModeSwitch(getContext()));

        // AutoHBM
        mAutoHBMSwitch = (TwoStatePreference) findPreference(KEY_AUTO_HBM_SWITCH);
        mAutoHBMSwitch.setOnPreferenceChangeListener(this);
        mAutoHBMSwitch.setChecked(prefs.getBoolean(KEY_AUTO_HBM_SWITCH, false));

        // AutoHBM threshold
        mThresholdPreference = findPreference(KEY_AUTO_HBM_THRESHOLD);
        mThresholdPreference.setMin(THRESHOLD_MIN);
        mThresholdPreference.setMax(THRESHOLD_MAX);
        mThresholdPreference.setSliderIncrement(THRESHOLD_STEP);
        mThresholdPreference.setOnPreferenceChangeListener(this);
        mThresholdPreference.setValue(
                readIntPreference(prefs, KEY_AUTO_HBM_THRESHOLD, THRESHOLD_DEFAULT));

        // AutoHBM disable delay
        mTimePreference = findPreference(KEY_HBM_DISABLE_TIME);
        mTimePreference.setMin(DISABLE_TIME_MIN);
        mTimePreference.setMax(DISABLE_TIME_MAX);
        mTimePreference.setSliderIncrement(1);
        mTimePreference.setOnPreferenceChangeListener(this);
        mTimePreference.setValue(
                readIntPreference(prefs, KEY_HBM_DISABLE_TIME, DISABLE_TIME_DEFAULT));
    }

    public static boolean isAUTOHBMEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HBMFragment.KEY_AUTO_HBM_SWITCH, false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAutoHBMSwitch) {
            Boolean enabled = (Boolean) newValue;
            SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            prefChange.putBoolean(KEY_AUTO_HBM_SWITCH, enabled).commit();
            FileUtils.enableService(getContext());
            return true;
        } else if (preference == mThresholdPreference) {
            writeIntPreference(KEY_AUTO_HBM_THRESHOLD, (Integer) newValue);
            return true;
        } else if (preference == mTimePreference) {
            writeIntPreference(KEY_HBM_DISABLE_TIME, (Integer) newValue);
            return true;
        }

        return false;
    }

    private void writeIntPreference(String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(key, String.valueOf(value)).apply();
    }

    private static int readIntPreference(SharedPreferences prefs, String key, int defaultValue) {
        Object stored = prefs.getAll().get(key);
        if (stored instanceof Integer) {
            return (Integer) stored;
        }
        if (stored instanceof String) {
            try {
                return Integer.parseInt((String) stored);
            } catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return defaultValue;
    }
}
