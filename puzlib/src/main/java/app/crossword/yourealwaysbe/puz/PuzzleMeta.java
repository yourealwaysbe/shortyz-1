package app.crossword.yourealwaysbe.puz;

import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Puzzle.HistoryItem;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class PuzzleMeta implements Serializable {

    public String author;
    public String title;
    public String source;
    public LocalDate date;
    public int percentComplete;
    public int percentFilled;
    public boolean updatable;
    public String sourceUrl;
    public String supportUrl;
    public Position position;
    public boolean across;
    public List<HistoryItem> historyList;
    public Note[] acrossNotes;
    public Note[] downNotes;

    public String toString() {
        return new StringBuilder("author: ")
                .append(author)
                .append("title: ")
                .append(title)
                .append(" source: ")
                .append(source)
                .append(" sourceUrl: ")
                .append(sourceUrl)
                .append(" supportUrl: ")
                .append(supportUrl)
                .append(" date: ")
                .append(date)
                .append(" percentComplete: ")
                .append(percentComplete)
                .append(" percentFilled: ")
                .append(percentFilled)
                .append(" updatable: ")
                .append(updatable)
                .append(" position: ")
                .append(position)
                .append(" across: ")
                .append(across)
                .append(" history: ")
                .append(historyList)
                .append(" across notes: ")
                .append(Arrays.toString(acrossNotes))
                .append(" down notes: ")
                .append(Arrays.toString(downNotes))
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PuzzleMeta that = (PuzzleMeta) o;

        if (percentComplete != that.percentComplete) return false;
        if (percentFilled != that.percentFilled) return false;
        if (updatable != that.updatable) return false;
        if (across != that.across) return false;
        if (author != null ? !author.equals(that.author) : that.author != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (sourceUrl != null ? !sourceUrl.equals(that.sourceUrl) : that.sourceUrl != null)
            return false;
        if (supportUrl != null ? !supportUrl.equals(that.supportUrl) : that.supportUrl != null)
            return false;
        if (historyList != null ?
            !historyList.equals(that.historyList) : that.historyList != null)
            return false;
        if (!Arrays.equals(acrossNotes, that.acrossNotes))
            return false;
        if (!Arrays.equals(downNotes, that.downNotes))
            return false;
        return !(position != null ? !position.equals(that.position) : that.position != null);
    }

    @Override
    public int hashCode() {
        int result = author != null ? author.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + percentComplete;
        result = 31 * result + percentFilled;
        result = 31 * result + (updatable ? 1 : 0);
        result = 31 * result + (sourceUrl != null ? sourceUrl.hashCode() : 0);
        result = 31 * result + (supportUrl != null ? supportUrl.hashCode() : 0);
        result = 31 * result + (position != null ? position.hashCode() : 0);
        result = 31 * result + (across ? 1 : 0);
        result = 31 * result + (historyList != null ? historyList.hashCode() : 0);
        result = 31 * result + (acrossNotes != null ? Arrays.hashCode(acrossNotes) : 0);
        result = 31 * result + (downNotes != null ? Arrays.hashCode(downNotes) : 0);
        return result;
    }
}
