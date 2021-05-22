package app.crossword.yourealwaysbe;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import android.content.ContentResolver;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.PuzzleStreamReader;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Takes an arbitrary URI and tries to convert to a puzzle and import
 */
public class PuzzleImporter {
    private static final Logger LOGGER
        = Logger.getLogger(PuzzleImporter.class.getCanonicalName());

    private static final String IMPORT_FILE_NAME_PATTERN
        = "import-%d-%s";
    private static final String IMPORT_FALLBACK_SOURCE
        = ForkyzApplication.getInstance().getString(
            R.string.import_fallback_source
        );

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
            ForkyzApplication.getInstance().getFileHandler();

        Puzzle puz = PuzzleStreamReader.parseInput(() -> {
            return new BufferedInputStream(resolver.openInputStream(uri));
        });

        if (puz == null)
            return false;

        if (puz.getSource() == null)
            puz.setSource(puz.getAuthor());
        if (puz.getSource() == null)
            puz.setSource(IMPORT_FALLBACK_SOURCE);

        try {
            return fileHandler.saveNewPuzzle(puz, getNewFileName());
        } catch (IOException e) {
            LOGGER.severe("Failed to save imported puzzle: " + e);
            return false;
        }
    }

    public static String getNewFileName() {
        return String.format(
            Locale.US,
            IMPORT_FILE_NAME_PATTERN,
            System.currentTimeMillis(),
            UUID.randomUUID()
        );
    }
}
