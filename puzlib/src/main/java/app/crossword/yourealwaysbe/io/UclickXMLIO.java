package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Puzzle;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Converts a puzzle from the XML format used by uclick syndicated puzzles
 * to the Across Lite .puz format.  The format is:
 *
 * <crossword>
 *   <Title v="[Title]" />
 *   <Author v="[Author]" />
 *   <Width v="[Width]" />
 *   <Height v="[Height]" />
 *   <AllAnswer v="[Grid]" />
 *   <Copyright = "[Copyright]" />
 *   <across>
 *       <a[i] a="[Answer]" c="[Clue]" n="[GridIndex]" cn="[ClueNumber]" />
 *   </across>
 *   <down>
 *       <d[j] ... />
 *   </down>
 * </crossword>
 *
 * [Grid] contains all of the letters in the solution, reading left-to-right,
 * top-to-bottom, with - for black squares. [i] is an incrementing number for
 * each across clue, starting at 1. [GridIndex] is the offset into [Grid] at
 * which the clue starts.  [Clue] text is HTML escaped.
 */
public class UclickXMLIO implements PuzzleParser {
    private static String CHARSET_NAME = "utf8";

    private static class UclickXMLParser extends DefaultHandler {
        private Puzzle puz;
        private boolean inAcross = false;
        private boolean inDown = false;
        private int maxClueNum = -1;
        private int width = 0;
        private int height = 0;

        // for checking file type was decent
        boolean hasCrossword = false;
        boolean hasAcross = false;
        boolean hasDown = false;

        public UclickXMLParser(Puzzle puz) {
            this.puz = puz;
        }

        public boolean isSuccessfulRead() {
            return hasCrossword
                && hasAcross
                && hasDown
                && maxClueNum > -1
                && puz.getWidth() > 0
                && puz.getHeight() > 0
                && puz.getNumberOfClues() > 0;
        }

        @Override
        public void startElement(String nsURI, String strippedName,
                String tagName, Attributes attributes) throws SAXException {
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;
            //System.out.println("Start" + name);
            if (inAcross) {
                int clueNum = Integer.parseInt(attributes.getValue("cn"));
                if (clueNum > maxClueNum) {
                    maxClueNum = clueNum;
                }
                try {
                    puz.addClue(new Clue(
                        clueNum,
                        true,
                        URLDecoder.decode(
                            attributes.getValue("c"), CHARSET_NAME
                        )
                    ));
                } catch (UnsupportedEncodingException e) {
                    puz.addClue(
                        new Clue(clueNum, true, attributes.getValue("c"))
                    );
                }
            } else if (inDown) {
                int clueNum = Integer.parseInt(attributes.getValue("cn"));
                if (clueNum > maxClueNum) {
                    maxClueNum = clueNum;
                }
                try {
                    puz.addClue(new Clue(
                        clueNum,
                        false,
                        URLDecoder.decode(
                            attributes.getValue("c"), CHARSET_NAME
                        )
                    ));
                } catch (UnsupportedEncodingException e) {
                    puz.addClue(
                        new Clue(clueNum, false, attributes.getValue("c"))
                    );
                }
            } else if (name.equalsIgnoreCase("title")) {
                puz.setTitle(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("author")) {
                puz.setAuthor(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("copyright")) {
                puz.setCopyright(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("width")) {
                width = Integer.parseInt(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("height")) {
                height = Integer.parseInt(attributes.getValue("v"));
            } else if (name.equalsIgnoreCase("allanswer")) {
                String rawGrid = attributes.getValue("v");
                Box[] boxesList = new Box[height*width];
                for (int i = 0; i < rawGrid.length(); i++) {
                    char sol = rawGrid.charAt(i);
                    if (sol != '-') {
                        boxesList[i] = new Box();
                        boxesList[i].setSolution(sol);
                        boxesList[i].setBlank();
                    }
                }
                puz.setBoxesFromList(boxesList, width, height);
            } else if (name.equalsIgnoreCase("across")) {
                inAcross = true;
                hasAcross = true;
            } else if (name.equalsIgnoreCase("down")) {
                inDown = true;
                hasDown = true;
            } else if (name.equalsIgnoreCase("crossword")) {
                hasCrossword = true;
            }
        }

        @Override
        public void endElement(String nsURI, String strippedName,
                String tagName) throws SAXException {
            //System.out.println("EndElement " +nsURI+" : "+tagName);
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;
            //System.out.println("End : "+name);

            if (name.equalsIgnoreCase("across")) {
                inAcross = false;
            } else if (name.equalsIgnoreCase("down")) {
                inDown = false;
            }
        }
    }

    @Override
    public Puzzle parseInput(InputStream is) {
        return parsePuzzle(is);
    }

    public static Puzzle parsePuzzle(InputStream is) {
        Puzzle puz = new Puzzle();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            XMLReader xr = parser.getXMLReader();
            UclickXMLParser handler = new UclickXMLParser(puz);
            xr.setContentHandler(handler);
            xr.parse(new InputSource(is));

            if (!handler.isSuccessfulRead())
                return null;

            puz.setNotes("");

            return puz;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean convertUclickPuzzle(InputStream is, DataOutputStream os,
            String copyright, LocalDate d) {
        Puzzle puz = parsePuzzle(is);
        puz.setDate(d);
        if (copyright != null)
            puz.setCopyright(copyright);

        try {
            IO.saveNative(puz, os);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to save puzzle: " + e.getMessage());
            return false;
        }
    }
}
