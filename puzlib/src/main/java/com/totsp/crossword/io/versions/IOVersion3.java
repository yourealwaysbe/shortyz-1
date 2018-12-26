package com.totsp.crossword.io.versions;

import com.totsp.crossword.io.IO;
import com.totsp.crossword.puz.Box;
import com.totsp.crossword.puz.Playboard.Position;
import com.totsp.crossword.puz.Puzzle;
import com.totsp.crossword.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

// Saves the current board position and clue orientation.
public class IOVersion3 extends IOVersion2 {
    private static final Logger LOG = Logger.getLogger(IOVersion3.class.getCanonicalName());

	@Override
	protected void applyMeta(Puzzle puz, PuzzleMeta meta){
		super.applyMeta(puz, meta);
		puz.setPosition(meta.position);
		puz.setAcross(meta.across);
	}

	@Override
	public PuzzleMeta readMeta(DataInputStream dis) throws IOException {
		PuzzleMeta meta = new PuzzleMeta();
		meta.author = IO.readNullTerminatedString(dis);
        LOG.info("Author: " + meta.author);
		meta.source = IO.readNullTerminatedString(dis);
        LOG.info("src: " + meta.source);
		meta.title = IO.readNullTerminatedString(dis);
        LOG.info("title: " + meta.title);
		meta.date = new Date( dis.readLong() );
        LOG.info("date: " + meta.date);
		meta.percentComplete = dis.readInt();
        LOG.info("pcnt: " + meta.percentComplete);
		meta.updatable = dis.read() == 1;
        LOG.info("upd: " + meta.updatable);
		meta.sourceUrl = IO.readNullTerminatedString(dis);
        LOG.info("url: " + meta.sourceUrl);
		int x = dis.readInt();
        LOG.info("x: " + x);
		int y = dis.readInt();
        LOG.info("y: " + y);
		meta.position = new Position(x, y);
		meta.across = dis.read() == 1;
        LOG.info("across: " + meta.across);
		return meta;
	}

	@Override
	public void write(Puzzle puz, DataOutputStream dos) throws IOException {
		IO.writeNullTerminatedString(dos, puz.getAuthor());
		IO.writeNullTerminatedString(dos, puz.getSource());
		IO.writeNullTerminatedString(dos, puz.getTitle());
		dos.writeLong(puz.getDate() == null ? 0 : puz.getDate().getTime());
		dos.writeInt(puz.getPercentComplete());
		dos.write(puz.isUpdatable() ? 1 : -1);
		IO.writeNullTerminatedString(dos, puz.getSourceUrl());
		Position p = puz.getPosition();
		if (p != null) {
			dos.writeInt(p.across);
			dos.writeInt(p.down);
		} else {
			dos.writeInt(0);
			dos.writeInt(0);
		}
		dos.write(puz.getAcross() ? 1 : -1);
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

}
