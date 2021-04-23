package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.lang.StringBuilder;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Converts a puzzle from the Crossword Compiler XML format.
 *
 * This is not necessarily a complete implementation, but works for the
 * sources tests.
 *
 * Converts to the Across Lite .puz format.
 *
 * The (supported) XML format is:
 *
 * <crossword-compiler>
 *   <rectangular-puzzle>
 *     <metadata>
 *       <title>[Title]</title>
 *       ...
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
public class CrosswordCompilerXMLIO {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private static final String UNDEFINED_CLUE = "-";

    private static class IndependentXMLParser extends DefaultHandler {
        private String title;
        private int width;
        private int height;
        private Box[][] boxes;
        private Map<Integer, String> acrossNumToClueMap = new HashMap<>();
        private Map<Integer, String> downNumToClueMap = new HashMap<>();
        private Map<Integer, String> acrossNumToCitationMap = new HashMap<>();
        private Map<Integer, String> downNumToCitationMap = new HashMap<>();
        private int maxClueNum = -1;

        public String getTitle() { return title; }
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

        // Use several handlers to maintain three different modes:
        // outerXML, inGrid, and inClues

        private DefaultHandler outerXML = new DefaultHandler() {
            private boolean inTitle = false;

            @Override
            public void startElement(String nsURI,
                                     String strippedName,
                                     String tagName,
                                     Attributes attributes) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("title")) {
                    inTitle = true;
                }
            }

