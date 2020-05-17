package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

// Adds history list in format
// int length (int clue boolean across)*
public class IOVersion5 extends IOVersion4 {
    private static final Logger LOG = Logger.getLogger(IOVersion5.class.getCanonicalName());

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = super.readMeta(dis);
        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);
    }
}
