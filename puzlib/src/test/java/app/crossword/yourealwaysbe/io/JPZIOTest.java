package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

public class JPZIOTest extends TestCase {

    public JPZIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/lat_puzzle_111128.xml");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        assertEquals("LA Times, Mon, Nov 28, 2011", puz.getTitle());
        assertEquals("Jeff Chen / Ed. Rich Norris", puz.getAuthor());
        assertEquals("(c) 2011 Tribune Media Services, Inc.", puz.getCopyright());
        assertEquals(
            "Test\n\n\n"
                + "Down:\n\n"
                + "22: Shower Heads v7\n"
                + "61: I'm NOT going to ATTEND it / I'm going to SKIP it\n",
            puz.getNotes()
        );

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals(1, boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isAcross());
        assertEquals(true, boxes[0][0].isDown());
        assertEquals(false, boxes[0][3].isAcross());

        assertEquals(boxes[0][0].getSolution(), 'C');
        assertEquals(boxes[5][14].getSolution(), 'Y');
        assertEquals(boxes[14][14].getSolution(), 'S');
        assertEquals(boxes[14][5], null);
        assertEquals(boxes[3][6].getSolution(), 'N');

        ClueList acrossClues = puz.getClues(true);
        ClueList downClues = puz.getClues(false);

        assertEquals(acrossClues.getClue(1).getHint(), "Baby bovine (4)");
        assertEquals(acrossClues.getClue(5).getHint(), "At the drop of __ (4)");
        assertEquals(
            acrossClues.getClue(13).getHint(),
            "Ice cream-and-cookies brand (4)"
        );
        assertEquals(acrossClues.getClue(23).getHint(), "Stat start");
        assertEquals(
            downClues.getClue(6).getHint(),
            "Schmooze, as with the A-list (6)"
        );
        assertEquals(downClues.getClue(7).getHint(), "Work like __ (4)");
    }

    public void testJPZ() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IO.copyStream(getTestPuzzle1InputStream(), baos);
        System.out.println(new String(baos.toByteArray()));
        Puzzle puz = JPZIO.readPuzzle(
            new ByteArrayInputStream(baos.toByteArray())
        );
        assertIsTestPuzzle1(puz);
    }
}
