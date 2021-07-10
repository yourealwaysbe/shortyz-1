package app.crossword.yourealwaysbe.forkyz;

import android.Manifest;
import android.app.Application;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.puz.MovementStrategy;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.util.files.FileHandlerInternal;
import app.crossword.yourealwaysbe.util.files.FileHandlerLegacy;
import app.crossword.yourealwaysbe.util.files.FileHandlerSAF;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;

import java.io.IOException;
import java.util.logging.Logger;

public class ForkyzApplication extends Application {
    private static final Logger LOGGER
        = Logger.getLogger(ForkyzApplication.class.getCanonicalName());

    public static final String PUZZLE_DOWNLOAD_CHANNEL_ID = "forkyz.downloads";
    public static final String STORAGE_LOC_PREF = "storageLocation";

    private static ForkyzApplication INSTANCE;
    private SharedPreferences settings;

    private FileHandler fileHandler;

    private OnSharedPreferenceChangeListener prefChangeListener
        = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(
                SharedPreferences prefs, String key
            ) {
                if (STORAGE_LOC_PREF.equals(key)
                        || FileHandlerSAF.SAF_ROOT_URI_PREF.equals(key)) {
                    Toast t = Toast.makeText(
                        ForkyzApplication.this,
                        R.string.storage_changed_please_restart,
                        Toast.LENGTH_LONG
                    );
                    t.show();
                    ForkyzApplication.this.setFileHander();
                }
            }
        };

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    @Override
    public void onCreate() {
        INSTANCE = this;
        // Initialize credentials and service object.
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        initialiseSettings();
        setFileHander();

        settings.registerOnSharedPreferenceChangeListener(prefChangeListener);

        super.onCreate();

        AndroidVersionUtils.Factory.getInstance().createNotificationChannel(this);
    }

    public static boolean isLandscape(DisplayMetrics metrics){
        return metrics.widthPixels > metrics.heightPixels;
    }

    public static boolean isTabletish(DisplayMetrics metrics) {
        double x = Math.pow(metrics.widthPixels / metrics.xdpi, 2);
        double y = Math.pow(metrics.heightPixels / metrics.ydpi, 2);
        double screenInches = Math.sqrt(x + y);
        if (screenInches > 9) { // look for a 9" or larger screen.
            return true;
        } else {
            return false;
        }
    }

    public static boolean isMiniTabletish(DisplayMetrics metrics) {
        switch (Build.VERSION.SDK_INT) {
        case 12:
        case 11:
        case 13:
        case 14:
        case 15:
        case 16:
            double x = Math.pow(metrics.widthPixels/metrics.xdpi,2);
            double y = Math.pow(metrics.heightPixels/metrics.ydpi,2);
            double screenInches = Math.sqrt(x+y);
            if (screenInches > 5.5 && screenInches <= 9) {
                return true;
            } else {
                return false;
            }
        default:
            return false;
        }
    }

    public SharedPreferences getSettings() {
        return settings;
    }

    public static ForkyzApplication getInstance(){
        return INSTANCE;
    }

    public boolean isInternalStorage() {
        String internalStorage = getString(R.string.internal_storage);
        String locPref
            = settings.getString(STORAGE_LOC_PREF, internalStorage);
        return locPref.equals(internalStorage);
    }

    public boolean isLegacyStorage() {
        String internalStorage = getString(R.string.internal_storage);
        String legacyStorage = getString(R.string.external_storage_legacy);
        String locPref
            = settings.getString(STORAGE_LOC_PREF, internalStorage);
        return locPref.equals(legacyStorage);
    }

    public boolean isMissingWritePermission() {
        return isLegacyStorage()
            && ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED;
    }

    public MovementStrategy getMovementStrategy() {
        String stratName = settings.getString(
            "movementStrategy", "MOVE_NEXT_ON_AXIS"
        );
        switch (stratName) {
        case "MOVE_NEXT_ON_AXIS":
            return MovementStrategy.MOVE_NEXT_ON_AXIS;
        case "STOP_ON_END":
            return MovementStrategy.STOP_ON_END;
        case "MOVE_NEXT_CLUE":
            return MovementStrategy.MOVE_NEXT_CLUE;
        case "MOVE_PARALLEL_WORD":
            return MovementStrategy.MOVE_PARALLEL_WORD;
        default:
            LOGGER.severe("Unknown MovementStrategy " + stratName);
            return null;
        }
    }

    private void setFileHander() {
        String locPref
            = settings.getString(
                STORAGE_LOC_PREF, getString(R.string.internal_storage)
            );

        if (locPref.equals(getString(R.string.external_storage_legacy))) {
            fileHandler = new FileHandlerLegacy(this);
        } else if (locPref.equals(getString(R.string.external_storage_saf))) {
            fileHandler = FileHandlerSAF.readHandlerFromPrefs(this);
        } else {
            fileHandler = new FileHandlerInternal(this);
        }

        if (fileHandler == null || !fileHandler.isStorageMounted()) {
            fileHandler = new FileHandlerInternal(this);
            Toast t = Toast.makeText(
                ForkyzApplication.this,
                R.string.storage_problem_falling_back_internal,
                Toast.LENGTH_LONG
            );
            t.show();
        }

        if (fileHandler.isStorageFull()) {
            Toast t = Toast.makeText(
                ForkyzApplication.this,
                R.string.storage_full_warning,
                Toast.LENGTH_LONG
            );
            t.show();
        }
    }

    /**
     * Makes sure any settings needed are there
     *
     * In particular, storage location is set up before any change listeners
     * are set. Otherwise they will be called the first time the user goes to
     * the settings screen.
     */
    private void initialiseSettings() {
        String storageLoc = settings.getString(STORAGE_LOC_PREF, null);

        if (storageLoc == null) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(
                STORAGE_LOC_PREF, getString(R.string.internal_storage)
            );
            editor.apply();
        }
    }
}
