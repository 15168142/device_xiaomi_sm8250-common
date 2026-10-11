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

package org.lineageos.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.charge.ChargeActivity;
import org.lineageos.settings.dirac.DiracActivity;
import org.lineageos.settings.display.DcDimmingUtils;
import org.lineageos.settings.hbm.HBMActivity;
import org.lineageos.settings.refreshrate.RefreshActivity;
import org.lineageos.settings.saturation.SaturationActivity;
import org.lineageos.settings.speaker.ClearSpeakerActivity;
import org.lineageos.settings.thermal.ThermalActivity;
import org.lineageos.settings.touchsampling.TouchSamplingUtils;

/**
 * Single entry point for the device specific settings, injected once into the Settings' System
 * page instead of injecting every feature separately.
 *
 * <p>Features that are a single toggle are offered inline, everything else links to its own page.
 */
public class XiaomiPartsFragment extends SettingsBasePreferenceFragment {

    private interface ToggleAction {
        void setEnabled(Context context, boolean enabled);
    }

    private static final String SETTINGS_PACKAGE = "com.android.settings";

    private SwitchPreferenceCompat mDcDimming;
    private SwitchPreferenceCompat mHtsr;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.xiaomi_parts, rootKey);

        mDcDimming = bindToggle(DcDimmingUtils.DC_DIMMING_ENABLE_KEY, DcDimmingUtils.isSupported(),
                R.string.dc_dimming_enable_summary_not_supported, DcDimmingUtils::setEnabled);
        mHtsr = bindToggle(TouchSamplingUtils.PREF_HTSR_ENABLE, TouchSamplingUtils.isSupported(),
                R.string.htsr_enable_summary_not_supported, TouchSamplingUtils::setEnabled);

        link("xiaomi_thermal", ThermalActivity.class);
        link("xiaomi_hbm", HBMActivity.class);
        link("xiaomi_saturation", SaturationActivity.class);
        link("xiaomi_refresh", RefreshActivity.class);
        link("xiaomi_dirac", DiracActivity.class);
        link("xiaomi_clear_speaker", ClearSpeakerActivity.class);
        link("xiaomi_charge", ChargeActivity.class);

        applySettingsTitle("display_category", "display_settings");
        applySettingsTitle("sound_category", "sound_audio_category_title");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Both can be changed by their Quick Settings tile while we are not the active page
        if (mDcDimming != null) {
            mDcDimming.setChecked(DcDimmingUtils.isEnabled(requireContext()));
        }
        if (mHtsr != null) {
            mHtsr.setChecked(TouchSamplingUtils.isEnabled(requireContext()));
        }
    }

    private SwitchPreferenceCompat bindToggle(String key, boolean supported, int unsupportedSummary,
            ToggleAction action) {
        SwitchPreferenceCompat preference = findPreference(key);
        if (preference == null) {
            return null;
        }
        if (!supported) {
            preference.setSummary(unsupportedSummary);
            preference.setEnabled(false);
            return preference;
        }
        preference.setOnPreferenceChangeListener((p, newValue) -> {
            action.setEnabled(requireContext(), (boolean) newValue);
            return true;
        });
        return preference;
    }

    private void link(String key, Class<? extends Activity> target) {
        Preference preference = findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceClickListener(p -> {
            startActivity(new Intent(requireContext(), target));
            return true;
        });
    }

    /**
     * Settings owns the localized names of its own pages and categories, reuse them so our
     * categories read exactly like the Settings section they mirror.
     */
    private void applySettingsTitle(String categoryKey, String settingsString) {
        Preference category = findPreference(categoryKey);
        if (category == null) {
            return;
        }
        try {
            Context settings = requireContext().createPackageContext(SETTINGS_PACKAGE, 0);
            int id = settings.getResources().getIdentifier(settingsString, "string",
                    SETTINGS_PACKAGE);
            if (id != 0) {
                category.setTitle(settings.getResources().getString(id));
            }
        } catch (NameNotFoundException e) {
            // Falls back to the title declared in the preference XML
        }
    }
}
