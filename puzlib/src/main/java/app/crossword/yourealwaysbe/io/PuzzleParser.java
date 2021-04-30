package app.crossword.yourealwaysbe.io;

import java.io.InputStream;

import app.crossword.yourealwaysbe.puz.Puzzle;

public interface PuzzleParser {
    /**
     * Parse puzzle from input stream
     *
     * Implementations of this method should make a reasonable effort to
     * detect when the input is not in the right format. E.g. do not
     * simply return an empty puzzle because the right XML tags were not
     * found.
     *
     * @return parse puzzle or null if failed or not in right format
     */
    public Puzzle parseInput(InputStream is) throws Exception;
}