            public void characters(char[] ch, int start, int length) throws SAXException {
                if (inTitle) {
                    IndependentXMLParser.this.title =
                        new String(ch, start, length);
                }
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("title")) {
                    inTitle = false;
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
                String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

                try {
                    if (name.equalsIgnoreCase("grid")) {
                        IndependentXMLParser.this.width
                            = Integer.parseInt(attributes.getValue("width"));
                        IndependentXMLParser.this.height
                            = Integer.parseInt(attributes.getValue("height"));
                        IndependentXMLParser.this.boxes = new Box[height][width];
                    } else if (name.equalsIgnoreCase("cell")) {
                        int x = Integer.parseInt(attributes.getValue("x")) - 1;
                        int y = Integer.parseInt(attributes.getValue("y")) - 1;
                        String solution = attributes.getValue("solution");
                        String number = attributes.getValue("number");
                        if (solution != null &&
                            0 <= x && x < IndependentXMLParser.this.getWidth() &&
                            0 <= y && y < IndependentXMLParser.this.getHeight()) {
                            Box box = new Box();

                            if (solution.length() > 0)
                                box.setSolution(solution.charAt(0));
                            box.setBlank();

                            if (number != null) {
                                int clueNumber = Integer.parseInt(number);
                                box.setClueNumber(clueNumber);
                                maxClueNum = Math.max(maxClueNum, clueNumber);
                            }

                            IndependentXMLParser.this.boxes[y][x] = box;
                        }
                    }
                } catch (NumberFormatException e) {
                    LOG.severe("Could not read Independent XML cell data: " + e);
                }
            }
        };

        private DefaultHandler inClues = new DefaultHandler() {
            private boolean inBold = false;
            private int inClueNum = -1;
            private String inClueFormat = "";
            private String inComplexClueFormat = "";

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
                    if (name.equalsIgnoreCase("b")) {
                        inBold = true;
                    } else if (name.equalsIgnoreCase("clue") && knowDirection()) {
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
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (inBold) {
                    String title = new String(ch, start, length);
                    if ("Across".equalsIgnoreCase(title)) {
                        curClueMap = IndependentXMLParser.this.acrossNumToClueMap;
                        curCitationMap = IndependentXMLParser.this.acrossNumToCitationMap;
                    } else if ("Down".equalsIgnoreCase(title)) {
                        curClueMap = IndependentXMLParser.this.downNumToClueMap;
                        curCitationMap = IndependentXMLParser.this.downNumToCitationMap;
                    }
                } else if (inClueNum >= 0 && knowDirection()) {
                    // some clues have html formatting in them so we
                    // have to combine multiple tags
                    String existing = curClueMap.get(inClueNum);
                    if (existing == null)
                        existing = "";

                    String text = new String(ch, start, length);

                    String fullClue = existing + text;
                    curClueMap.put(inClueNum, fullClue);
                }
            }

            @Override
            public void endElement(String nsURI,
                                   String strippedName,
                                   String tagName) throws SAXException {
                strippedName = strippedName.trim();
                String name = strippedName.length() == 0 ? tagName.trim() : strippedName;

                if (name.equalsIgnoreCase("b")) {
                    inBold = false;
                } else if (name.equalsIgnoreCase("clue") && knowDirection()) {
                    // finalise clue with formatting info
                    String clue = curClueMap.get(inClueNum);

                    String fullClue = (clue == null) ? "" : clue;
                    if (inComplexClueFormat.length() > 0)
                        fullClue = String.format("%s (Clues %s)",
                                                 fullClue,
                                                 inComplexClueFormat);
                    if (inClueFormat.length() > 0)
                        fullClue = String.format("%s (%s)", fullClue, inClueFormat);

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

            if (name.equalsIgnoreCase("grid")) {
                state = inGrid;
            } else if (name.equalsIgnoreCase("clues")) {
                state = inClues;
            }

            state.startElement(nsURI, name, tagName, attributes);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
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

    public static boolean convertPuzzle(InputStream is,
                                        DataOutputStream os,
                                        String copyright,
                                        LocalDate d) {
        Puzzle puz = new Puzzle();
        puz.setDate(d);
        puz.setCopyright(copyright);
        puz.setAuthor("The Independent");
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            XMLReader xr = parser.getXMLReader();
            IndependentXMLParser handler = new IndependentXMLParser();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(is));

            puz.setVersion(IO.VERSION_STRING);
            puz.setTitle(handler.getTitle());
            puz.setWidth(handler.getWidth());
            puz.setHeight(handler.getHeight());
            puz.setBoxes(handler.getBoxes());

            setClues(puz, handler);
            setNote(puz, handler);

            IO.saveNative(puz, os);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to parse XML file: " + e.getMessage());
            return false;
        }
    }

    private static void setClues(Puzzle puz, IndependentXMLParser handler) {
        Map<Integer, String> acrossNumToClueMap = handler.getAcrossNumToClueMap();
        Map<Integer, String> downNumToClueMap = handler.getDownNumToClueMap();
        int maxClueNum = handler.getMaxClueNum();

        int numberOfClues = acrossNumToClueMap.size() + downNumToClueMap.size();
        puz.setNumberOfClues(numberOfClues);

        String[] rawClues = new String[numberOfClues];
        int i = 0;
        for(int clueNum = 1; clueNum <= maxClueNum; clueNum++) {
            if (acrossNumToClueMap.containsKey(clueNum)) {
                rawClues[i] = acrossNumToClueMap.get(clueNum);
                i++;
            }
            if (downNumToClueMap.containsKey(clueNum)) {
                rawClues[i] = downNumToClueMap.get(clueNum);
                i++;
            }
        }

        puz.setRawClues(rawClues);
    }

    private static void setNote(Puzzle puz, IndependentXMLParser handler) {
        Map<Integer, String> acrossNumToCitationMap = handler.getAcrossNumToCitationMap();
        Map<Integer, String> downNumToCitationMap = handler.getDownNumToCitationMap();
        int maxClueNum = handler.getMaxClueNum();

        StringBuilder notes = new StringBuilder();

        notes.append("Across:\n\n");

        for(int clueNum = 1; clueNum <= maxClueNum; clueNum++) {
            String citation = acrossNumToCitationMap.get(clueNum);
            if (citation != null)
                notes.append(String.format("%d: %s\n", clueNum, citation));
        }

        notes.append("\nDown:\n\n");

        for(int clueNum = 1; clueNum <= maxClueNum; clueNum++) {
            String citation = downNumToCitationMap.get(clueNum);
            if (citation != null)
                notes.append(String.format("%d: %s\n", clueNum, citation));
        }

        puz.setNotes(notes.toString());
    }
}
