package app.crossword.yourealwaysbe.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.puz.Puzzle;

public class PuzzleStreamReader {
    private static final Logger LOGGER
        = Logger.getLogger(PuzzleStreamReader.class.getCanonicalName());

    private static final PuzzleParser[] PARSERS = {
        new IO(),
        new JPZIO(),
        new IPuzIO(),
        new UclickXMLIO(),
        new BrainsOnlyIO(),
        new KingFeaturesPlaintextIO()
    };

    public interface InputStreamSupplier {
        /**
         * Return a new input stream for the puzzle
         *
         * Will be consumed and closed by the reader
         */
        public InputStream get() throws IOException;
    }

    /**
     * Read the puzzle from the input stream, try multiple formats
     *
     * The supplier function should return an input stream for the
     * puzzle source. The method will try known file formats until it
     * finds one that parses to completion.
     */
    public static Puzzle parseInput(InputStreamSupplier isSupplier) {
        for (PuzzleParser parser : PARSERS) {
            try (InputStream is = isSupplier.get()) {
                Puzzle puz = parser.parseInput(is);
                if (puz != null)
                    return puz;
            } catch (Exception e) {
                LOGGER.info("Parse attempt failed with " + e);
                // on to the next one
            }
        }
        return null;
    }
}
