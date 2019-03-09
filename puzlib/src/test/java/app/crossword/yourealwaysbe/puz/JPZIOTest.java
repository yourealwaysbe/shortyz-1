package app.crossword.yourealwaysbe.puz;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.io.JPZIO;

public class JPZIOTest extends TestCase{
	
	public JPZIOTest(String testName) {
        super(testName);
    }

	
	public void testLAT() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JPZIO.copyStream(JPZIOTest.class.getResourceAsStream("/lat_puzzle_111128.xml"), baos);
		System.out.println(new String(baos.toByteArray()));
		Puzzle puz = JPZIO.readPuzzle(new ByteArrayInputStream(baos.toByteArray()));
	}

}
