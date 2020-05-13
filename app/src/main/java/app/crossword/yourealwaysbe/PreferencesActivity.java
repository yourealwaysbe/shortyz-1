package app.crossword.yourealwaysbe;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceFragmentCompat;

import app.crossword.yourealwaysbe.forkyz.R;

public class PreferencesActivity
       extends AppCompatActivity
       implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preferences_activity);

        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.preferencesActivity,
                                            new PreferencesFragment())
                                   .commit();
    }

    // from https://developer.android.com/guide/topics/ui/settings/organize-your-settings
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller,
                                             Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment
            = getSupportFragmentManager().getFragmentFactory()
                                         .instantiate(getClassLoader(),
                                                      pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.preferencesActivity, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}
