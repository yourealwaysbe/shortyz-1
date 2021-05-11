package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Puzzle;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.StringBuilder;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Converts a puzzle from the JPZ Crossword Compiler XML format.
 *
 * This is not necessarily a complete implementation, but works for the
 * sources tested.
 *
 * Converts to the Across Lite .puz format.
 *
 * The (supported) XML format is:
 *
 * <crossword-compiler>
 *   <rectangular-puzzle>
 *     <metadata>
 *       <title>[Title]</title>
 *       <creator>[Author]</creator>
 *       <copyright>[Copyright]</copyright>
 *       <description>[Description]</description>
 *     </metadata>
 *     <crossword>
 *       <grid width="[width]" height="[height]">
 *         <cell x="[x]" y="[y]" solution="[letter]" ?number="[number]"/>
 *         <cell x="[x]" y="[y]" type="block" .../>
 *         ...
 *       </grid>
 *       <clues ordering="normal">
 *         <title><b>Across [or] Down</b></title>
 *         <clue number="[number]" format="[length]" citation="[explanation]">
 *           [clue]
 *         </clue>
 *         <clue number="[number]" is-link="[ordering num]">
 *           [clue]
 *         </clue>
*       </clues>
 *     </crossword>
 *   </rectangular-puzzle>
 * </crossword-compiler>
 */
