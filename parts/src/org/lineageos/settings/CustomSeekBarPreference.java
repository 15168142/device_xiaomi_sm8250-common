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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.SliderPreference;

import com.google.android.material.slider.Slider;

/**
 * A {@link SliderPreference} that keeps the legacy two line summary, appending the current value
 * below the preference summary, and offers a reset icon when a default value is available.
 *
 * <p>The start/end buttons are left to the framework, which wires them up from the
 * {@code iconStart}/{@code iconEnd} attributes.
 */
public class CustomSeekBarPreference extends SliderPreference {

    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";

    private boolean mShowSign;
    private String mUnits = "";
    private String mDefaultValueText;
    private boolean mDefaultValueTextExists;
    private boolean mDefaultValueExists;
    private int mDefaultValue;

    private boolean mValueInTitle;

    private CharSequence mUserSummary;

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
        // A slider with no title of its own shows its value as the primary text
        mValueInTitle = getTitle() == null || getTitle().length() == 0;
        mUserSummary = super.getSummary();
        updateSummaryNow();
    }

    private void readLegacyAttrs(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CustomSeekBarPreference);
        try {
            mShowSign = a.getBoolean(R.styleable.CustomSeekBarPreference_showSign, false);
            final String units = a.getString(R.styleable.CustomSeekBarPreference_units);
            if (units != null) {
                mUnits = units;
            }

            setUpdatesContinuously(a.getBoolean(
                    R.styleable.CustomSeekBarPreference_continuousUpdates, false));

            mDefaultValueText = a.getString(
                    R.styleable.CustomSeekBarPreference_defaultValueText);
            mDefaultValueTextExists = mDefaultValueText != null && !mDefaultValueText.isEmpty();

            final String defaultValue = attrs.getAttributeValue(ANDROIDNS, "defaultValue");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                try {
                    mDefaultValue = Integer.parseInt(defaultValue);
                    mDefaultValueExists = true;
                } catch (NumberFormatException ignored) {
                    mDefaultValueExists = false;
                }
            }

            final int interval = a.getInt(R.styleable.CustomSeekBarPreference_interval, 0);
            if (interval > 0) {
                setSliderIncrement(interval);
            }

            // Guard against improper slider increment
            final int span = Math.max(0, getMax() - getMin());
            final int step = getSliderIncrement();
            if (step <= 0 || span == 0) {
                setSliderIncrement(1);
            } else if ((span % step) != 0) {
                int gcd = gcd(span, step);
                if (gcd <= 0) {
                    gcd = 1;
                }
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
        updateSummaryNow();
    }

    private void updateSummaryNow() {
        final CharSequence valueText = composeSummary(mUserSummary, getValue());
        if (mValueInTitle) {
            super.setTitle(valueText);
        } else {
            super.setSummary(valueText);
        }
    }

    private String formatValueForSummary(int value) {
        if (mDefaultValueExists && mDefaultValueTextExists && value == mDefaultValue) {
            return mDefaultValueText;
        }
        String text = String.valueOf(value);
        if (mShowSign && value > 0) {
            text = "+" + text;
        }
        if (!mUnits.isEmpty()) {
            text = text + " " + mUnits;
        }
        return text;
    }

    private CharSequence composeSummary(CharSequence userSummary, int value) {
        String valueLine = getContext().getString(
                R.string.custom_seekbar_value, formatValueForSummary(value));
        if (mDefaultValueExists && value == mDefaultValue) {
            valueLine = valueLine + " (" + getContext().getString(
                    R.string.custom_seekbar_default_value) + ")";
        }
        if (userSummary == null || userSummary.length() == 0) {
            return valueLine;
        }
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

        final TextView valueView = (TextView) holder.findViewById(
                mValueInTitle ? android.R.id.title : android.R.id.summary);
        if (valueView != null) {
            valueView.setText(composeSummary(mUserSummary, getValue()));
        }

        final View labelFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.label_frame);
        if (labelFrame != null) {
            labelFrame.setVisibility(mDefaultValueExists ? View.VISIBLE : View.GONE);
        }

        final TextView endText = (TextView) holder.findViewById(android.R.id.text2);
        if (endText != null) {
            attachResetIcon(endText);
        }

        final Slider slider = (Slider) holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.slider);
        if (slider != null && valueView != null) {
            slider.addOnChangeListener((s, value, fromUser) -> {
                if (fromUser) {
                    valueView.setText(composeSummary(mUserSummary, (int) value));
                }
            });
        }

        attachLongPressJumps(holder);
    }

    /**
     * Long pressing the start button jumps to the middle of the range first and then to the
     * minimum, long pressing the end button does the same towards the maximum.
     */
    private void attachLongPressJumps(PreferenceViewHolder holder) {
        final View startFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_start_frame);
        if (startFrame != null) {
            startFrame.setOnLongClickListener(v -> {
                applyUserValue(jumpTarget(false));
                return true;
            });
        }

        final View endFrame = holder.findViewById(
                com.android.settingslib.widget.preference.slider.R.id.icon_end_frame);
        if (endFrame != null) {
            endFrame.setOnLongClickListener(v -> {
                applyUserValue(jumpTarget(true));
                return true;
            });
        }
    }

    private int jumpTarget(boolean toEnd) {
        final int min = getMin();
        final int max = getMax();

        if ((max - min) <= (2 * getSliderIncrement())) {
            return toEnd ? max : min;
        }

        final int middle = toEnd
                ? -Math.floorDiv(-(max + min), 2) : Math.floorDiv(max + min, 2);
        final int value = getValue();
        if (toEnd) {
            return value < middle ? middle : max;
        }
        return value > middle ? middle : min;
    }

    @Override
    public void onDependencyChanged(@NonNull Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
        notifyChanged();
    }

    private void applyUserValue(int newValue) {
        if (newValue == getValue() || !callChangeListener(newValue)) {
            return;
        }
        setValue(newValue);
        notifyChanged();
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        if (a == 0) {
            return b;
        }
        if (b == 0) {
            return a;
        }
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
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
        if (icon == null) {
            return;
        }

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null);
        tv.setCompoundDrawablePadding(dp(tv, 6));
        tv.setClickable(isEnabled());
        tv.setFocusable(isEnabled());

        final int tapSlop = dp(tv, 8);

        tv.setOnTouchListener((v, ev) -> {
            if (!isEnabled() || ev.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }

            final Drawable[] drawables = tv.getCompoundDrawablesRelative();
            final Drawable end = drawables[2];
            if (end == null) {
                return false;
            }

            final int iconWidth = end.getIntrinsicWidth();
            final int x = (int) ev.getX();
            final boolean isRtl =
                    ViewCompat.getLayoutDirection(tv) == ViewCompat.LAYOUT_DIRECTION_RTL;

            if (isRtl) {
                if (x <= ViewCompat.getPaddingStart(tv) + iconWidth + tapSlop) {
                    applyUserValue(mDefaultValue);
                    return true;
                }
            } else {
                final int left = tv.getWidth() - ViewCompat.getPaddingEnd(tv) - iconWidth - tapSlop;
                if (x >= left) {
                    applyUserValue(mDefaultValue);
                    return true;
                }
            }
            return false;
        });
    }

    private static int dp(TextView view, int dpValue) {
        return Math.round(dpValue * view.getResources().getDisplayMetrics().density);
    }
}
