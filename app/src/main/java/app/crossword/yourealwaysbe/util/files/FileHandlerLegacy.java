
package app.crossword.yourealwaysbe.util.files;

import java.util.logging.Logger;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

/**
 * Implementation of original Shortyz file access directly working with
 * external SD card directory.
 */
@SuppressWarnings("deprecation")
public class FileHandlerLegacy extends FileHandlerJavaFile {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerLegacy.class.getCanonicalName());

    public FileHandlerLegacy(Context applicationContext) {
        super(applicationContext, Environment.getExternalStorageDirectory());
    }

    @Override
    public boolean isStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()
        );
    }

    @Override
    public boolean isStorageFull() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            return isStorageFull18();
        else
            return isStorageFull1();
    }


    @TargetApi(18)
    private  boolean isStorageFull18() {
        StatFs stats = new StatFs(
            Environment.getExternalStorageDirectory().getAbsolutePath()
        );

        return (
            stats.getAvailableBlocksLong() * stats.getBlockSizeLong()
                < 1024L * 1024L
        );
    }

    @TargetApi(1)
    private  boolean isStorageFull1() {
        StatFs stats = new StatFs(
            Environment.getExternalStorageDirectory().getAbsolutePath()
        );

        long available
            = (long) stats.getAvailableBlocks() * (long) stats.getBlockSize();

        return (available < 1024L * 1024L);
    }
}
