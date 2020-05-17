package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

// Adds percentFilled
public class IOVersion4 extends IOVersion3 {
    private static final Logger LOG = Logger.getLogger(IOVersion4.class.getCanonicalName());

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = super.readMeta(dis);
        meta.percentFilled = dis.readInt();
        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);
        dos.writeInt(puz.getPercentFilled());
    }
}
