package app.crossword.yourealwaysbe;

import java.io.BufferedInputStream;
import java.util.logging.Logger;

import android.content.ContentResolver;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.PuzzleStreamReader;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Takes an arbitrary URI and tries to convert to a puzzle and import
 */
public class PuzzleImporter {
    private static final Logger LOGGER
        = Logger.getLogger(PuzzleImporter.class.getCanonicalName());

    /** Import from a URI supported by resolver
     *
     * Currently does not use file extension or MIME type. Instead, use puzlib
     * that tries each known format in turn until one succeeds. Clunky, but
     * hopefully robust.
     *
     * @return true if succeeded (will return false if uri is null)
     */
    public static boolean importUri(ContentResolver resolver, Uri uri) {
        if (uri == null)
            return false;

        FileHandler fileHandler =
            ForkyzApplication.getInstance().getFileHandler(); FileHandle
            fileHandle = fileHandler.getFileHandle(uri);

        Puzzle puz = PuzzleStreamReader.parseInput(() -> {
            return new BufferedInputStream(resolver.openInputStream(uri));
        });

        if (puz == null)
            return false;

        // TODO: save puzzle
        LOGGER.info("FORKYZ: puzzle loaded from " + uri);

        return true;
    }
}
