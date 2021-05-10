package app.crossword.yourealwaysbe.io;

import java.io.InputStream;
import java.time.LocalDate;

import org.json.JSONObject;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

public class IPuzIOTest extends TestCase {

    public IPuzIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/test.ipuz");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test & puzzle");
        assertEquals(puz.getAuthor(), "Test author");
        assertEquals(puz.getCopyright(), "Test copyright");
        assertEquals(puz.getSourceUrl(), "https://testurl.com");
        assertEquals(puz.getSource(), "Test publisher");
        assertEquals(puz.getDate(), LocalDate.of(2003,2,1));

        assertEquals(puz.getWidth(), 3);
        assertEquals(puz.getHeight(), 2);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), 1);
        assertEquals(boxes[0][1].getClueNumber(), 2);
        assertFalse(boxes[0][1].isCircled());
        assertEquals(boxes[0][2], null);
        assertEquals(boxes[1][0].getClueNumber(), 3);
        assertEquals(boxes[1][0].getResponse(), 'A');
        assertTrue(boxes[1][0].isCircled());

        assertTrue(boxes[0][0].isBlank());
        assertEquals(boxes[0][1].getResponse(), 'B');
        assertEquals(boxes[1][1].getResponse(), 'C');
        assertTrue(boxes[1][2].isBlank());

        assertEquals(boxes[0][0].getSolution(), 'A');
        assertEquals(boxes[0][1].getSolution(), 'B');
        assertEquals(boxes[1][0].getSolution(), 'A');
        assertEquals(boxes[1][1].getSolution(), 'C');
        assertEquals(boxes[1][2].getSolution(), 'D');

        ClueList acrossClues = puz.getClues(true);
        ClueList downClues = puz.getClues(false);

        assertEquals(acrossClues.getClue(1).getHint(), "Test clue 1");
        assertEquals(acrossClues.getClue(3).getHint(), "Test clue 2 (clues 3/2)");
        assertEquals(downClues.getClue(1).getHint(), "Test clue 3");
        assertEquals(
            downClues.getClue(2).getHint(),
            "Test clue 4 (cont. 1 Across/1 Down) "
                + "(ref. 1&2 Across) (clues 2/1/3) (3-2-1)"
        );
    }

    public void testIPuz() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }

}

