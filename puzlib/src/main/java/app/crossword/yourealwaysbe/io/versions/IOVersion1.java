package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;

public class IOVersion1 implements IOVersion {

    public void read(Puzzle puz, DataInputStream dis) throws IOException {
        PuzzleMeta meta = readMeta(dis);
        applyMeta(puz, meta);
        Box[][] boxes = puz.getBoxes();
        for(Box[] row : boxes ){
            for(Box b : row){
                if(b == null){
                    continue;
                }
                b.setCheated(dis.readBoolean());
                b.setResponder(IO.readNullTerminatedString(dis));
            }
        }
        try{
            puz.setTime(dis.readLong());
        }catch(IOException ioe){
            ioe.printStackTrace();
        }


    }

    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        //System.out.println("Applying V1 Meta");
        puz.setSource(meta.source);
        puz.setDate(meta.date);
    }

    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = new PuzzleMeta();
        meta.author = IO.readNullTerminatedString(dis);
        meta.source = IO.readNullTerminatedString(dis);
        meta.title = IO.readNullTerminatedString(dis);
        meta.date =
            Instant.ofEpochMilli(dis.readLong())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        meta.percentComplete = dis.readInt();
        meta.percentFilled = meta.percentComplete;
        return meta;
    }

    public void write(Puzzle puz, DataOutputStream dos) throws IOException {
        writeMeta(puz, dos);
        Box[][] boxes = puz.getBoxes();
        for(Box[] row : boxes ){
            for(Box b : row){
                if(b == null){
                    continue;
                }
                dos.writeBoolean(b.isCheated());
                IO.writeNullTerminatedString(dos, b.getResponder());
            }
        }
        dos.writeLong(puz.getTime());
    }

    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        IO.writeNullTerminatedString(dos, puz.getAuthor());
        IO.writeNullTerminatedString(dos, puz.getSource());
        IO.writeNullTerminatedString(dos, puz.getTitle());
        LocalDate date = puz.getDate();
        if (date == null) {
            dos.writeLong(0);
        } else {
            dos.writeLong(
                date.atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            );
        }
        dos.writeInt(puz.getPercentComplete());
    }
}
