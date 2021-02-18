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
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;

public class KeyboardManager {
    private static final Logger LOG = Logger.getLogger(KeyboardManager.class.getCanonicalName());

    private Activity activity;
    private SharedPreferences prefs;
    private ForkyzKeyboard keyboardView;

    private enum KeyboardMode {
        ALWAYS_SHOW, SHOW_SPARINGLY, NEVER_SHOW
    }

    /**
     * Create a new manager to handle the keyboard
     *
     * To use, pass on calls to the implemented methods below.
     *
     * @param activity the activity the keyboard is for
     * @param keyboardView the keyboard view of the activity
     */
    public KeyboardManager(Activity activity, ForkyzKeyboard keyboardView) {
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.keyboardView = keyboardView;
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
    public void onPause() { }

    /**
     * Call this when the activity receives an onStop
     */
    public void onStop() { }

    /**
     * Call this when the activity receives an onDestroy
     */
    public void onDestroy() { }

    /**
     * Show the keyboard -- must be called after UI drawn
     *
     * @param view the view the keyboard should work for, will request
     * focus
     */
    public void showKeyboard(View view) {
        if (getKeyboardMode() != KeyboardMode.NEVER_SHOW
                && view != null
                && view.requestFocus()) {
            keyboardView.setVisibility(View.VISIBLE);
            keyboardView.attachToView(view);
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
        if (force || getKeyboardMode() != KeyboardMode.ALWAYS_SHOW)
            keyboardView.setVisibility(View.GONE);
    }

    private KeyboardMode getKeyboardMode() {
        String never = activity.getString(R.string.keyboard_never_show);
        String spare = activity.getString(R.string.keyboard_show_sparingly);
        String always = activity.getString(R.string.keyboard_always_show);

        String modePref = prefs.getString("keyboardShowHide", spare);

        if (never.equals(modePref))
            return KeyboardMode.NEVER_SHOW;
        else if (always.equals(modePref))
            return KeyboardMode.ALWAYS_SHOW;
        else
            return KeyboardMode.SHOW_SPARINGLY;
    }
}
