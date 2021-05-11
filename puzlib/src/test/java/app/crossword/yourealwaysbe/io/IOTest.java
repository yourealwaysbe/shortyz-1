/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;

import junit.framework.TestCase;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

/**
 *
 * @author kebernet
 */
public class IOTest extends TestCase {

    public IOTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/test.puz");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        assertEquals("NY Times, Fri, Nov 13, 2009", puz.getTitle());
        assertEquals("Dana Motley / Will Shortz", puz.getAuthor());
        assertEquals("© 2009, The New York Times", puz.getCopyright());

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals(1, boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isAcross());
        assertEquals(true, boxes[0][0].isDown());
        assertEquals(false, boxes[0][3].isAcross());

        assertEquals(boxes[0][0].getSolution(), 'R');
        assertEquals(boxes[5][14], null);
        assertEquals(boxes[14][14].getSolution(), 'S');
        assertEquals(boxes[14][5].getSolution(), 'T');
        assertEquals(boxes[3][6].getSolution(), 'E');

        ClueList acrossClues = puz.getClues(true);
        ClueList downClues = puz.getClues(false);

        assertEquals(acrossClues.getClue(1).getHint(), "Bring to perfection");
        assertEquals(acrossClues.getClue(23).getHint(), "Surprised reaction");
        assertEquals(
            downClues.getClue(5).getHint(),
            "Sch. whose sports teams are the Violets"
        );
        assertEquals(downClues.getClue(6).getHint(), "Not work at all");
        assertEquals(downClues.getClue(7).getHint(), "Kale kin");
        assertEquals(
            downClues.getClue(13).getHint(),
            "President who was born a King"
        );
    }

    /**
     * Test of load method, of class IO.
     */
    public void testLoad() throws Exception {
        try (
            InputStream is = getTestPuzzle1InputStream();
        ) {
            Puzzle puz = IO.loadNative(is);
            assertIsTestPuzzle1(puz);
        }
    }

    public void testSave() throws Exception {
        try (
            InputStream is = IOTest.class.getResourceAsStream("/test.puz")
        ) {
            Puzzle puz = IO.loadNative(is);
            System.out.println("Loaded.");
            File tmp = File.createTempFile("test", ".puz");
            tmp.deleteOnExit();

            try (
                OutputStream os = new FileOutputStream(tmp)
            ) {
                IO.saveNative(puz, os);
            }

            try (
                InputStream is2 = new FileInputStream(tmp)
            ) {
                Puzzle puz2 = IO.loadNative(is2);
                Box[][] b1 = puz.getBoxes();
                Box[][] b2 = puz2.getBoxes();

                for(int x=0; x < b1.length; x++ ){
                    for(int y=0; y<b1[x].length; y++){
                        System.out.println(b1[x][y] +" == "+ b2[x][y] );
                    }
                }

                assertEquals(puz, puz2);
            }

            try (
                InputStream isp = new FileInputStream(tmp)
            ) {
                Puzzle p = IO.loadNative(isp);
                p.setDate(LocalDate.now());
                p.setSource("Unit Test");

                File metaFile = new File(
                    tmp.getParentFile(),
                    tmp.getName().substring(
                        0, tmp.getName().lastIndexOf(".")
                    ) + ".forkyz"
                );
                metaFile.deleteOnExit();

                try (
                    OutputStream puzOS = new FileOutputStream(tmp);
                    OutputStream metaOS = new FileOutputStream(metaFile)
                ) {
                    IO.save(p, puzOS, metaOS);
                }

                try (
                    InputStream ism = new FileInputStream(metaFile)
                ) {
                    PuzzleMeta m = IO.readMeta(ism);
                    System.out.println(
                        m.title +"\n"+m.source+"\n"+m.percentComplete
                    );
                }
            }
        }
    }

    public void testGext() throws Exception{
        System.out.println("GEXT Test --------------------------");

        try (
            InputStream is = IOTest.class.getResourceAsStream(
                "/2010-7-4-LosAngelesTimes.puz"
            )
        ) {
            Puzzle puz = IO.loadNative(is);
            File tmp = File.createTempFile("test", ".puz");
            tmp.deleteOnExit();

            try (
                OutputStream dos = new FileOutputStream(tmp)
            ) {
                IO.saveNative(puz, dos);
                try (
                    InputStream is2 = new FileInputStream(tmp)
                ) {
                    puz = IO.loadNative(is2);
                    assertTrue(puz.getBoxes()[2][2].isCircled());
                }
            }
        }
    }

    public void testCrack() throws Exception {
        System.out.println("testCrack");
        try (
            InputStream is =
                IOTest.class.getResourceAsStream("/puz_110523margulies.puz")
        ) {
            Puzzle p = IO.loadNative(is);
            {
                Playboard board = new Playboard(p);
                for(Clue c : board.getPuzzle().getClues(true)){
                    for(Box box : board.getWordBoxes(c.getNumber(), true)){
                        System.out.print(box.getSolution());
                    }
                    System.out.println();
                }
            }
            System.out.println("========================");

            long incept = System.currentTimeMillis();
            boolean b = IO.crack(p);
            System.out.println(b + " "+(System.currentTimeMillis() - incept));
            Playboard board = new Playboard(p);
            for(Clue c : board.getPuzzle().getClues(true)){
                for(Box box : board.getWordBoxes(c.getNumber(), true)){
                    System.out.print(box.getSolution());
                }
                System.out.println();
            }
            System.out.println(b + " "+(System.currentTimeMillis() - incept));
        }
    }

    /**
     * Note: This is a sanity check, but any changes to unlock functionality should be tested more extensively.
     */
    public void testUnlockCode() throws Exception {
        try (
            InputStream is =
                IOTest.class.getResourceAsStream("/2010-7-19-NewYorkTimes.puz")
        ) {
            Puzzle puz = IO.loadNative(is);
            for(Box b :  puz.getBoxesList()){
                if(b != null)
                System.out.print(b.getSolution()+" ");
            }
            System.out.println();
            try{
                assertTrue(IO.tryUnscramble(
                    puz, 2465, puz.initializeUnscrambleData())
                );
                for(Box b :  puz.getBoxesList()){
                    if(b != null)
                    System.out.print(b.getSolution()+" ");
                }
                System.out.println();

                try (
                    ObjectOutputStream oos = new ObjectOutputStream(
                        new ByteArrayOutputStream()
                    )
                ) {
                    oos.writeObject(puz);
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
