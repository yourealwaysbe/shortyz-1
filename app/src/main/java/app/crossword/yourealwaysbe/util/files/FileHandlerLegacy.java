
package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public FileHandlerLegacy() {
        super(Environment.getExternalStorageDirectory());
    }

    @Override
    public boolean isStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(
            Environment.getExternalStorageState()
        );
    }

    @Override
    public boolean isStorageFull() {
        StatFs stats = new StatFs(
            Environment.getExternalStorageDirectory().getAbsolutePath()
        );

        return (
            stats.getAvailableBlocksLong() * stats.getBlockSizeLong()
                < 1024L * 1024L
        );
    }
}
