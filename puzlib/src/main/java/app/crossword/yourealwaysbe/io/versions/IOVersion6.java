package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Puzzle.ClueNumDir;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.SortedMap;

// Moves clue notes out of the puz file and into meta
// i don't really think they were allowed in the puz file format
public class IOVersion6 extends IOVersion5 {
    private static final Logger LOG = Logger.getLogger(
        IOVersion6.class.getCanonicalName()
    );

    @Override
    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        super.applyMeta(puz, meta);
        applyNotes(puz, meta.acrossNotes, true);
        applyNotes(puz, meta.downNotes, false);
    }

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = super.readMeta(dis);
        loadNotes(true, meta, dis);
        loadNotes(false, meta, dis);
        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);
        saveNotes(dos, puz, true);
        saveNotes(dos, puz, false);
    }

    /**
     * First an int, is num notes
     * Format of notes (all null-term strings):
     *     scratch, free-form, anagram src, anagram sol
     */
    public static void loadNotes(
        boolean isAcross, PuzzleMeta meta, DataInputStream input
    ) throws IOException {

        int numNotes = input.readInt();

        Note[] notes = new Note[numNotes];

        for (int x = 0; x < numNotes; x++) {
            String scratch = IO.readNullTerminatedString(input);
            String text = IO.readNullTerminatedString(input);
            String anagramSrc = IO.readNullTerminatedString(input);
            String anagramSol = IO.readNullTerminatedString(input);
            if (scratch != null
                    || text != null
                    || anagramSrc != null
                    || anagramSol != null) {
                notes[x] = new Note(scratch, text, anagramSrc, anagramSol);
            }
        }

        if (isAcross)
            meta.acrossNotes = notes;
        else
            meta.downNotes = notes;
    }

    private static void saveNotes(
        DataOutputStream dos, Puzzle puz, boolean isAcross
    ) throws IOException {

        int size = 0;
        for (ClueNumDir cnd : puz.getClueNumDirs()) {
            if (cnd.getAcross() == isAcross)
                size += 1;
        }

        // not really useful since notes should match the number of clue
        // positions
        dos.writeInt(size);

        for (ClueNumDir cnd : puz.getClueNumDirs()) {
            if (cnd.getAcross() == isAcross) {
                String scratch = null;
                String text = null;
                String anagramSrc = null;
                String anagramSol = null;

                Note note = puz.getNote(cnd.getClueNumber(), isAcross);

                if (note != null) {
                    scratch = note.getCompressedScratch();
                    text = note.getText();
                    anagramSrc = note.getCompressedAnagramSource();
                    anagramSol = note.getCompressedAnagramSolution();
                }

                IO.writeNullTerminatedString(dos, scratch);
                IO.writeNullTerminatedString(dos, text);
                IO.writeNullTerminatedString(dos, anagramSrc);
                IO.writeNullTerminatedString(dos, anagramSol);
            }
        }
    }

    private void applyNotes(Puzzle puz, Note[] notes, boolean isAcross) {
        if (notes != null) {
            int idx = 0;
            for (ClueNumDir cnd : puz.getClueNumDirs()) {
                int number = cnd.getClueNumber();
                boolean across = cnd.getAcross();

                if (across == isAcross) {
                    if (idx < notes.length) {
                        Note n = notes[idx];
                        if (n != null)
                            puz.setNote(number, n, isAcross);
                        idx += 1;
                    } else {
                        LOG.info(
                            "WARNING: mismatch between number of "
                                + "clues and number of notes."
                        );
                    }
                }
            }
        }
    }
}
