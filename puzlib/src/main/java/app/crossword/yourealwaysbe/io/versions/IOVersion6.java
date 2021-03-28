package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Puzzle.HistoryItem;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

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
        saveNotes(dos, puz.getAcrossNotes());
        saveNotes(dos, puz.getDownNotes());
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
        DataOutputStream dos, Note[] notes
    ) throws IOException {
        if (notes == null) {
            dos.writeInt(0);
            return;
        }

        dos.writeInt(notes.length);

        for (Note note : notes) {
            String scratch = null;
            String text = null;
            String anagramSrc = null;
            String anagramSol = null;

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

    private void applyNotes(Puzzle puz, Note[] notes, boolean isAcross) {
        if (notes != null) {
            for (int i = 0; i < notes.length; i++) {
                Note n = notes[i];
                if (n != null)
                    puz.setNoteRaw(n, i, isAcross);
            }
        }
    }
}
