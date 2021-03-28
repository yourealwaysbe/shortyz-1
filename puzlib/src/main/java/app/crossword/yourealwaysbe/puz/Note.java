package app.crossword.yourealwaysbe.puz;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.io.charset.Surrogate;

public class Note implements Serializable {
    private static final Logger LOG = Logger.getLogger(Note.class.getCanonicalName());
    private String scratch;
    private String text;
    private String anagramSource;
    private String anagramSolution;

    public Note(String scratch,
                String text,
                String anagramSource,
                String anagramSolution) {
        this.text = text;
        this.scratch = scratch;
        this.anagramSource = anagramSource;
        this.anagramSolution = anagramSolution;
    }

    public Note(int wordLength) {
        this.scratch = createBlankString(wordLength);
    }

    public String getText() {
        return text;
    }

    public String getScratch() {
        return scratch;
    }

    public String getAnagramSource() {
        return anagramSource;
    }

    public String getAnagramSolution() {
        return anagramSolution;
    }

    /**
     * Return null if the string is full of blanks
     */
    public String getCompressedScratch() {
        if (isBlankString(scratch))
            return null;
        else
            return scratch;
    }

    /**
     * Return null if the string is full of blanks
     */
    public String getCompressedAnagramSource() {
        if (isBlankString(anagramSource))
            return null;
        else
            return anagramSource;
    }

    /**
     * Return null if the string is full of blanks
     */
    public String getCompressedAnagramSolution() {
        if (isBlankString(anagramSolution))
            return null;
        else
            return anagramSolution;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setScratch(String scratch) {
        this.scratch = scratch;
    }

    public void setAnagramSource(String anagramSource) {
        this.anagramSource = anagramSource;
    }

    public void setAnagramSolution(String anagramSolution) {
        this.anagramSolution = anagramSolution;
    }

    public boolean isEmpty() {
        return (text == null || text.length() == 0) &&
               (scratch == null || scratch.trim().length() == 0) &&
               (anagramSource == null || anagramSource.trim().length() == 0) &&
               (anagramSolution == null || anagramSolution.trim().length() == 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Note) {
            Note n = (Note) o;
            return safeStringEquals(this.text, n.text) &&
                   safeStringEquals(this.scratch, n.scratch) &&
                   safeStringEquals(this.anagramSource, n.anagramSource) &&
                   safeStringEquals(this.anagramSolution, n.anagramSolution);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = (prime * result) + (text == null ? 0 : text.hashCode());
        result = (prime * result) +
                 (scratch == null ? 0 : scratch.hashCode());
        result = (prime * result) +
                 (anagramSource == null ? 0 : anagramSource.hashCode());
        result = (prime * result) +
                 (anagramSolution == null ? 0 : anagramSolution.hashCode());

        return result;
    }


    private static final boolean safeStringEquals(String s1, String s2) {
        if (s1 == null) {
            return (s2 == null);
        } else {
            return s1.equals(s2);
        }
    }

    private String createBlankString(int len) {
        if (len == 0) return "";

        char[] padding = new char[len];
        Arrays.fill(padding, Box.BLANK);
        return new String(padding);
    }

    private boolean isBlankString(String s) {
        if (s == null)
            return true;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != Box.BLANK)
                return false;
        }
        return true;
    }

    public void setScratchLetter(int pos, char letter) {
        String letterText = Character.toString(letter);
        String newScratchText;

        if (scratch == null) {
            LOG.warning("Can't set scratch letter because scratch text not created");
            return;
        }

        int len = scratch.length();
        if (pos == 0) {
            newScratchText = letterText + scratch.substring(1);
        } else if (pos == len - 1) {
            newScratchText = scratch.substring(0, pos) + letterText;
        } else {
            newScratchText = scratch.substring(0, pos) + letterText + scratch.substring(pos + 1);
        }
        scratch = newScratchText;
    }

    public void deleteScratchLetterAt(int pos) {
        setScratchLetter(pos, Box.BLANK);
    }
}
