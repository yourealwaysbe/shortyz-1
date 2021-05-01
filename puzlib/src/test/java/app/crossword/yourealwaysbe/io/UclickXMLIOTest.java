package app.crossword.yourealwaysbe.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Tests for UclickXMLIO.
 */
public class UclickXMLIOTest extends TestCase {
    private static final String TITLE = "12/15/09 LET'S BE HONEST";
    private static final String AUTHOR = "by Billie Truitt, edited by Stanley Newman";
    private static final LocalDate DATE = LocalDate.of(2009, 12, 15);
    private static final String COPYRIGHT = "Stanley Newman, distributed by Creators Syndicate, Inc.";

    private InputStream is;
    private DataOutputStream os;
    private File tmp;

    public UclickXMLIOTest(String testName) {
        super(testName);
    }

    public static InputStream getTestPuzzle1InputStream() {
        return UclickXMLIOTest.class.getResourceAsStream(
            "/crnet091215-data.xml"
        );
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        assertEquals(TITLE, puz.getTitle());
        assertEquals(AUTHOR, puz.getAuthor());

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals(1, boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isAcross());
        assertEquals(true, boxes[0][0].isDown());
        assertEquals(false, boxes[0][3].isAcross());

        assertEquals(boxes[0][0].getSolution(), 'G');
        assertEquals(boxes[5][14], null);
        assertEquals(boxes[14][14].getSolution(), 'S');
        assertEquals(boxes[14][5].getSolution(), 'L');
        assertEquals(boxes[3][6].getSolution(), 'N');

        String[] rawClues = puz.getRawClues();
        assertEquals(rawClues[0], "Film legend Greta");
        assertEquals(rawClues[5], "Rampaging");
        assertEquals(rawClues[7], "Get even for");
        assertEquals(rawClues[8], "Nickname for an NCO");
        assertEquals(rawClues[15], "Covered with rocks");
        assertEquals(rawClues[25], "Annoying noise");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        is = getTestPuzzle1InputStream();
        tmp = File.createTempFile("uclick-test", ".puz");
        os = new DataOutputStream(new FileOutputStream(tmp));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        is.close();
        os.close();
        tmp.delete();
    }

    public void testConvert() throws IOException {
        assertTrue(UclickXMLIO.convertUclickPuzzle(is, os, COPYRIGHT, DATE));
        Puzzle puz = null;

        try (
            DataInputStream dis = new DataInputStream(
                new FileInputStream(tmp)
            )
        ) {
            puz = IO.loadNative(dis);
        }

        assertIsTestPuzzle1(puz);
        assertEquals(COPYRIGHT, puz.getCopyright());
    }

}
