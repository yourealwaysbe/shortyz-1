package app.crossword.yourealwaysbe.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Heavily adapted from sources below, description no longer relevant so
 * removed.
 *
 * Original ource: https://gist.github.com/slightfoot/c508cdc8828a478572e0
 * Adapted for Shortyz by Simon Lightfoot <simon@demondevelopers.com>
  */
public class NightModeHelper {

    public enum Mode {
        DAY, NIGHT, SYSTEM;
    }

    private static int sUiNightMode = Configuration.UI_MODE_NIGHT_NO;
    private static String UI_THEME = "uiTheme";
    private static Mode DEFAULT_MODE = Mode.DAY;

    private WeakReference<AppCompatActivity> mActivity;
    private Mode currentMode;
    private SharedPreferences prefs;

    public static NightModeHelper bind(AppCompatActivity activity) {
        NightModeHelper helper = new NightModeHelper(activity);
        helper.restoreNightMode();

        // This may seem pointless but it forces the Theme to be reloaded
        // with new styles that would change due to new Configuration.
        int activityTheme = getTheme(activity);
        activity.setTheme(activityTheme);

        return helper;
    }

    private NightModeHelper(AppCompatActivity activity) {
        mActivity = new WeakReference<>(activity);
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        currentMode = Mode.valueOf(
            prefs.getString(UI_THEME, DEFAULT_MODE.name())
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void next() {
        switch (currentMode) {
        case DAY:
            setMode(Mode.NIGHT);
            break;
        case NIGHT:
            setMode(Mode.SYSTEM);
            break;
        case SYSTEM:
            setMode(Mode.DAY);
            break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void setMode(Mode mode) {
        currentMode = mode;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(UI_THEME, currentMode.name());
        editor.apply();

        restoreNightMode();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void restoreNightMode() {
        AppCompatActivity activity = mActivity.get();
        if (activity != null) {
            switch (currentMode) {
            case DAY:
                activity.getDelegate()
                        .setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case NIGHT:
                activity.getDelegate()
                        .setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SYSTEM:
                activity.getDelegate()
                        .setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            }
        }
    }

    public Mode getCurrentMode() {
        return currentMode;
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public boolean isNightMode(){
        switch (currentMode) {
        case DAY: return false;
        case NIGHT: return true;
        case SYSTEM: return getSystemNightMode();
        default: return false;
        }
    }

    public void unbind() {
        mActivity.clear();
    }

    @NonNull
    private AppCompatActivity getActivity() {
        AppCompatActivity activity = mActivity.get();
        if (activity == null) {
            throw new IllegalStateException("No activity is currently bound.");
        }
        return activity;
    }

    /**
     * Helper method to get the theme resource id. Warning, accessing non-public methods is
     * a no-no and there is no guarantee this will work.
     *
     * @param context the context you want to extract the theme-resource-id from
     * @return The themeId associated w/ the context
     */
    @Deprecated
    private static int getTheme(Context context) {
        try {
            Class<?> wrapper = Context.class;
            Method method = wrapper.getMethod("getThemeResId");
            method.setAccessible(true);
            return (Integer) method.invoke(context);
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Exception getting theme", e);
        }
        return 0;
    }

    private boolean getSystemNightMode() {
        AppCompatActivity activity = mActivity.get();
        if (activity == null)
            return false;

        int mode = (activity.getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK);

        return mode == Configuration.UI_MODE_NIGHT_YES;
    }
}
