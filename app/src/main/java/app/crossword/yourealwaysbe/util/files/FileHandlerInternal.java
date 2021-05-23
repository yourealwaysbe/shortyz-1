
package app.crossword.yourealwaysbe.util.files;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;

/**
 * File storage in the default internal app storage location
 */
public class FileHandlerInternal extends FileHandlerJavaFile {
    private static final Logger LOGGER
        = Logger.getLogger(FileHandlerInternal.class.getCanonicalName());

    public FileHandlerInternal(Context applicationContext) {
        super(applicationContext, applicationContext.getFilesDir());
    }

    @Override
    public boolean isStorageMounted() {
        return true;
    }

    @Override
    public boolean isStorageFull() {
        try {
            Context context = getApplicationContext();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                StorageManager storageManager
                    = context.getSystemService(StorageManager.class);
                UUID appSpecificInternalDirUuid
                    = storageManager.getUuidForPath(context.getFilesDir());
                long availableBytes =
                    storageManager.getAllocatableBytes(
                        appSpecificInternalDirUuid
                    );
                return availableBytes < 1024L * 1024L;
            } else {
                File files = context.getFilesDir();
                return files.getFreeSpace() < 1024L * 1024L;
            }
        } catch (IOException e) {
            // we don't know it's not full...
            return false;
        }
    }
}
