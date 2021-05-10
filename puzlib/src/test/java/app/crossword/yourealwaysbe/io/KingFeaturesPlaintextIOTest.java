package app.crossword.yourealwaysbe.io;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStream;
import java.time.LocalDate;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Tests for KingFeaturesPlaintextIO.
 */
public class KingFeaturesPlaintextIOTest extends TestCase {

    private static final String TITLE = "Premier Crossword";
    private static final String AUTHOR = "Donna J. Stone";
    private static final String COPYRIGHT = "\u00a9 2010 King Features Syndicate, Inc.";
    private static final LocalDate DATE = LocalDate.of(2010, 7, 4);

    private InputStream is;
    private DataOutputStream os;
    private File tmp;

    public KingFeaturesPlaintextIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return KingFeaturesPlaintextIOTest.class.getResourceAsStream(
            "/premiere-20100704.txt"
        );
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        Box[][] boxes = puz.getBoxes();

        assertEquals(21, boxes.length);
        assertEquals(21, boxes[0].length);
        assertEquals(1, boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isAcross());
        assertEquals(true, boxes[0][0].isDown());
        assertEquals(false, boxes[0][3].isAcross());

        assertEquals(boxes[0][0].getSolution(), 'F');
        assertEquals(boxes[5][14].getSolution(), 'E');
        assertEquals(boxes[14][14].getSolution(), 'E');
        assertEquals(boxes[14][5].getSolution(), 'R');
        assertEquals(boxes[1][7], null);

        String[] rawClues = puz.getRawClues();
        assertEquals(rawClues[0], "Murals on plaster");
        assertEquals(rawClues[5], "One preserving fruit, e.g.");
        assertEquals(rawClues[7], "In stitches");
        assertEquals(rawClues[8], "Glucose-level regulator");
        assertEquals(rawClues[15], "Napoleonic marshal Michel");
        assertEquals(rawClues[25], "Cocky retort to a bully");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        is = KingFeaturesPlaintextIOTest.class.getResourceAsStream("/premiere-20100704.txt");
        tmp = File.createTempFile("kfp-test", ".puz");
        os = new DataOutputStream(new FileOutputStream(tmp));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        is.close();
        os.close();
        tmp.delete();
    }

    public void testConvert() {
        assertTrue(KingFeaturesPlaintextIO.convertKFPuzzle(is, os, TITLE, AUTHOR, COPYRIGHT, DATE));
        Puzzle puz = null;
        try (
            InputStream is = new FileInputStream(tmp)
        ) {
            puz = IO.loadNative(is);
        } catch (IOException e) {
            e.printStackTrace();
            fail("IO Error in IO.load - " + e.getMessage());
        }

        assertIsTestPuzzle1(puz);

        assertEquals(TITLE, puz.getTitle());
        assertEquals(AUTHOR, puz.getAuthor());
        assertEquals(COPYRIGHT, puz.getCopyright());
    }

}
