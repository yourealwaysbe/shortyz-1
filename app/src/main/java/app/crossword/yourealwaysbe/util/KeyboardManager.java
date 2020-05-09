/**
 * Manage on screen keyboard for Play/Notes/ClueList activity (and others)
 */

package app.crossword.yourealwaysbe;

import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;

import app.crossword.yourealwaysbe.forkyz.R;

public class KeyboardManager {
    private static final Logger LOG = Logger.getLogger(KeyboardView.class.getCanonicalName());

    private static final String KEYBOARD_TYPE = "keyboardType";

    private Activity activity;
    private Configuration configuration;
    private Handler handler = new Handler();
    private KeyboardView keyboardView;
	private SharedPreferences prefs;
    private boolean useNativeKeyboard;

    /**
     * Create a new manager to handle the keyboard for an activity.
     *
     * To use, pas on calls to the implemented methods below.
     *
     * @param activity the activity we're managing for
     * @param keyboardView the view on the layout to use for non-native keyboard
     */
    public KeyboardManager(Activity activity,
                           KeyboardView keyboardView) {
        this.activity = activity;
        this.keyboardView = keyboardView;

        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        configuration = activity.getBaseContext()
                                .getResources()
                                .getConfiguration();

        final Activity finalActivity = activity;
        View.OnKeyListener onKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                return keyEvent.getAction() == KeyEvent.ACTION_UP && finalActivity.onKeyUp(i, keyEvent);
            }
        };

        int keyboardType = "CONDENSED_ARROWS".equals(prefs.getString(
                KEYBOARD_TYPE, "")) ? R.xml.keyboard_dpad : R.xml.keyboard;
        Keyboard keyboard = new Keyboard(activity, keyboardType);
        keyboardView.setKeyboard(keyboard);
        useNativeKeyboard = "NATIVE".equals(prefs.getString(KEYBOARD_TYPE, ""));

        if (this.useNativeKeyboard) {
            keyboardView.setVisibility(View.GONE);
        }
        keyboardView.setOnKeyListener(onKeyListener);

        final KeyboardView finalKeyboardView = keyboardView;
        keyboardView
                .setOnKeyboardActionListener(new OnKeyboardActionListener() {
                    private long lastSwipe = 0;

                    public void onKey(int primaryCode, int[] keyCodes) {
                        long eventTime = System.currentTimeMillis();

                        if (finalKeyboardView.getVisibility() == View.GONE ||
                            (eventTime - lastSwipe) < 500) {
                            return;
                        }

                        KeyEvent event = new KeyEvent(eventTime, eventTime,
                                KeyEvent.ACTION_UP, primaryCode, 0, 0, 0,
                                0, KeyEvent.FLAG_SOFT_KEYBOARD
                                | KeyEvent.FLAG_KEEP_TOUCH_MODE);
                        finalActivity.onKeyUp(primaryCode, event);
                    }

                    public void onPress(int primaryCode) {
                    }

                    public void onRelease(int primaryCode) {
                    }

                    public void onText(CharSequence text) {
                    }

                    public void swipeDown() {
                        long eventTime = System.currentTimeMillis();
                        lastSwipe = eventTime;

                        KeyEvent event = new KeyEvent(eventTime, eventTime,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_DOWN, 0, 0, 0, 0,
                                KeyEvent.FLAG_SOFT_KEYBOARD
                                        | KeyEvent.FLAG_KEEP_TOUCH_MODE);
                        finalActivity.onKeyUp(KeyEvent.KEYCODE_DPAD_DOWN, event);
                    }

                    public void swipeLeft() {
                        long eventTime = System.currentTimeMillis();
                        lastSwipe = eventTime;

                        KeyEvent event = new KeyEvent(eventTime, eventTime,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT, 0, 0, 0, 0,
                                KeyEvent.FLAG_SOFT_KEYBOARD
                                        | KeyEvent.FLAG_KEEP_TOUCH_MODE);
                        finalActivity.onKeyUp(KeyEvent.KEYCODE_DPAD_LEFT, event);
                    }

                    public void swipeRight() {
                        long eventTime = System.currentTimeMillis();
                        lastSwipe = eventTime;

                        KeyEvent event = new KeyEvent(eventTime, eventTime,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_RIGHT, 0, 0, 0, 0,
                                KeyEvent.FLAG_SOFT_KEYBOARD
                                        | KeyEvent.FLAG_KEEP_TOUCH_MODE);
                        finalActivity.onKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT, event);
                    }

                    public void swipeUp() {
                        long eventTime = System.currentTimeMillis();
                        lastSwipe = eventTime;

                        KeyEvent event = new KeyEvent(eventTime, eventTime,
                                KeyEvent.ACTION_DOWN,
                                KeyEvent.KEYCODE_DPAD_UP, 0, 0, 0, 0,
                                KeyEvent.FLAG_SOFT_KEYBOARD
                                        | KeyEvent.FLAG_KEEP_TOUCH_MODE);
                        finalActivity.onKeyUp(KeyEvent.KEYCODE_DPAD_UP, event);
                    }
                });
    }

    public boolean getUseNativeKeyboard() {
        return useNativeKeyboard;
    }

    /**
     * Call this from the activities onResume method
     *
     * @param keyboardView the keyboard view after resumption
     */
    public void onResume(KeyboardView keyboardView) {
        int keyboardType = "CONDENSED_ARROWS".equals(prefs.getString(
                KEYBOARD_TYPE, "")) ? R.xml.keyboard_dpad : R.xml.keyboard;
        final Keyboard keyboard = new Keyboard(activity, keyboardType);
        keyboardView.setKeyboard(keyboard);
        this.useNativeKeyboard = "NATIVE".equals(prefs.getString(
                KEYBOARD_TYPE, ""));

        if (useNativeKeyboard) {
            keyboardView.setVisibility(View.GONE);
        }

        final KeyboardView finalKeyboardView = keyboardView;
        handler.post(new Runnable() {
            @Override
            public void run() {
                finalKeyboardView.invalidate();
                finalKeyboardView.invalidateAllKeys();
            }
        });
    }

    /**
     * Call whenever the activity is rendering
     */
    public void render() {
        showKeyboard();
    }

    /**
     * Call this when the activity configuration changes
     *
     * @param newConfig the new config passed to the activity
     */
    public void onConfigurationChanged(Configuration newConfig) {
        this.configuration = newConfig;
    }

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
        if (prefs.getBoolean("forceKeyboard", false) ||
            (configuration.hardKeyboardHidden
                == Configuration.HARDKEYBOARDHIDDEN_YES) ||
            (configuration.hardKeyboardHidden
                == Configuration.HARDKEYBOARDHIDDEN_UNDEFINED)) {
            if (useNativeKeyboard) {
                keyboardView.setVisibility(View.GONE);

                InputMethodManager imm
                    = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                        InputMethodManager.HIDE_NOT_ALWAYS);
            } else {
                this.keyboardView.setVisibility(View.VISIBLE);
            }
        } else {
            this.keyboardView.setVisibility(View.GONE);
        }
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
				if (!useNativeKeyboard) {
					if (gainFocus) {
						keyboardView.setVisibility(View.GONE);
					} else {
						keyboardView.setVisibility(View.VISIBLE);
					}
				}
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
        if (prefs.getBoolean("forceKeyboard", false))
            return;

        if ((configuration.hardKeyboardHidden
                 == Configuration.HARDKEYBOARDHIDDEN_YES) ||
            (configuration.hardKeyboardHidden
                 == Configuration.HARDKEYBOARDHIDDEN_UNDEFINED)) {
            View focus = activity.getCurrentFocus();
            if (focus != null) {
                InputMethodManager imm
                    = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
        }
        keyboardView.setVisibility(View.GONE);
    }

}
