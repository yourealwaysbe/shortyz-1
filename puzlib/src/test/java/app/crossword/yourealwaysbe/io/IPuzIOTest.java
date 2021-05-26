package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Puzzle.ClueNumDir;
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

    /**
     * Test HTML in various parts of puzzle
     */
    public static InputStream getTestPuzzleHTMLInputStream() {
        return JPZIOTest.class.getResourceAsStream("/html.ipuz");
    }

    public static void assertIsTestPuzzleHTML(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test & puzzle\nFor testing");
        assertEquals(puz.getAuthor(), "Test author\nForTest");
        assertEquals(puz.getSource(), "Test \u00A0\u00A0publisher\ntesttest");

        ClueList acrossClues = puz.getClues(true);

        assertEquals(
            acrossClues.getClue(1).getHint(),
            "Test clue 1\nA clue!"
        );
    }

    public void testIPuz() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }

    public void testIPuzWriteRead() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzReadPlayWriteRead() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            puz.setSupportUrl("http://test.url");
            puz.setTime(1234L);
            puz.setPosition(new Position(1, 2));
            puz.setAcross(false);

            puz.updateHistory(3, true);
            puz.updateHistory(1, false);

            puz.setNote(
                1,
                new Note("test1", "test2", "test3", "test4"),
                true
            );
            puz.setNote(
                2,
                new Note("test5", "test6\nnew line", "test7", "test8"),
                false
            );

            Box[][] boxes = puz.getBoxes();

            boxes[0][1].setResponse('X');
            boxes[1][2].setResponse('Y');
            boxes[0][1].setResponder("Test");
            boxes[1][0].setCheated(true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            Box[][] boxes2 = puz2.getBoxes();

            assertEquals(puz2.getSupportUrl(), "http://test.url");
            assertEquals(puz2.getTime(), 1234L);
            assertEquals(puz.getPosition(), puz2.getPosition());
            assertFalse(puz.getAcross());
            assertEquals(puz.getHistory().get(0), new ClueNumDir(1, false));
            assertEquals(puz.getHistory().get(1), new ClueNumDir(3, true));
            assertEquals(puz.getNote(1, true).getText(), "test2");
            assertEquals(puz.getNote(2, false).getText(), "test6\nnew line");
            assertEquals(puz.getNote(2, false).getAnagramSource(), "test7");
            assertEquals(boxes2[0][1].getResponse(), 'X');
            assertEquals(boxes2[1][2].getResponse(), 'Y');
            assertEquals(boxes2[0][1].getResponder(), "Test");
            assertFalse(boxes2[0][1].isCheated());
            assertTrue(boxes2[1][0].isCheated());

            assertEquals(puz, puz2);
        }
    }

    public void testIPuzHTML() throws Exception {
        try (InputStream is = getTestPuzzleHTMLInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);
            assertIsTestPuzzleHTML(puz);
        }
    }

    public void testIPuzWriteReadHTML() throws Exception {
        try (InputStream is = getTestPuzzleHTMLInputStream()) {
            Puzzle puz = IPuzIO.readPuzzle(is);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IPuzIO.writePuzzle(puz, baos);
            baos.close();

            ByteArrayInputStream bais
                = new ByteArrayInputStream(baos.toByteArray());

            Puzzle puz2 = IPuzIO.readPuzzle(bais);

            assertEquals(puz, puz2);
        }
    }
}

