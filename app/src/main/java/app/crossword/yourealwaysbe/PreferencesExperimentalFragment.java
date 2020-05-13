package app.crossword.yourealwaysbe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;

import app.crossword.yourealwaysbe.forkyz.R;

public class PreferencesExperimentalFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_experimental, rootKey);

        findPreference("aboutScrapes")
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/scrapes.html"),
                            getActivity(), HTMLActivity.class);
                    getActivity().startActivity(i);

                    return true;
                }
            });
    }
}