public class JPZIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger("app.crossword.yourealwaysbe");

    private static final String UNDEFINED_CLUE = "-";

    private static class JPZXMLParser extends DefaultHandler {
        private String title = "";
        private String creator = "";
        private String copyright = "";
        private String description = "";
        private int width;
        private int height;
        private Box[][] boxes;
        private Map<Integer, String> acrossNumToClueMap = new HashMap<>();
        private Map<Integer, String> downNumToClueMap = new HashMap<>();
        private Map<Integer, String> acrossNumToCitationMap = new HashMap<>();
        private Map<Integer, String> downNumToCitationMap = new HashMap<>();
        private int maxClueNum = -1;
        private StringBuilder charBuffer = new StringBuilder();

        // sanity checks
        private boolean hasRectangularPuzzleEle = false;
        private boolean hasGridEle = false;
        private boolean hasCluesEle = false;

        public String getTitle() { return title; }
        public String getCreator() { return creator; }
        public String getCopyright() { return copyright; }
        public String getDescription() { return description; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public Box[][] getBoxes() { return boxes; }
        public Map<Integer, String> getAcrossNumToClueMap() {
            return acrossNumToClueMap;
        }
        public Map<Integer, String> getDownNumToClueMap() {
            return downNumToClueMap;
        }
        public Map<Integer, String> getAcrossNumToCitationMap() {
            return acrossNumToCitationMap;
        }
        public Map<Integer, String> getDownNumToCitationMap() {
            return downNumToCitationMap;
        }
        public int getMaxClueNum() { return maxClueNum; }

        /**
         * Best assessment of whether read succeeded (i.e. was a JPZ
         * file)
         */
        public boolean isSuccessfulRead() {
            return hasRectangularPuzzleEle
                && hasGridEle
                && hasCluesEle
                && getWidth() > 0
                && getHeight() > 0
                && getMaxClueNum() > 0
                && (getAcrossNumToClueMap().size() > 0
                        || getDownNumToClueMap().size() > 0);
        }

        // Use several handlers to maintain three different modes:
        // outerXML, inGrid, and inClues

        private DefaultHandler outerXML = new DefaultHandler() {
            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("title")
                        || name.equalsIgnoreCase("creator")
                        || name.equalsIgnoreCase("copyright")
                        || name.equalsIgnoreCase("description")) {
                    charBuffer.delete(0, charBuffer.length());
                }
            }

            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                charBuffer.append(ch, start, length);
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                String charData = charBuffer.toString().trim();

                if (name.equalsIgnoreCase("title")) {
                    title = charData;
                } else if (name.equalsIgnoreCase("creator")) {
                    creator = charData;
                } else if (name.equalsIgnoreCase("copyright")) {
                    copyright = charData;
                } else if (name.equalsIgnoreCase("description")) {
                    description = charData;
                }
            }
        };

        private DefaultHandler inGrid = new DefaultHandler() {
            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0
                    ? tagName.trim() : strippedName;

                try {
                    if (name.equalsIgnoreCase("grid")) {
                        JPZXMLParser.this.width
                            = Integer.parseInt(attributes.getValue("width"));
                        JPZXMLParser.this.height
                            = Integer.parseInt(attributes.getValue("height"));
                        JPZXMLParser.this.boxes = new Box[height][width];
                    } else if (name.equalsIgnoreCase("cell")) {
                        int x = Integer.parseInt(attributes.getValue("x")) - 1;
                        int y = Integer.parseInt(attributes.getValue("y")) - 1;
                        String solution = attributes.getValue("solution");
                        String number = attributes.getValue("number");
                        if (solution != null &&
                            0 <= x && x < JPZXMLParser.this.getWidth() &&
                            0 <= y && y < JPZXMLParser.this.getHeight()) {
                            Box box = new Box();

                            if (solution.length() > 0)
                                box.setSolution(solution.charAt(0));
                            box.setBlank();

                            if (number != null) {
                                int clueNumber = Integer.parseInt(number);
                                box.setClueNumber(clueNumber);
                                maxClueNum = Math.max(maxClueNum, clueNumber);
                            }

                            JPZXMLParser.this.boxes[y][x] = box;
                        }
                    }
                } catch (NumberFormatException e) {
                    LOG.severe("Could not read Independent XML cell data: " + e);
                }
            }
        };

        private DefaultHandler inClues = new DefaultHandler() {
            private int inClueNum = -1;
            private String inClueFormat = "";
            private String inComplexClueFormat = "";

            private StringBuilder charBuffer = new StringBuilder();

            private Map<Integer, String> curClueMap = null;
            private Map<Integer, String> curCitationMap = null;

            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

                try {
                    if (name.equalsIgnoreCase("title")) {
                        charBuffer.delete(0, charBuffer.length());
                    } else if (name.equalsIgnoreCase("clue") && knowDirection()) {
                        charBuffer.delete(0, charBuffer.length());

                        String numAttr = attributes.getValue("number");
                        inClueNum = extractClueNumber(numAttr);
                        maxClueNum = Math.max(inClueNum, maxClueNum);

                        String link = attributes.getValue("is-link");
                        if (link == null) {
                            inClueFormat = attributes.getValue("format");
                            if (inClueFormat == null)
                                inClueFormat = "";

                            if (isComplexClueNumber(numAttr))
                                inComplexClueFormat = numAttr;

                            String citation = attributes.getValue("citation");
                            if (citation != null)
                                curCitationMap.put(inClueNum, citation);

                            // clue appears in characters between start
                            // and end
                        }
                    }
                } catch (NumberFormatException e) {
                    LOG.severe("Could not read Independent XML cell data: " + e);
                }
            }

            @Override
            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                charBuffer.append(ch, start, length);
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("title")) {
                    String title = charBuffer.toString().toUpperCase();
                    if (title.contains("ACROSS")) {
                        curClueMap = JPZXMLParser.this.acrossNumToClueMap;
                        curCitationMap = JPZXMLParser.this.acrossNumToCitationMap;
                    } else if (title.contains("DOWN")) {
                        curClueMap = JPZXMLParser.this.downNumToClueMap;
                        curCitationMap = JPZXMLParser.this.downNumToCitationMap;
                    }
                } else if (name.equalsIgnoreCase("clue") && knowDirection()) {
                    String fullClue = charBuffer.toString();

                    if (inComplexClueFormat.length() > 0) {
                        fullClue = String.format("%s (Clues %s)",
                                                 fullClue,
                                                 inComplexClueFormat);
                    }

                    if (inClueFormat.length() > 0) {
                        fullClue = String.format(
                            "%s (%s)", fullClue, inClueFormat
                        );
                    }

                    curClueMap.put(inClueNum, fullClue);

                    inClueNum = -1;
                    inClueFormat = "";
                    inComplexClueFormat = "";
                }
            }

            /**
             * Detect if clue spans several words
             */
            private boolean isComplexClueNumber(String numberString)
                    throws NumberFormatException {
                if (numberString == null)
                    throw new NumberFormatException("Null number in clue");

                return numberString.split("[^0-9]").length > 1;
            }

            /**
             * Get primary clue number from a potentially complex number
             */
            private int extractClueNumber(String numberString)
                    throws NumberFormatException {
                if (numberString == null)
                    throw new NumberFormatException("Null number in clue");
                // some clues are spread across the board
                String[] nums = numberString.split("[^0-9]");
                if (nums.length == 0)
                    throw new NumberFormatException("No numbers given with clue " +
                                                    numberString);

                return Integer.parseInt(nums[0]);
            }

            /**
             * True if we've figured out whether we're across or down
             */
            private boolean knowDirection() {
                return curClueMap != null && curCitationMap != null;
            }
        };

        private DefaultHandler state = outerXML;

        @Override
        public void startElement(String nsURI,
                                 String strippedName,
                                 String tagName,
                                 Attributes attributes) throws SAXException {
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

            if (name.equalsIgnoreCase("rectangular-puzzle")) {
                hasRectangularPuzzleEle = true;
            } else if (name.equalsIgnoreCase("grid")) {
                hasGridEle = true;
                state = inGrid;
            } else if (name.equalsIgnoreCase("clues")) {
                hasCluesEle = true;
                state = inClues;
            }

            state.startElement(nsURI, name, tagName, attributes);
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            state.characters(ch, start, length);
        }

        @Override
        public void endElement(String nsURI,
                               String strippedName,
                               String tagName) throws SAXException {
            strippedName = strippedName.trim();
            String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

            state.endElement(nsURI, strippedName, tagName);

            if (name.equalsIgnoreCase("grid")) {
                state = outerXML;
            } else if (name.equalsIgnoreCase("clues")) {
                state = outerXML;
            } else if (name.equalsIgnoreCase("crossword")) {
                fillInMissingClues();
            }
        }

        /**
         * Populate clue maps with "undefined" strings
         *
         * Sometimes the Indy format omits the is-link clues
         */
        private void fillInMissingClues() {
            for (int y = 0; y < boxes.length; y++) {
                for (int x = 0; x < boxes[y].length; x++) {
                    if (boxes[y][x] != null) {
                        int clue = boxes[y][x].getClueNumber();
                        if (clue > 0) {
                            boolean boxLeft = x > 0 && boxes[y][x-1] != null;
                            boolean boxRight = x < boxes[y].length - 1
                                && boxes[y][x+1] != null;

                            boolean boxUp = y > 0 && boxes[y-1][x] != null;
                            boolean boxDown = y < boxes.length - 1
                                && boxes[y+1][x] != null;

                            boolean hasAcross
                                = acrossNumToClueMap.containsKey(clue);
                            boolean hasDown
                                = downNumToClueMap.containsKey(clue);

                           if (!boxLeft && boxRight && !hasAcross)
                                acrossNumToClueMap.put(clue, UNDEFINED_CLUE);
                           if (!boxUp && boxDown && !hasDown)
                                downNumToClueMap.put(clue, UNDEFINED_CLUE);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws Exception {
        Puzzle puz = new Puzzle();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLReader xr = parser.getXMLReader();
        JPZXMLParser handler = new JPZXMLParser();
        xr.setContentHandler(handler);
        xr.parse(new InputSource(unzipOrPassthrough(is)));

        if (!handler.isSuccessfulRead())
            return null;

        puz.setTitle(handler.getTitle());
        puz.setAuthor(handler.getCreator());
        puz.setCopyright(handler.getCopyright());
        puz.setBoxes(handler.getBoxes());

        setClues(puz, handler);
        setNote(puz, handler);

        return puz;
    }

    public static boolean convertPuzzle(InputStream is,
                                        DataOutputStream os,
                                        LocalDate d) {
        try {
            Puzzle puz = readPuzzle(is);
            puz.setDate(d);
            IO.saveNative(puz, os);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.severe("Unable to convert JPZ file: " + e.getMessage());
            return false;
        }
    }

    private static void setClues(Puzzle puz, JPZXMLParser handler) {
        Map<Integer, String> acrossNumToClueMap
            = handler.getAcrossNumToClueMap();

        for (Map.Entry<Integer, String> entry : acrossNumToClueMap.entrySet()) {
            puz.addClue(new Clue(entry.getKey(), true, entry.getValue()));
        }

        Map<Integer, String> downNumToClueMap = handler.getDownNumToClueMap();

        for (Map.Entry<Integer, String> entry : downNumToClueMap.entrySet()) {
            puz.addClue(new Clue(entry.getKey(), false, entry.getValue()));
        }
    }

    private static void setNote(Puzzle puz, JPZXMLParser handler) {
        Map<Integer, String> acrossNumToCitationMap
            = handler.getAcrossNumToCitationMap();
        Map<Integer, String> downNumToCitationMap
            = handler.getDownNumToCitationMap();
        int maxClueNum = handler.getMaxClueNum();

        StringBuilder notes = new StringBuilder();

        String description = handler.getDescription();
        if (description != null)
            notes.append(description);

        if (acrossNumToCitationMap.size() > 0) {
            if (notes.length() > 0)
                notes.append("\n\n");

            notes.append("Across:\n\n");

            for(int clueNum = 1; clueNum <= maxClueNum; clueNum++) {
                String citation = acrossNumToCitationMap.get(clueNum);
                if (citation != null)
                    notes.append(String.format("%d: %s\n", clueNum, citation));
            }
        }

        if (downNumToCitationMap.size() > 0) {
            if (notes.length() > 0)
                notes.append("\n\n");

            notes.append("\nDown:\n\n");

            for(int clueNum = 1; clueNum <= maxClueNum; clueNum++) {
                String citation = downNumToCitationMap.get(clueNum);
                if (citation != null)
                    notes.append(String.format("%d: %s\n", clueNum, citation));
            }
        }

        puz.setNotes(notes.toString());
    }

    /**
     * Returns a new input stream that either passes through is or
     * unzips.
     *
     * Closing return stream has no effect (it's a byte array stream)
     */
    private static ByteArrayInputStream unzipOrPassthrough(InputStream is)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        IO.copyStream(is, baos);

        try (
            ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(baos.toByteArray())
            )
        ) {
            ZipEntry entry = zis.getNextEntry();
            while (entry.isDirectory()) {
                entry = zis.getNextEntry();
            }
            baos = new ByteArrayOutputStream();
            IO.copyStream(zis, baos);
        } catch (Exception e) {
            // not zipped, carry on
        }

        // replace &nbsp; with space
        // and copyright symbol with (c) (else encoding error on
        // android)
        try (
            Scanner in = new Scanner(
                new ByteArrayInputStream(baos.toByteArray())
            );
            ByteArrayOutputStream replaced = new ByteArrayOutputStream();
            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(replaced)
            );
        ) {
            while (in.hasNextLine()) {
                String line = in.nextLine();
                line = line.replaceAll("&nbsp;", " ");
                line = line.replaceAll("©", "(c)");
                out.write(line + "\n");
            }
            out.flush();
            return new ByteArrayInputStream(replaced.toByteArray());
        }
    }
}
