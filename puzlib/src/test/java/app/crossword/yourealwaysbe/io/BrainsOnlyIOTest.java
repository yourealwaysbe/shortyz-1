package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Puzzle;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Created with IntelliJ IDEA.
 * User: keber_000
 * Date: 2/9/14
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class BrainsOnlyIOTest  extends TestCase {

    public static InputStream getTestPuzzle1InputStream() {
        return BrainsOnlyIOTest.class.getResourceAsStream("/brainsonly.txt");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        assertEquals("SODA SPEAK", puz.getTitle());
        assertEquals(
            "S.N. & Robert Francis, edited by Stanley Newman",
            puz.getAuthor()
        );

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals(1, boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isAcross());
        assertEquals(true, boxes[0][0].isDown());
        assertEquals(false, boxes[0][3].isAcross());

        assertEquals(boxes[0][0].getSolution(), 'D');
        assertEquals(boxes[5][14].getSolution(), 'Y');
        assertEquals(boxes[14][14].getSolution(), 'P');
        assertEquals(boxes[14][5], null);
        assertEquals(boxes[3][6], null);

        assertEquals("Toss out", puz.getAcrossClues()[0]);
        assertEquals("Sancho Panza's mount", puz.getDownClues()[0]);
        assertEquals("Straighten out", puz.findAcrossClue(41));
    }

    public void testParse() throws Exception {
        Puzzle puz = BrainsOnlyIO.parse(getTestPuzzle1InputStream());
        assertIsTestPuzzle1(puz);
    }

    public void testParse2() throws Exception {

        Puzzle puz = BrainsOnlyIO.parse(BrainsOnlyIOTest.class.getResourceAsStream("/brainsonly2.txt"));
        assertEquals("OCCUPIED NATIONS: Surrounding the long answers", puz.getTitle());
        System.out.println("Across clue 15 "+ puz.findAcrossClue(15) );
        assertEquals("Elevator guy", puz.findAcrossClue(15));
        System.out.println("5 across "+puz.findAcrossClue(5));
        assertEquals("Company with a duck mascot", puz.findAcrossClue(5));

    }

    public void testParse3() throws Exception {
        try {
            // This was from http://brainsonly.com/servlets-newsday-crossword/newsdaycrossword?date=150903
            BrainsOnlyIO.parse(BrainsOnlyIOTest.class.getResourceAsStream("/brainsonly3.txt"));
        } catch (IOException e) {
            return;
        }
        fail("Expected brainsonly3.txt to fail to parse");
    }
}
