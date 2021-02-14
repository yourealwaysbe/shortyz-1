/**
 * Manage on screen keyboard for Play/Notes/ClueList activity (and others)
 */

package app.crossword.yourealwaysbe.util;

import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View.OnFocusChangeListener;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import app.crossword.yourealwaysbe.forkyz.R;

public class KeyboardManager {
    private static final Logger LOG = Logger.getLogger(KeyboardManager.class.getCanonicalName());

    private Activity activity;
    private SharedPreferences prefs;

    /**
     * Create a new manager to handle the keyboard
     *
     * To use, pass on calls to the implemented methods below.
     *
     * @param activity the activity the keyboard is for
     */
    public KeyboardManager(Activity activity) {
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    /**
     * Call this from the activities onResume method
     */
    public void onResume() {
        // do nothing
    }

    /**
     * Call this when the activity receives an onPause
     */
    public void onPause() { hideKeyboard(true); }

    /**
     * Call this when the activity receives an onStop
     */
    public void onStop() { hideKeyboard(true); }

    /**
     * Call this when the activity receives an onDestroy
     */
    public void onDestroy() { hideKeyboard(true); }

    /**
     * Show the keyboard -- must be called after UI drawn
     *
     * @param view the view the keyboard should work for, will request
     * focus
     */
    public void showKeyboard(View view) {
        if (view != null && view.requestFocus()) {
            InputMethodManager imm
                = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    public void hideKeyboard() { hideKeyboard(false); }

    /**
     * Hide the keyboard unless the user always wants it
     *
     * @param force force hide the keyboard, even if user has set always
     * show
     */
    public void hideKeyboard(boolean force) {
        boolean ignoreHide
            = !force && prefs.getBoolean("dontHideKeyboard", false);
        View focus = activity.getCurrentFocus();
        if (focus != null && !ignoreHide) {
            InputMethodManager imm
                = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
