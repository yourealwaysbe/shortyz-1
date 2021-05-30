package app.crossword.yourealwaysbe;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import android.content.ContentResolver;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.PuzzleStreamReader;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Takes an arbitrary URI and tries to convert to a puzzle and import
 */
public class PuzzleImporter {
    private static final Logger LOGGER
        = Logger.getLogger(PuzzleImporter.class.getCanonicalName());

    // date, name, uuid
    private static final String IMPORT_FILE_NAME_PATTERN
        = "%s-%s-%s";
    private static final String IMPORT_FALLBACK_SOURCE
        = ForkyzApplication.getInstance().getString(
            R.string.import_fallback_source
        );
    private static final String FILE_NAME_REMOVE_CHARS = "[^A-Za-z0-9]";

    /** Import from a URI supported by resolver
     *
     * Currently does not use file extension or MIME type. Instead, use puzlib
     * that tries each known format in turn until one succeeds. Clunky, but
     * hopefully robust.
     *
     * @return new puz handle if succeeded (will return null if failed
     * or uri is null)
     */
    public static PuzHandle importUri(ContentResolver resolver, Uri uri) {
        if (uri == null)
            return null;

        FileHandler fileHandler =
            ForkyzApplication.getInstance().getFileHandler();

        Puzzle puz = PuzzleStreamReader.parseInput(() -> {
            return new BufferedInputStream(resolver.openInputStream(uri));
        });

        if (puz == null)
            return null;

        if (puz.getSource() == null)
            puz.setSource(puz.getAuthor());
        if (puz.getSource() == null)
            puz.setSource(IMPORT_FALLBACK_SOURCE);

        try {
            return fileHandler.saveNewPuzzle(puz, getNewFileName(puz));
        } catch (IOException e) {
            LOGGER.severe("Failed to save imported puzzle: " + e);
            return null;
        }
    }

    public static String getNewFileName(Puzzle puzzle) {
        String name = puzzle.getSource();
        if (name == null || name.length() == 0)
            name = puzzle.getAuthor();
        if (name == null || name.length() == 0)
            name = puzzle.getTitle();
        if (name == null)
            name = IMPORT_FALLBACK_SOURCE;

        String normalizedName
            = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll(FILE_NAME_REMOVE_CHARS, "");

        return String.format(
            Locale.US,
            IMPORT_FILE_NAME_PATTERN,
            LocalDate.now(),
            normalizedName,
            UUID.randomUUID()
        );
    }
}
