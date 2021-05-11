
package app.crossword.yourealwaysbe.puz;

import java.util.Collection;

public interface ClueList extends Iterable<Clue> {
    /**
     * Get clue by clue number (not index)
     */
    public Clue getClue(int number);

    /**
     * Get clues, iterator will go in clue order
     */
    public Collection<Clue> getClues();

    public boolean hasClue(int number);

    public int size();

    public int getFirstClueNumber();

    /**
     * Get the next clue after the given clue number
     *
     * Wraps back to beginning or returns -1 if no next number
     */
    public int getNextClueNumber(int number, boolean wrap);

    /**
     * Get the clue before the given clue number
     *
     * Wraps back to end or -1 if no previous
     */
    public int getPreviousClueNumber(int number, boolean wrap);

    /**
     * Returns index of clue in clue list
     */
    public int getClueIndex(int number);
}
