/**
 * Manage on screen keyboard for Play/Notes/ClueList activity (and others)
 */

package app.crossword.yourealwaysbe;

import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;

import app.crossword.yourealwaysbe.forkyz.R;

public class KeyboardManager {
    private static final Logger LOG = Logger.getLogger(KeyboardManager.class.getCanonicalName());

    private Activity activity;

    /**
     * Create a new manager to handle the keyboard
     *
     * To use, pass on calls to the implemented methods below.
     *
     * @param activity the activity the keyboard is for
     */
    public KeyboardManager(Activity activity) {
        this.activity = activity;
    }

    /**
     * Call this from the activities onResume method
     */
    public void onResume() { }

    /**
     * Call this when the activity receives an onPause
     */
    public void onPause() {
        hideKeyboard();
    }

    /**
     * Call this when the activity receives an onStop
     */
    public void onStop() {
        hideKeyboard();
    }

    /**
     * Call this when the activity receives an onDestroy
     */
    public void onDestroy() {
        hideKeyboard();
    }

    /**
     * Show the keyboard
     */
    public void showKeyboard() {
        InputMethodManager imm
            = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * Hide the keyboard when the given view in focus
     *
     * @param view the view to disable the keyboard for
     */
    public void disableForView(View view) {
        view.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean gainFocus) {
                if (!gainFocus) {
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });
    }

    /**
     * Hide the keyboard unless the user always wants it
     */
    public void hideKeyboard() {
        View focus = activity.getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm
                = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
