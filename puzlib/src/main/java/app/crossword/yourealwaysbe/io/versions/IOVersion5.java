package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Puzzle.HistoryItem;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

// Adds history list in format
// int length (int clue boolean across)*
public class IOVersion5 extends IOVersion4 {
    private static final Logger LOG = Logger.getLogger(IOVersion5.class.getCanonicalName());

    @Override
    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        super.applyMeta(puz, meta);
        puz.setHistory(meta.historyList);
    }

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
        PuzzleMeta meta = super.readMeta(dis);

        meta.historyList = new LinkedList<HistoryItem>();

        int length = dis.readInt();
        for (int i = 0; i < length; i++) {
            int number = dis.readInt();
            boolean across = dis.readBoolean();

            HistoryItem item = new HistoryItem(number, across);
            meta.historyList.add(item);
        }

        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);

        List<HistoryItem> history = puz.getHistory();

        dos.writeInt(history.size());
        for (HistoryItem item : puz.getHistory()) {
            dos.writeInt(item.getClueNumber());
            dos.writeBoolean(item.getAcross());
        }
    }
}
