package app.crossword.yourealwaysbe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import app.crossword.yourealwaysbe.firstrun.FirstrunActivity;
import app.crossword.yourealwaysbe.service.BackgroundDownloadService;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;


public class PreferencesActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

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
                            PreferencesActivity.this, HTMLActivity.class);
                    PreferencesActivity.this.startActivity(i);

                    return true;
                }
            });

        findPreference("license")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/license.html"),
                            PreferencesActivity.this, HTMLActivity.class);
                    PreferencesActivity.this.startActivity(i);

                    return true;
                }
            });

        findPreference("aboutScrapes")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/scrapes.html"),
                            PreferencesActivity.this, HTMLActivity.class);
                    PreferencesActivity.this.startActivity(i);

                    return true;
                }
            });

        findPreference("firstRun")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference arg0) {
                Intent i = new Intent(Intent.ACTION_VIEW, null,
                        PreferencesActivity.this, FirstrunActivity.class);
                PreferencesActivity.this.startActivity(i);

                return true;
            }
        });
    }

    protected void onResume() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String pref) {
        if (pref.equals("backgroundDownload") ||
                pref.equals("backgroundDownloadRequireUnmetered") ||
                pref.equals("backgroundDownloadAllowRoaming") ||
                pref.equals("backgroundDownloadRequireCharging")) {
            Context context = PreferencesActivity.this.getApplicationContext();
            BackgroundDownloadService.updateJob(context);
        }
    }
}
