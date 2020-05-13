package app.crossword.yourealwaysbe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceFragmentCompat;

import app.crossword.yourealwaysbe.firstrun.FirstrunActivity;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.service.BackgroundDownloadService;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;

public class PreferencesFragment
       extends PreferenceFragmentCompat
       implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        if (!AndroidVersionUtils.Factory.getInstance().isBackgroundDownloadAvaliable()) {
            Preference backgroundDownload = findPreference("backgroundDownload");
            backgroundDownload.setSelectable(false);
            backgroundDownload.setEnabled(false);
            backgroundDownload.setSummary("Requires Android Lollipop or later");
        }

        findPreference("releaseNotes")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/release.html"),
                            getActivity(), HTMLActivity.class);
                    getActivity().startActivity(i);

                    return true;
                }
            });

        findPreference("license")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/license.html"),
                            getActivity(), HTMLActivity.class);
                    getActivity().startActivity(i);

                    return true;
                }
            });

        findPreference("firstRun")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference arg0) {
                Intent i = new Intent(Intent.ACTION_VIEW, null,
                        getActivity(), FirstrunActivity.class);
                getActivity().startActivity(i);

                return true;
            }
        });
    }

    protected void onResumePreferences() {
        PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext())
                .registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    protected void onPausePreferences() {
        PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String pref) {
        if (pref.equals("backgroundDownload") ||
                pref.equals("backgroundDownloadRequireUnmetered") ||
                pref.equals("backgroundDownloadAllowRoaming") ||
                pref.equals("backgroundDownloadRequireCharging")) {
            Context context = getActivity().getApplicationContext();
            BackgroundDownloadService.updateJob(context);
        }
    }
}
