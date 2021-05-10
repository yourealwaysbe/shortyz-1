
package app.crossword.yourealwaysbe.io;

import junit.framework.TestCase;

public class PuzzleStreamReaderTest extends TestCase {
    public void testAcrossLite() {
        IOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(() -> {
                return IOTest.getTestPuzzle1InputStream();
            })
        );
    }

    public void testJPZ() {
        JPZIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(() -> {
                return JPZIOTest.getTestPuzzle1InputStream();
            })
        );
    }

    public void testBrainsOnly() {
        BrainsOnlyIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(() -> {
                return BrainsOnlyIOTest.getTestPuzzle1InputStream();
            })
        );
    }

    public void testKingFeaturesPlaintext() {
        KingFeaturesPlaintextIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(() -> {
                return KingFeaturesPlaintextIOTest.getTestPuzzle1InputStream();
            })
        );
    }

    public void testUclick() {
        UclickXMLIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInput(() -> {
                return UclickXMLIOTest.getTestPuzzle1InputStream();
            })
        );
    }
}

