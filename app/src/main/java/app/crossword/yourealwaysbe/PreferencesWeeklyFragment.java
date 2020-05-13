package app.crossword.yourealwaysbe;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import app.crossword.yourealwaysbe.forkyz.R;

public class PreferencesWeeklyFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_weekly, rootKey);
    }
}
