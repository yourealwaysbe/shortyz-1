package app.crossword.yourealwaysbe.puz;

import app.crossword.yourealwaysbe.puz.Playboard.Clue;

import java.util.Objects;

public class HistoryItem {
    private Clue clue;
    private boolean across;
    private int index;

    public HistoryItem(Clue clue, boolean across, int index) {
        this.clue = clue;
        this.across = across;
        this.index = index;
    }

    public Clue getClue() { return clue; }
    public boolean getAcross() { return across; }
    public int getIndex() { return index; }

    public int hashCode() {
        return Objects.hash(clue, across, index);
    }

    public boolean equals(Object o) {
        if (o instanceof HistoryItem) {
            HistoryItem other = (HistoryItem) o;
            if (clue == null && other.clue != null)
                return false;
            return clue.equals(other.clue) &&
                   across == other.across &&
                   index == other.index;
        }
        return false;
    }
}
