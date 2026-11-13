/*
 * Copyright (C) 2020 The LineageOS Project
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
package org.lineageos.settings.thermal;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.lineageos.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ThermalSettingsFragment extends SettingsBasePreferenceFragment
        implements ApplicationsState.Callbacks {

    private AllPackagesAdapter mAllPackagesAdapter;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private ActivityFilter mActivityFilter;
    private Map<String, ApplicationsState.AppEntry> mEntryMap =
            new HashMap<String, ApplicationsState.AppEntry>();

    private RecyclerView mAppsRecyclerView;
    private EditText mSearchView;
    private ImageButton mClearSearch;
    private MaterialSwitch mEnabledSwitch;
    private boolean mProfilesEnabled;

    private ThermalUtils mThermalUtils;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplicationsState = ApplicationsState.getInstance(requireActivity().getApplication());
        mSession = mApplicationsState.newSession(this);
        mActivityFilter = new ActivityFilter(requireActivity().getPackageManager());

        mAllPackagesAdapter = new AllPackagesAdapter(requireContext());

        mThermalUtils = new ThermalUtils(requireContext());
        mProfilesEnabled = ThermalUtils.isServiceEnabled(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.thermal_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAppsRecyclerView = view.findViewById(R.id.thermal_rv_view);
        mAppsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mAppsRecyclerView.setAdapter(mAllPackagesAdapter);

        mEnabledSwitch = view.findViewById(R.id.thermal_enabled);
        mEnabledSwitch.setChecked(mProfilesEnabled);
        mEnabledSwitch.setOnCheckedChangeListener((buttonView, enabled) -> {
            mProfilesEnabled = enabled;
            ThermalUtils.setServiceEnabled(requireContext(), enabled);
            updateEnabledState();
        });
        view.findViewById(R.id.thermal_enabled_container).setOnClickListener(
                v -> mEnabledSwitch.toggle());

        mSearchView = view.findViewById(R.id.thermal_search);
        mClearSearch = view.findViewById(R.id.thermal_search_clear);
        mSearchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                mClearSearch.setVisibility(text.length() == 0 ? View.GONE : View.VISIBLE);
                mAllPackagesAdapter.filter(text.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mClearSearch.setOnClickListener(v -> {
            mSearchView.setText("");
            mSearchView.requestFocus();
        });

        updateEnabledState();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.thermal_title);
        mProfilesEnabled = ThermalUtils.isServiceEnabled(requireContext());
        mEnabledSwitch.setChecked(mProfilesEnabled);
        updateEnabledState();
        mSession.onResume();
        rebuild();
    }

    @Override
    public void onPause() {
        mSession.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mAppsRecyclerView != null) {
            mAppsRecyclerView.setAdapter(null);
            mAppsRecyclerView = null;
        }
        mSearchView = null;
        mClearSearch = null;
        mEnabledSwitch = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mSession.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onPackageListChanged() {
        mActivityFilter.updateLauncherInfoList();
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> entries) {
        if (entries != null && isAdded()) {
            handleAppEntries(entries);
            mAllPackagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoadEntriesCompleted() {
        rebuild();
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    private void handleAppEntries(List<ApplicationsState.AppEntry> entries) {
        mEntryMap.clear();
        for (ApplicationsState.AppEntry entry : entries) {
            mEntryMap.put(entry.info.packageName, entry);
        }
        mAllPackagesAdapter.setEntries(entries);
    }

    private void rebuild() {
        mSession.rebuild(mActivityFilter, ApplicationsState.ALPHA_COMPARATOR);
    }

    private void updateEnabledState() {
        if (getView() != null) {
            TextView summary = getView().findViewById(R.id.thermal_enabled_summary);
            summary.setText(mProfilesEnabled ? R.string.thermal_enable_summary
                    : R.string.thermal_disabled_summary);
        }
        if (mSearchView != null) {
            mSearchView.setEnabled(mProfilesEnabled);
            mClearSearch.setEnabled(mProfilesEnabled);
        }
        if (mAppsRecyclerView != null) {
            mAppsRecyclerView.setAlpha(mProfilesEnabled ? 1f : 0.5f);
        }
        mAllPackagesAdapter.notifyDataSetChanged();
    }

    private int getStateDrawable(int state) {
        switch (state) {
            case ThermalUtils.STATE_BENCHMARK:
                return R.drawable.ic_thermal_benchmark;
            case ThermalUtils.STATE_BROWSER:
                return R.drawable.ic_thermal_browser;
            case ThermalUtils.STATE_CAMERA:
                return R.drawable.ic_thermal_camera;
            case ThermalUtils.STATE_DIALER:
                return R.drawable.ic_thermal_dialer;
            case ThermalUtils.STATE_GAMING:
                return R.drawable.ic_thermal_gaming;
            case ThermalUtils.STATE_STREAMING:
                return R.drawable.ic_thermal_streaming;
            case ThermalUtils.STATE_DEFAULT:
            default:
                return R.drawable.ic_thermal_default;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final Spinner mode;
        private final ImageView icon;
        private final ImageView stateIcon;
        private final ImageButton touchIcon;

        private ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.app_name);
            mode = view.findViewById(R.id.app_mode);
            icon = view.findViewById(R.id.app_icon);
            stateIcon = view.findViewById(R.id.state);
            touchIcon = view.findViewById(R.id.touch);
            view.setTag(this);
        }
    }

    private class ModeAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private final int[] items = {
                R.string.thermal_default,
                R.string.thermal_benchmark,
                R.string.thermal_browser,
                R.string.thermal_camera,
                R.string.thermal_dialer,
                R.string.thermal_gaming,
                R.string.thermal_streaming
        };

        private ModeAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view;
            if (convertView != null) {
                view = (TextView) convertView;
            } else {
                view = (TextView) inflater.inflate(android.R.layout.simple_spinner_dropdown_item,
                        parent, false);
            }

            view.setText(items[position]);
            view.setTextSize(14f);

            return view;
        }
    }

    private class AllPackagesAdapter extends RecyclerView.Adapter<ViewHolder>
            implements AdapterView.OnItemSelectedListener, SectionIndexer {

        private final List<ApplicationsState.AppEntry> mAllEntries = new ArrayList<>();
        private final List<ApplicationsState.AppEntry> mEntries = new ArrayList<>();
        private String[] mSections = new String[0];
        private int[] mPositions = new int[0];
        private String mQuery = "";

        private AllPackagesAdapter(Context context) {
            mActivityFilter = new ActivityFilter(context.getPackageManager());
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
        }

        @Override
        public long getItemId(int position) {
            return mEntries.get(position).id;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.thermal_list_item, parent, false));
            Context context = holder.itemView.getContext();
            holder.mode.setAdapter(new ModeAdapter(context));
            holder.mode.setOnItemSelectedListener(this);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ApplicationsState.AppEntry entry = mEntries.get(position);

            if (entry == null) {
                return;
            }

            int packageState = mProfilesEnabled
                    ? mThermalUtils.getStateForPackage(entry.info.packageName)
                    : ThermalUtils.STATE_DEFAULT;

            holder.title.setText(entry.label);
            holder.title.setOnClickListener(v -> holder.mode.performClick());
            mApplicationsState.ensureIcon(entry);
            holder.icon.setImageDrawable(entry.icon);
            holder.mode.setSelection(packageState, false);
            holder.mode.setEnabled(mProfilesEnabled);
            holder.mode.setTag(entry);
            holder.stateIcon.setImageResource(getStateDrawable(packageState));

            boolean hasTouchControls = packageState == ThermalUtils.STATE_GAMING
                    || packageState == ThermalUtils.STATE_BENCHMARK;
            holder.touchIcon.setVisibility(hasTouchControls ? View.VISIBLE : View.GONE);
            holder.touchIcon.setEnabled(mProfilesEnabled);
            holder.touchIcon.setOnClickListener(v -> openTouchSettings(entry));
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries) {
            mAllEntries.clear();
            mAllEntries.addAll(entries);
            filter(mQuery);
        }

        private void filter(String query) {
            mQuery = query == null ? "" : query.trim();
            final String normalizedQuery = mQuery.toLowerCase(Locale.getDefault());
            mEntries.clear();

            for (ApplicationsState.AppEntry entry : mAllEntries) {
                final CharSequence label = entry.label;
                if (normalizedQuery.isEmpty() || (label != null
                        && label.toString().toLowerCase(Locale.getDefault())
                                .contains(normalizedQuery))) {
                    mEntries.add(entry);
                }
            }

            rebuildSections();
            notifyDataSetChanged();
        }

        private void rebuildSections() {
            final ArrayList<String> sections = new ArrayList<>();
            final ArrayList<Integer> positions = new ArrayList<>();
            final PackageManager pm = requireContext().getPackageManager();
            String lastSectionIndex = null;
            int offset = 0;

            for (int i = 0; i < mEntries.size(); i++) {
                final ApplicationInfo info = mEntries.get(i).info;
                final String label = (String) info.loadLabel(pm);
                final String sectionIndex;

                if (!info.enabled) {
                    sectionIndex = "--";
                } else if (TextUtils.isEmpty(label)) {
                    sectionIndex = "";
                } else {
                    sectionIndex = label.substring(0, 1).toUpperCase(Locale.getDefault());
                }

                if (lastSectionIndex == null
                        || !TextUtils.equals(sectionIndex, lastSectionIndex)) {
                    sections.add(sectionIndex);
                    positions.add(offset);
                    lastSectionIndex = sectionIndex;
                }

                offset++;
            }

            mSections = sections.toArray(new String[0]);
            mPositions = new int[positions.size()];
            for (int i = 0; i < positions.size(); i++) {
                mPositions[i] = positions.get(i);
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final ApplicationsState.AppEntry entry =
                    (ApplicationsState.AppEntry) parent.getTag();
            if (entry == null || !mProfilesEnabled) {
                return;
            }

            int currentState = mThermalUtils.getStateForPackage(entry.info.packageName);
            if (currentState != position) {
                mThermalUtils.writePackage(entry.info.packageName, position);
                notifyDataSetChanged();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        @Override
        public int getPositionForSection(int section) {
            if (section < 0 || section >= mSections.length) {
                return -1;
            }

            return mPositions[section];
        }

        @Override
        public int getSectionForPosition(int position) {
            if (position < 0 || position >= getItemCount()) {
                return -1;
            }

            final int index = Arrays.binarySearch(mPositions, position);
            return index >= 0 ? index : -index - 2;
        }

        @Override
        public Object[] getSections() {
            return mSections;
        }
    }

    private void openTouchSettings(ApplicationsState.AppEntry entry) {
        TouchSettingsFragment fragment = new TouchSettingsFragment();
        Bundle arguments = new Bundle();
        arguments.putString("appName", String.valueOf(entry.label));
        arguments.putString("packageName", entry.info.packageName);
        fragment.setArguments(arguments);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    private class ActivityFilter implements ApplicationsState.AppFilter {

        private final PackageManager mPackageManager;
        private final List<String> mLauncherResolveInfoList = new ArrayList<String>();

        private ActivityFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;

            updateLauncherInfoList();
        }

        public void updateLauncherInfoList() {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoList = mPackageManager.queryIntentActivities(i, 0);

            synchronized (mLauncherResolveInfoList) {
                mLauncherResolveInfoList.clear();
                for (ResolveInfo ri : resolveInfoList) {
                    mLauncherResolveInfoList.add(ri.activityInfo.packageName);
                }
            }
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry entry) {
            boolean show = !mAllPackagesAdapter.mEntries.contains(entry.info.packageName);
            if (show) {
                synchronized (mLauncherResolveInfoList) {
                    show = mLauncherResolveInfoList.contains(entry.info.packageName);
                }
            }
            return show;
        }
    }
}
