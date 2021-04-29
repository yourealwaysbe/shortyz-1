package app.crossword.yourealwaysbe.puz;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.io.UclickXMLIO;

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

    @Override
    public void setUp() throws Exception {
        super.setUp();
        is = UclickXMLIOTest.class.getResourceAsStream("/crnet091215-data.xml");
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

        assertEquals(TITLE, puz.getTitle());
        assertEquals(AUTHOR, puz.getAuthor());
        assertEquals(COPYRIGHT, puz.getCopyright());

        assertEquals("Film legend Greta", puz.findAcrossClue(1));
        assertEquals("Ballerina's skirt", puz.findAcrossClue(49));
        assertEquals("Equips for combat", puz.findAcrossClue(60));
        assertEquals("Double curves", puz.findAcrossClue(65));

        assertEquals("Squash or pumpkin", puz.findDownClue(1));
        assertEquals("Toss in", puz.findDownClue(21));
        assertEquals("Bullfight shouts", puz.findDownClue(56));
    }

}
