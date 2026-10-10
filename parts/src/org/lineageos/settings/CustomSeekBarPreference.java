/*
 * Copyright (C) 2016-2025 crDroid Android Project
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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.SliderPreference;

import com.google.android.material.slider.Slider;

public class CustomSeekBarPreference extends SliderPreference {

    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";

    private boolean mShowSign;
    @Nullable
    private String mUnits = "";
    @Nullable
    private String mDefaultValueText;
    private boolean mDefaultValueTextExists;
    private boolean mDefaultValueExists;
    private int mDefaultValue;

    private CharSequence mUserSummary;
    private boolean mInUserDrag = false;

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        readLegacyAttrs(context, attrs);
        initDefaults();
    }

    public CustomSeekBarPreference(Context context) {
        super(context, null);
        initDefaults();
    }

    private void initDefaults() {
        setHapticFeedbackMode(HAPTIC_FEEDBACK_MODE_ON_TICKS);
        mUserSummary = super.getSummary();
        updateSummaryNow();
    }

    private void readLegacyAttrs(Context c, AttributeSet attrs) {
        if (attrs == null) return;
        final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference);
        try {
            mShowSign = a.getBoolean(R.styleable.CustomSeekBarPreference_showSign, false);
            final String units = a.getString(R.styleable.CustomSeekBarPreference_units);
            if (units != null) mUnits = units;

            final boolean continuous = a.getBoolean(
                    R.styleable.CustomSeekBarPreference_continuousUpdates, false);
            setUpdatesContinuously(continuous);

            mDefaultValueText = a.getString(
                    R.styleable.CustomSeekBarPreference_defaultValueText);
            mDefaultValueTextExists = mDefaultValueText != null && !mDefaultValueText.isEmpty();

            String defaultValue = attrs.getAttributeValue(ANDROIDNS, "defaultValue");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                try {
                    mDefaultValue = Integer.parseInt(defaultValue);
                    mDefaultValueExists = true;
                } catch (NumberFormatException ignored) {
                    mDefaultValueExists = false;
                }
            }

            int interval = a.getInt(R.styleable.CustomSeekBarPreference_interval, 0);
            if (interval > 0) {
                setSliderIncrement(interval);
            }

            // Guard against improper slider increment
            int span = Math.max(0, getMax() - getMin());
            int step = getSliderIncrement();
            if (step <= 0 || span == 0) {
                setSliderIncrement(1);
            } else if ((span % step) != 0) {
                int gcd = gcd(span, step);
                if (gcd <= 0) gcd = 1;
                setSliderIncrement(gcd);
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    public void setSummary(CharSequence summary) {
        mUserSummary = summary;
        updateSummaryNow();
    }

    @Override
    public void setValue(int sliderValue) {
        super.setValue(sliderValue);
        if (!mInUserDrag) updateSummaryNow();
    }

    private void updateSummaryNow() {
        super.setSummary(composeSummary(mUserSummary, getValue()));
    }

    private String formatValueForSummary(int v) {
        if (mDefaultValueExists && mDefaultValueTextExists && v == mDefaultValue) {
            return mDefaultValueText;
        }
        String s = String.valueOf(v);
        if (mShowSign && v > 0) s = "+" + s;
        if (mUnits != null && !mUnits.isEmpty()) s = s + " " + mUnits;
        return s;
    }

    private CharSequence composeSummary(CharSequence userSummary, int v) {
        String valueLine = getContext().getString(
                R.string.custom_seekbar_value, formatValueForSummary(v));
        if (mDefaultValueExists && v == mDefaultValue) {
            valueLine = valueLine + " (" + getContext().getString(
                    R.string.custom_seekbar_default_value) + ")";
        }
        if (userSummary == null || userSummary.length() == 0) return valueLine;
        return userSummary + "\n" + valueLine;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Integer) {
            mDefaultValueExists = true;
            mDefaultValue = (Integer) defaultValue;
        }
        super.setDefaultValue(defaultValue);
        updateSummaryNow();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        if (summaryView != null) {
            summaryView.setText(composeSummary(mUserSummary, getValue()));
        }

        final View labelFrame = holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.label_frame);
        final TextView endText = (TextView) holder.findViewById(android.R.id.text2);
        if (labelFrame != null) {
            labelFrame.setVisibility(mDefaultValueExists ? View.VISIBLE : View.GONE);
        }
        if (endText != null) {
            attachResetIcon(endText);
        }

        final ViewGroup minusFrame = (ViewGroup) holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_start_frame);
        final ImageView minusIcon = (ImageView) holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_start);
        final ViewGroup plusFrame = (ViewGroup) holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_end_frame);
        final ImageView plusIcon = (ImageView) holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_end);
        final Slider slider = (Slider) holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.slider);

        final int step = Math.max(1, getSliderIncrement());

        if (minusFrame != null && minusIcon != null) {
            minusFrame.setVisibility(View.VISIBLE);
            minusIcon.setImageResource(R.drawable.ic_custom_seekbar_minus);
            minusFrame.setOnClickListener(v -> {
                if (!isEnabled()) return;
                int base = slider != null ? Math.round(slider.getValue()) : getValue();
                applyUserValue(Math.max(getMin(), base - step), slider);
                updatePlusMinusEnabledStates(holder);
            });
        }

        if (plusFrame != null && plusIcon != null) {
            plusFrame.setVisibility(View.VISIBLE);
            plusIcon.setImageResource(R.drawable.ic_custom_seekbar_plus);
            plusFrame.setOnClickListener(v -> {
                if (!isEnabled()) return;
                int base = slider != null ? Math.round(slider.getValue()) : getValue();
                applyUserValue(Math.min(getMax(), base + step), slider);
                updatePlusMinusEnabledStates(holder);
            });
        }

        updatePlusMinusEnabledStates(holder);

        if (slider != null && summaryView != null) {
            slider.addOnChangeListener((s, value, fromUser) -> {
                if (fromUser) {
                    summaryView.setText(composeSummary(mUserSummary, (int) value));
                    updatePlusMinusEnabledStates(holder);
                }
            });
            slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider s) {
                    mInUserDrag = true;
                }

                @Override
                public void onStopTrackingTouch(@NonNull Slider s) {
                    mInUserDrag = false;
                    applyUserValue(Math.round(s.getValue()), s);
                    updatePlusMinusEnabledStates(holder);
                }
            });
        }
    }

    @Override
    public void onDependencyChanged(@NonNull Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
        notifyChanged();
    }

    private void applyUserValue(int newVal, @Nullable Slider slider) {
        if (newVal == getValue()) return;
        if (!callChangeListener(newVal)) {
            if (slider != null) slider.setValue(getValue());
            return;
        }
        setValue(newVal);
        notifyChanged();
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        if (a == 0) return b;
        if (b == 0) return a;
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    private void updatePlusMinusEnabledStates(PreferenceViewHolder holder) {
        final View minusFrame = holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_start_frame);
        final ImageView minusIcon = (ImageView) holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_start);
        final View plusFrame = holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_end_frame);
        final ImageView plusIcon = (ImageView) holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_end);
        final boolean enabled = isEnabled();
        final int value = getValue();

        if (minusFrame != null && minusIcon != null) {
            final boolean active = enabled && (value > getMin());
            minusFrame.setEnabled(active);
            minusIcon.setEnabled(active);
        }
        if (plusFrame != null && plusIcon != null) {
            final boolean active = enabled && (value < getMax());
            plusFrame.setEnabled(active);
            plusIcon.setEnabled(active);
        }
    }

    private void attachResetIcon(TextView tv) {
        if (!mDefaultValueExists) {
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            tv.setOnTouchListener(null);
            tv.setClickable(false);
            return;
        }

        final Drawable icon = ResourcesCompat.getDrawable(
                tv.getResources(), R.drawable.ic_custom_seekbar_reset, tv.getContext().getTheme());
        if (icon == null) return;

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null);
        tv.setCompoundDrawablePadding(dp(tv, 6));
        tv.setClickable(isEnabled());
        tv.setFocusable(isEnabled());

        final int tapSlop = dp(tv, 8);

        tv.setOnTouchListener((v, ev) -> {
            if (!isEnabled() || ev.getAction() != MotionEvent.ACTION_UP) return false;

            final boolean isRtl =
                    ViewCompat.getLayoutDirection(tv) == ViewCompat.LAYOUT_DIRECTION_RTL;
            final Drawable[] drs = tv.getCompoundDrawablesRelative();
            final Drawable end = drs[2];
            if (end == null) return false;

            final int iconW = end.getIntrinsicWidth();
            final int x = (int) ev.getX();

            if (!isRtl) {
                final int left = tv.getWidth() - ViewCompat.getPaddingEnd(tv) - iconW - tapSlop;
                if (x >= left) {
                    applyUserValue(mDefaultValue, null);
                    return true;
                }
            } else {
                final int right = ViewCompat.getPaddingStart(tv) + iconW + tapSlop;
                if (x <= right) {
                    applyUserValue(mDefaultValue, null);
                    return true;
                }
            }
            return false;
        });
    }

    private static int dp(TextView v, int dp) {
        return Math.round(dp * v.getResources().getDisplayMetrics().density);
    }
}
