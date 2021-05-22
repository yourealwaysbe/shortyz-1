package app.crossword.yourealwaysbe.versions;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;

public abstract class DefaultUtil implements AndroidVersionUtils {
    public abstract void setContext(Context ctx);

    public abstract void onActionBarWithText(MenuItem a);

    public abstract void onActionBarWithText(SubMenu reveal);

    public void hideWindowTitle(AppCompatActivity a) {
        a.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public abstract void onActionBarWithoutText(MenuItem a);


    public abstract boolean checkBackgroundDownload(SharedPreferences prefs, boolean hasWritePermissions);

    public abstract void clearBackgroundDownload(SharedPreferences prefs);

}
