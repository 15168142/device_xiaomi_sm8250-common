/*
 * Copyright (C) 2025 The LineageOS Project
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

package org.lineageos.settings.charge;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class ChargeSettingsFragment extends SettingsBasePreferenceFragment {

    private static final String KEY_BYPASS_CHARGE = "charge_bypass";
    private static final String KEY_ILLUSTRATION = "charge_bypass_illustration";

    private ChargeUtils mChargeUtils;
    private SwitchPreferenceCompat mBypassSwitch;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.charge_settings, rootKey);

        mChargeUtils = new ChargeUtils(requireContext());

        IllustrationPreference illustration = findPreference(KEY_ILLUSTRATION);
        if (illustration != null) {
            illustration.setImageDrawable(
                    requireContext().getDrawable(R.drawable.bypass_charging_illustration));
        }

        mBypassSwitch = findPreference(KEY_BYPASS_CHARGE);
        if (!mChargeUtils.isBypassChargeSupported()) {
            mBypassSwitch.setSummary(R.string.charge_bypass_unavailable);
            mBypassSwitch.setEnabled(false);
            return;
        }

        mBypassSwitch.setChecked(mChargeUtils.isBypassChargeEnabled());
        mBypassSwitch.setOnPreferenceChangeListener(this::onBypassChargeChanged);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBypassSwitch != null && mChargeUtils.isBypassChargeSupported()) {
            mBypassSwitch.setChecked(mChargeUtils.isBypassChargeEnabled());
        }
    }

    private boolean onBypassChargeChanged(Preference preference, Object newValue) {
        if (!(boolean) newValue) {
            mChargeUtils.enableBypassCharge(false);
            updateTile();
            return true;
        }

        ChargeUtils.SafetyCheckResult safetyCheck = mChargeUtils.performSafetyChecks();
        if (!safetyCheck.isSafe()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.charge_bypass_title)
                    .setMessage(getString(R.string.charge_bypass_safety_failed,
                            safetyCheck.getReason()))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return false;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.charge_bypass_title)
                .setMessage(R.string.charge_bypass_warning)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mChargeUtils.enableBypassCharge(true);
                    mBypassSwitch.setChecked(mChargeUtils.isBypassChargeEnabled());
                    updateTile();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnCancelListener(dialog -> mBypassSwitch.setChecked(false))
                .show();
        return false;
    }

    private void updateTile() {
        BypassChargeTileService.updateTile(requireContext());
    }
}
