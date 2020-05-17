package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IOVersion2 extends IOVersion1 {

    @Override
    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        super.applyMeta(puz, meta);
        puz.setUpdatable(meta.updatable);
        puz.setSourceUrl(meta.sourceUrl);
    }

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException{
        PuzzleMeta meta = super.readMeta(dis);
        meta.updatable = dis.read() == 1;
        meta.sourceUrl = IO.readNullTerminatedString(dis);
        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);
        dos.write(puz.isUpdatable() ? 1 : -1);
        IO.writeNullTerminatedString(dos, puz.getSourceUrl());
    }
}
