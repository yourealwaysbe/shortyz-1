package app.crossword.yourealwaysbe.puz;

import app.crossword.yourealwaysbe.puz.Playboard.Clue;

import java.util.Objects;

public class HistoryItem {
    private Clue clue;
    private boolean across;

    public HistoryItem(Clue clue, boolean across) {
        this.clue = clue;
        this.across = across;
    }

    public Clue getClue() { return clue; }
    public boolean getAcross() { return across; }

    public int hashCode() {
        return Objects.hash(clue, across);
    }

    public boolean equals(Object o) {
        if (o instanceof HistoryItem) {
            HistoryItem other = (HistoryItem) o;
            if (clue == null && other.clue != null)
                return false;
            return clue.equals(other.clue) && across == other.across;
        }
        return false;
    }
}
