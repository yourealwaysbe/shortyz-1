
package app.crossword.yourealwaysbe.util.files;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;
import androidx.annotation.RequiresApi;

/**
 * Class for SAF calculations that need high API
 *
 * Move these methods to a separate class as they cannot even be loaded pre
 * API 19. Don't want to burden FileHandlerSAF so.
 */
@RequiresApi(21)
public class SAFStorageCalculator {
    private static final Logger LOGGER
        = Logger.getLogger(SAFStorageCalculator.class.getCanonicalName());

    public static boolean isStorageFull(Context context, Uri rootUri) {
        // with thanks to https://stackoverflow.com/a/40848958
        Uri folderUri = DocumentsContract.buildDocumentUriUsingTree(
                rootUri,
                DocumentsContract.getTreeDocumentId(rootUri)
        );
        try {
            ParcelFileDescriptor pfd
                = context
                    .getContentResolver()
                    .openFileDescriptor(folderUri, "r");

            if (pfd != null) {
                StructStatVfs stats = Os.fstatvfs(pfd.getFileDescriptor());
                long availableBytes = stats.f_bavail * stats.f_bsize;
                return availableBytes < 1024L * 1024L;
            }
        } catch (FileNotFoundException | ErrnoException e) {
            LOGGER.info("Could not calculate storage available");
            e.printStackTrace();
            // fall through
        }

        // assume false if we failed to find out
        return false;
    }
}
