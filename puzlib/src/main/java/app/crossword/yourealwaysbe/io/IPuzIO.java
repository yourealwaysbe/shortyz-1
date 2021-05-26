
package app.crossword.yourealwaysbe.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Position;
import app.crossword.yourealwaysbe.puz.Puzzle.ClueNumDir;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Read IPuz from a stream.
 *
 * Throws an exception if the puzzle is not in IPuz format, or is in a format
 * not supported.
 *
 * Many fields are ignored. A rough estimate has been made as to whether
 * a puzzle is playable if that field is ignored. If it may be, then the
 * puzzle will load without it. If it's probably a problem, it will
 * raise an IPuzFormatException.
 *
 * Currently checksums of solutions are not supported. The puzzle will
 * still be playable. The Puzzle class has a checksum field that is not
 * used, and corresponds to Across Lite checksums.
 *
 * http://www.ipuz.org/
 */
public class IPuzIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(IPuzIO.class.getCanonicalName());

    private static final Charset WRITE_CHARSET = Charset.forName("UTF-8");

    private static final String FIELD_VERSION = "version";
    private static final String FIELD_KIND = "kind";

    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_COPYRIGHT = "copyright";
    private static final String FIELD_DATE = "date";
    private static final String FIELD_INTRO = "intro";
    private static final String FIELD_NOTES = "notes";
    private static final String FIELD_PUBLISHER = "publisher";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_URL = "url";

    private static final String FIELD_DIMENSIONS = "dimensions";
    private static final String FIELD_WIDTH = "width";
    private static final String FIELD_HEIGHT = "height";

    private static final String FIELD_PUZZLE = "puzzle";
    private static final String FIELD_SAVED = "saved";
    private static final String FIELD_SOLUTION = "solution";
    private static final String FIELD_SHOW_ENUMERATIONS = "showenumerations";
    private static final String FIELD_CLUES = "clues";

    private static final String FIELD_CELL = "cell";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_STYLE = "style";
    private static final String FIELD_SHAPE_BG = "shapebg";

    private static final String SHAPE_BG_CIRCLE = "circle";

    private static final String FIELD_CLUES_ACROSS = "Across";
    private static final String FIELD_CLUES_DOWN = "Down";

    private static final String FIELD_CLUE_NUMBER = "number";
    private static final String FIELD_CLUE_NUMBERS = "numbers";
    private static final String FIELD_CLUE_HINT = "clue";
    private static final String FIELD_CLUE_CONTINUED = "continued";
    private static final String FIELD_CLUE_REFERENCES = "references";
    private static final String FIELD_CLUE_DIRECTION = "direction";

    private static final String FIELD_ENUMERATION = "enumeration";

    private static final String FIELD_ORIGIN = "origin";
    private static final String FIELD_BLOCK = "block";
    private static final String FIELD_EMPTY = "empty";

    private static final String DEFAULT_BLOCK = "#";
    private static final String DEFAULT_EMPTY = "0";

    private static final String WRITE_VERSION = "http://ipuz.org/v2";
    private static final String WRITE_KIND = "http://ipuz.org/crossword#1";

    private static final String[] SUPPORTED_VERSIONS = {
        "http://ipuz.org/v1",
        WRITE_VERSION
    };
    private static final String[] SUPPORTED_KINDS = {
        WRITE_KIND,
        "http://ipuz.org/crossword/crypticcrossword#1"
    };

    // Fields considered important enough for it to be impossible to
    // load the puzzle without them
    private static final String[] FIELD_CLUES_UNSUPPORTED = {
        "Diagonal",
        "Diagonal Up",
        "Diagonal Down Left",
        "Diagonal Up Left",
        "Zones",
        "Clues"
    };

    private static final String EXT_NAMESPACE = "app.crossword.yourealwaysbe";

    private static final String FIELD_EXT_SUPPORT_URL
        = getQualifiedExtensionName("supporturl");
    private static final String FIELD_EXT_PLAY_DATA
        = getQualifiedExtensionName("playdata");

    private static final String FIELD_VOLATILE = "volatile";
    private static final String FIELD_IS_VOLATILE = "*";
    private static final String FIELD_IS_NOT_VOLATILE = "";

    private static final String[] VOLATILE_EXTENSIONS = {
        FIELD_EXT_PLAY_DATA
    };

    private static final String[] NON_VOLATILE_EXTENSIONS = {
        FIELD_EXT_SUPPORT_URL
    };

    private static final String FIELD_BOX_EXTRAS = "boxextras";
    private static final String FIELD_BOX_CHEATED = "cheated";
    private static final String FIELD_BOX_RESPONDER = "responder";
    private static final String FIELD_COMPLETION_TIME = "completiontime";
    private static final String FIELD_PCNT_COMPLETE = "percentcomplete";
    private static final String FIELD_PCNT_FILLED = "percentfilled";
    private static final String FIELD_UPDATABLE = "updatable";
    private static final String FIELD_POSITION = "position";
    private static final String FIELD_POSITION_ROW = "row";
    private static final String FIELD_POSITION_COL = "col";
    private static final String FIELD_POSITION_ACROSS = "across";
    private static final String FIELD_CLUE_HISTORY = "cluehistory";
    private static final String FIELD_CLUE_ACROSS = "across";
    private static final String FIELD_CLUE_NOTES = "cluenotes";
    private static final String FIELD_CLUE_NOTE_CLUE = "clue";
    private static final String FIELD_CLUE_NOTE_SCRATCH = "scratch";
    private static final String FIELD_CLUE_NOTE_TEXT = "text";
    private static final String FIELD_CLUE_NOTE_ANAGRAM_SRC = "anagramsource";
    private static final String FIELD_CLUE_NOTE_ANAGRAM_SOL
        = "anagramsolution";

    private static final Pattern INT_STRING_RE = Pattern.compile("\\d+");
    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);

    private static final String NULL_CLUE = "-";
    // IPuz tags not to strip from HTML (preserve line breaks)
    private static final Whitelist JSOUP_CLEAN_WHITELIST = new Whitelist();
    static {
        JSOUP_CLEAN_WHITELIST.addTags("br");
    }

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class IPuzFormatException extends Exception {
        public IPuzFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        VisibleByteArrayOutputStream baos = new VisibleByteArrayOutputStream();
        IO.copyStream(is, baos);

        String charset = getCharsetName(baos);

        if (charset == null)
            return null;

        JSONObject json = new JSONObject(baos.toString(charset));

        try {
            checkIPuzVersion(json);
            checkIPuzKind(json);

            Puzzle puz = new Puzzle();

            readMetaData(json, puz);
            readBoxes(json, puz);
            readClues(json, puz);
            readExtensions(json, puz);

            return puz;
        } catch (IPuzFormatException | JSONException e) {
            LOG.severe("Could not read IPuz file: " + e);
            return null;
        }
    }

    private static void checkIPuzVersion(JSONObject puzJson)
            throws IPuzFormatException {
        String version = puzJson.getString(FIELD_VERSION);
        for (String supportedVersion : SUPPORTED_VERSIONS) {
            if (supportedVersion.equalsIgnoreCase(version))
                return;
        }
        throw new IPuzFormatException(
            "Unsupported IPuz version: " + version
        );
    }

    private static void checkIPuzKind(JSONObject puzJson)
            throws IPuzFormatException {
        JSONArray kinds = puzJson.getJSONArray(FIELD_KIND);

        for (int i = 0; i < kinds.length(); i++) {
            String kind = kinds.getString(i);
            for (String supportedKind : SUPPORTED_KINDS) {
                if (supportedKind.equalsIgnoreCase(kind))
                    return;
            }
        }

        throw new IPuzFormatException("No supported IPuz kind: " + kinds);
    }

    /**
     * Read puzzle info from puzJson into puz
     *
     * Meta-data stuff, like title, copyright, etc.
     */
    private static void readMetaData(JSONObject puzJson, Puzzle puz) {
        puz.setTitle(getHtmlOptString(puzJson, FIELD_TITLE));
        puz.setAuthor(getHtmlOptString(puzJson, FIELD_AUTHOR));
        puz.setCopyright(puzJson.optString(FIELD_COPYRIGHT));

        String intro = getHtmlOptString(puzJson, FIELD_INTRO);
        String notes = getHtmlOptString(puzJson, FIELD_NOTES);

        StringBuilder fullNotes = new StringBuilder();

        if (intro != null && intro.length() > 0)
            fullNotes.append(intro);

        if (notes != null && notes.length() > 0) {
            if (fullNotes.length() > 0)
                fullNotes.append("\n\n");
            fullNotes.append(notes);
        }

        puz.setNotes(fullNotes.toString());

        puz.setSourceUrl(puzJson.optString(FIELD_URL));
        puz.setSource(getHtmlOptString(puzJson, FIELD_PUBLISHER));

        String date = puzJson.optString(FIELD_DATE);
        if (date != null && date.length() > 0)
            puz.setDate(LocalDate.parse(date, DATE_FORMATTER));
    }

    /**
     * Get optional field from JSON
     *
     * Strips any HTML elements from it.
     */
    private static String getHtmlOptString(JSONObject json, String field) {
        String value = json.optString(field);

        if (value == null || value.length() == 0)
            return null;

        return unHtmlString(value);
    }

    /**
     * Remove IPuz HTML from a string
     */
    private static String unHtmlString(String value) {
        if (value == null)
            return null;

        // this is a bit hacky: any break tag is normalised to "\r?\n<br>"
        // by the clean method, we remove the \r\ns and turn <br> into \n
        return StringEscapeUtils.unescapeHtml4(
            Jsoup.clean(value, JSOUP_CLEAN_WHITELIST)
                .replace("\r", "")
                .replace("\n", "")
                .replace("<br>", "\n")
        );
    }

    /**
     * Return IPuz HTML encoding of string
     */
    private static String htmlString(String value) {
        return StringEscapeUtils.escapeHtml4(value)
            .replace("\r", "")
            .replace("\n", "<br/>");
    }

    /**
     * Read fully populated Box objects from JSON into puz
     */
    private static void readBoxes(JSONObject puzJson, Puzzle puz)
            throws IPuzFormatException {
        JSONObject dimensions = puzJson.getJSONObject(FIELD_DIMENSIONS);

        int width = dimensions.getInt(FIELD_WIDTH);
        int height = dimensions.getInt(FIELD_HEIGHT);

        Box[][] boxes = new Box[height][width];

        readPuzzleCells(puzJson, boxes);
        readSaved(puzJson, boxes);
        readSolution(puzJson, boxes);

        try {
            puz.setBoxes(boxes);
        } catch (IllegalArgumentException e) {
            throw new IPuzFormatException(
                "Boxes not compatible: " + e.getMessage()
            );
        }
    }

    /**
     * Populate boxes array following cells in JSON
     *
     * I.e. no box, block, empty, or clue number, possibly with styles
     * etc.
     */
    private static void readPuzzleCells(JSONObject puzJson, Box[][] boxes)
            throws IPuzFormatException {
        JSONArray cells = puzJson.getJSONArray(FIELD_PUZZLE);

        if (cells.length() < boxes.length) {
            throw new IPuzFormatException(
                "Number of cell rows doesn't match boxes dimensions"
            );
        }

        String block = getBlockString(puzJson);
        String empty = getEmptyCellString(puzJson);

        for (int row = 0; row < boxes.length; row++) {
            JSONArray rowCells = cells.getJSONArray(row);

            if (rowCells.length() < boxes[row].length) {
                throw new IPuzFormatException(
                    "Number of cell columns doesn't match boxes dimension"
                );
            }

            for (int col = 0; col < boxes[row].length; col++) {
                boxes[row][col]
                    = getBoxFromObj(rowCells.get(col), block, empty);
            }
        }
    }

    private static String getBlockString(JSONObject puzJson) {
        return puzJson.optString(FIELD_BLOCK, DEFAULT_BLOCK);
    }

    private static String getEmptyCellString(JSONObject puzJson) {
        return puzJson.optString(FIELD_EMPTY, DEFAULT_EMPTY);
    }

    /**
     * Turn the (JSON) object into a box
     *
     * If null or block value, then blank. If empty then empty box, else
     * box with clue number and maybe decoration.
     *
     * @param cell the object in the JSON for the cell (could be a
     * number, string, or JSON object
     * @param block the string for a block
     * @param empty the string for an empty cell
     */
    private static Box getBoxFromObj(Object cell, String block, String empty)
            throws IPuzFormatException {
        if (cell == null || JSONObject.NULL.equals(cell)) {
            return null;
        } else if (cell instanceof JSONObject) {
            JSONObject json = (JSONObject) cell;

            // unsure if ipuz allows cell field to be missing, but
            // reasonable to assume empty cell if so
            Object cellObj = json.opt(FIELD_CELL);
            if (cellObj == null)
                cellObj = empty;

            Box box = getBoxFromObj(cellObj, block, empty);

            String initVal = json.optString(FIELD_VALUE);
            if (initVal != null && initVal.length() > 0) {
                if (initVal.length() != 1) {
                    throw new IPuzFormatException(
                        "Cannot represent values of more than one character: "
                            + initVal
                    );
                }
                box.setResponse(initVal.charAt(0));
            }

            JSONObject style = json.optJSONObject(FIELD_STYLE);
            if (SHAPE_BG_CIRCLE.equals(style.optString(FIELD_SHAPE_BG))) {
                box.setCircled(true);
            }

            return box;
        } else if (cell.equals(block.toString())) {
            return null;
        } else if (cell.equals(empty.toString())) {
            return new Box();
        } else {
            try {
                Box box = new Box();
                box.setClueNumber(Integer.valueOf(cell.toString()));
                return box;
            } catch (NumberFormatException e) {
                throw new IPuzFormatException(
                    "Unrecognised cell in puzzle: " + cell
                );
            }
        }
    }

    /**
     * Populate boxes array with response data from JSON
     */
    private static void readSaved(JSONObject puzJson, Box[][] boxes)
            throws IPuzFormatException {
        String block = getBlockString(puzJson);
        String empty = getEmptyCellString(puzJson);
        JSONArray saved = puzJson.optJSONArray(FIELD_SAVED);
        if (saved != null && saved.length() > 0)
            readValues(saved, boxes, false, block, empty);
    }

    /**
     * Populate boxes array with puzzle solution data from JSON
     */
    private static void readSolution(JSONObject puzJson, Box[][] boxes)
            throws IPuzFormatException {
        String block = getBlockString(puzJson);
        String empty = getEmptyCellString(puzJson);
        JSONArray solution = puzJson.optJSONArray(FIELD_SOLUTION);
        if (solution != null && solution.length() > 0)
            readValues(solution, boxes, true, block, empty);
    }

    /**
     * Reads a value array and loads into boxes saved/solution
     *
     * @param cells the array of arrays of CrosswordValues
     * @param boxes the boxes to read data into
     * @param isSolution whether puzzles solution is being read, else
     * saved user responses will be read
     */
    private static void readValues(
        JSONArray cells, Box[][] boxes, boolean isSolution,
        String block, String empty
    ) throws IPuzFormatException {
        int height = Math.min(cells.length(), boxes.length);

        for (int row = 0; row < height; row++) {
            JSONArray rowCells = cells.getJSONArray(row);

            int width = Math.min(rowCells.length(), boxes[row].length);

            for (int col = 0; col < width; col++) {
                Character value = getCrosswordValueFromObj(
                    rowCells.get(col), block, empty
                );

                if (value !=  null) {
                    if (isSolution)
                        boxes[row][col].setSolution(value);
                    else
                        boxes[row][col].setResponse(value);
                }
            }
        }
    }

    /**
     * Fill in the saved data for the box from the object in the JSON
     *
     * @param cell the object in the JSON saved array
     * @param block the representation for a block
     * @param empty the representation of an empty cell
     * @return value of response if given, Box.BLANK if empty, null if
     * block or omitted
     */
    private static Character getCrosswordValueFromObj(
        Object cell, String block, String empty
    ) throws IPuzFormatException {
        if (cell == null || JSONObject.NULL.equals(cell)) {
            return null;
        } else if (cell instanceof JSONArray) {
            JSONArray values = (JSONArray) cell;
            if (values.length() != 1) {
                throw new IPuzFormatException(
                    "Multiple cell values not supported: " + values
                );
            }
            return getCrosswordValueFromObj(values.get(0), block, empty);
        } else if (cell instanceof JSONObject) {
            JSONObject json = (JSONObject) cell;
            String value = json.optString(FIELD_VALUE);
            if (value == null || value.length() == 0) {
                return null;
            } else {
                if (value.length() != 1) {
                    throw new IPuzFormatException(
                        "Cannot represent values of more than one character: "
                            + value
                    );
                }
                return value.charAt(0);
            }
        } else if (block.equals(cell.toString())) {
            return null;
        } else if (empty.equals(cell.toString())) {
            return Box.BLANK;
        } else { // assume string
            String value = cell.toString();
            if (value.length() != 1) {
                throw new IPuzFormatException(
                    "Cannot represent values of more than one character: "
                        + value
                );
            }
            return value.charAt(0);
        }
    }

    /**
     * Read clues into puz
     */
    private static void readClues(JSONObject puzJson, Puzzle puz)
            throws IPuzFormatException {
        // default to true as it is safest
        boolean showEnumerations = puzJson.optBoolean(
            FIELD_SHOW_ENUMERATIONS, true
        );

        JSONObject clues = puzJson.getJSONObject(FIELD_CLUES);

        for (String unsupportedField : FIELD_CLUES_UNSUPPORTED) {
            if (getCluesSubList(clues, unsupportedField) != null) {
                throw new IPuzFormatException(
                    "Unsupported clues list: " + unsupportedField
                );
            }
        }

        JSONArray across = getCluesSubList(clues, FIELD_CLUES_ACROSS);
        JSONArray down = getCluesSubList(clues, FIELD_CLUES_DOWN);

        if (across == null && down == null) {
            throw new IPuzFormatException(
                "No across or down clues found in puzzle"
            );
        }

        addClues(across, true, showEnumerations, puz);
        addClues(down, false, showEnumerations, puz);
    }

    /**
     * Get sub list from clues object
     *
     * May be of the form "field" or "field:displayname".
     *
     * @return the json array of clues or null if not found
     */
    private static JSONArray getCluesSubList(JSONObject clues, String name) {
        JSONArray list = clues.optJSONArray(name);

        if (list != null)
            return list;

        JSONArray names = clues.names();
        for (int i = 0; i < names.length(); i++) {
            String field = names.getString(i);
            if (field.startsWith(name + ":"))
                return clues.getJSONArray(field);
        }

        return null;
    }

    /**
     * Transfer clues from json to puzzle
     *
     * Adds enumeration text to hint if showEnumerations is true
     */
    private static void addClues(
        JSONArray jsonClues, boolean across, boolean showEnumerations,
        Puzzle puz
    ) throws IPuzFormatException {
        for (int i = 0; i < jsonClues.length(); i++) {
            Object clueObj = jsonClues.get(i);
            Clue clue = getClue(clueObj, across, showEnumerations);
            if (clue != null)
                puz.addClue(clue);
        }
    }

    /**
     * Convert a JSON object clue into a Clue
     *
     * Adds enumeration to hint if showEnumerations is true
     */
    private static Clue getClue(
        Object clueObj, boolean across, boolean showEnumerations
    ) throws IPuzFormatException {
        if (clueObj instanceof JSONArray) {
            JSONArray clueArray = (JSONArray) clueObj;
            if (clueArray.length() != 2) {
                throw new IPuzFormatException(
                    "Unexpected clue array length: " + clueArray.length()
                );
            }
            Object clueNumObj = clueArray.get(0);
            String hint = unHtmlString(clueArray.getString(1));

            return buildClue(clueNumObj, across, hint, null);
        } else if (clueObj instanceof JSONObject) {
            JSONObject clueJson = (JSONObject) clueObj;

            // get clue number
            Object clueNumObj = clueJson.opt(FIELD_CLUE_NUMBER);
            int number = getClueNumber(clueNumObj);
            if (number < 0) {
                clueNumObj = clueJson.opt(FIELD_CLUE_NUMBERS);
                number = getClueNumber(clueNumObj);
                if (number < 0) {
                    throw new IPuzFormatException(
                        "Could not get clue number from clue: " + clueObj
                    );
                }
            }

            // build hint, bake in additional info
            StringBuilder hint = new StringBuilder();

            hint.append(getHtmlOptString(clueJson, FIELD_CLUE_HINT));

            JSONArray conts = clueJson.optJSONArray(FIELD_CLUE_CONTINUED);
            if (conts != null && conts.length() > 0) {
                addCrossRefList(hint, conts, "cont.");
            }

            JSONArray refs = clueJson.optJSONArray(FIELD_CLUE_REFERENCES);
            if (refs != null && refs.length() > 0) {
                addCrossRefList(hint, refs, "ref.");
            }

            String enumeration = showEnumerations
                ? clueJson.optString(FIELD_ENUMERATION)
                : null;

            return buildClue(clueNumObj, across, hint.toString(), enumeration);
        } else {
            throw new IPuzFormatException(
                "Unsupported clue format " + clueObj.getClass() + ": " + clueObj
            );
        }
    }

    /**
     * Add cross references to hint
     *
     * @param hint hint being built
     * @param refs list of references
     * @param description text to identify what reference list is (e.g.
     * "cont." or "ref.")
     */
    private static void addCrossRefList(
        StringBuilder hint, JSONArray refs, String description
    ) {
        hint.append(" (");
        hint.append(description);
        hint.append(" ");
        for (int i = 0; i < refs.length(); i++) {
            if (i > 0)
                hint.append("/");
            JSONObject ref = refs.getJSONObject(i);
            hint.append(getComplexClueNumString(
                ref.get(FIELD_CLUE_NUMBER))
            );
            hint.append(" ");;
            hint.append(ref.getString(FIELD_CLUE_DIRECTION));
        }
        hint.append(")");
    }

    /**
     * Build a Clue object from info
     *
     * In particular handles the different clue number formats
     *
     * @param clueNumObj the ClueNum object in JSON
     * @param enumeration null or empty if no enumeration to be shown in clue
     */
    private static Clue buildClue(
        Object clueNumObj, boolean across, String hint, String enumeration
    ) throws IPuzFormatException {
        int number = getClueNumber(clueNumObj);

        if (number < 0) {
            throw new IPuzFormatException(
                "Could not get clue number from " + clueNumObj
            );
        }

        if (isComplexClueNumber(clueNumObj)) {
            String numString = getComplexClueNumString(clueNumObj);
            if (numString != null && numString.length() > 0)
                hint += " (clues " + numString + ")";
        }

        if (enumeration != null && enumeration.length() > 0) {
            hint += " (" + enumeration + ")";
        }

        return new Clue(number, across, hint);
    }

    /**
     * Check if clueNumObj is something other than a simple number
     *
     * Will accept a JSONArray of ClueNums not just a single ClueNum
     */
    private static boolean isComplexClueNumber(Object clueNumObj)
            throws IPuzFormatException {
        if (clueNumObj instanceof Number)
            return false;

        if (clueNumObj instanceof String) {
            String numString = (String) clueNumObj;
            return !numString.matches("^\\d+$");
        }

        if (clueNumObj instanceof JSONArray) {
            return true;
        }

        throw new IPuzFormatException(
            "Unrecognised clue number format: " + clueNumObj.getClass()
        );
    }

    /**
     * Return basic number from clueNumObj
     *
     * If a number, return its int value. If a string, return first
     * integer in the string. If a JSONArray return the first item in
     * the array from which a number can be extracted.
     *
     * Else return -1 (including if null passed).
     */
    private static int getClueNumber(Object clueNumObj) {
        if (clueNumObj instanceof Number)
            return ((Number) clueNumObj).intValue();

        if (clueNumObj instanceof String) {
            String numString = (String) clueNumObj;

            Matcher intMatch = INT_STRING_RE.matcher(numString);

            if (intMatch.find())
                return Integer.valueOf(intMatch.group());
        }

        if (clueNumObj instanceof JSONArray) {
            JSONArray clueNums = (JSONArray) clueNumObj;

            for (Object subNumObj : clueNums) {
                int subNum = getClueNumber(subNumObj);
                if (subNum >= 0)
                    return subNum;
            }
        }

        return -1;
    }

    /**
     * Extract string from a complex clue number representation
     *
     * E.g. "1/2" from ["1", "2"]
     *
     * Returns null if nothing useful could be extracted.
     */
    private static String getComplexClueNumString(Object clueNumObj) {
        if (clueNumObj instanceof Number)
            return String.valueOf(((Number) clueNumObj).intValue());

        if (clueNumObj instanceof String)
            return (String) clueNumObj;

        if (clueNumObj instanceof JSONArray) {
            JSONArray objs = (JSONArray) clueNumObj;

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < objs.length(); i++) {
                if (i > 0)
                    builder.append("/");
                builder.append(getComplexClueNumString(objs.get(i)));
            }

            return builder.toString();
        }

        return null;
    }

    /**
     * Read non-standard IPuz fields into puzzle
     */
    private static void readExtensions(JSONObject puzJson, Puzzle puz)
            throws IPuzFormatException {
        String supportUrl = puzJson.optString(FIELD_EXT_SUPPORT_URL);
        if (supportUrl != null && !supportUrl.isEmpty())
            puz.setSupportUrl(supportUrl);

        JSONObject playData = puzJson.optJSONObject(FIELD_EXT_PLAY_DATA);

        if (playData != null && !JSONObject.NULL.equals(playData))
            readPlayData(playData, puz);
    }

    /**
     * Read play data extension from playData object to puz
     *
     * @param playData the playData field of the puz json
     */
    private static void readPlayData(JSONObject playData, Puzzle puz)
            throws IPuzFormatException {
        readBoxExtras(playData, puz);
        readPosition(playData, puz);
        readClueHistory(playData, puz);
        readClueNotes(playData, puz);

        if (playData.has(FIELD_COMPLETION_TIME))
            puz.setTime(playData.getLong(FIELD_COMPLETION_TIME));

        if (playData.has(FIELD_UPDATABLE))
            puz.setUpdatable(playData.getBoolean(FIELD_UPDATABLE));
    }

    /**
     * Read non-standard info about boxes (e.g. is cheated)
     *
     * Assumes boxes have been set on puz
     */
    private static void readBoxExtras(JSONObject playData, Puzzle puz) {
        if (!playData.has(FIELD_BOX_EXTRAS))
            return;

        JSONArray cellsJson = playData.getJSONArray(FIELD_BOX_EXTRAS);

        Box[][] boxes = puz.getBoxes();

        int numRows = Math.min(cellsJson.length(), boxes.length);

        for (int row = 0; row < numRows; row++) {
            JSONArray rowJson = cellsJson.getJSONArray(row);

            int numCols = Math.min(rowJson.length(), boxes[row].length);

            for (int col = 0; col < numCols; col++) {
                Box box = boxes[row][col];
                if (box != null) {
                    JSONObject boxJson = rowJson.getJSONObject(col);

                    if (boxJson.has(FIELD_BOX_CHEATED)) {
                        box.setCheated(boxJson.getBoolean(FIELD_BOX_CHEATED));
                    }
                    if (boxJson.has(FIELD_BOX_RESPONDER)) {
                        box.setResponder(
                            boxJson.getString(FIELD_BOX_RESPONDER)
                        );
                    }
                }
            }
        }
    }

    /**
     * Read the position from playData
     *
     * Assumes puz.getWidth() and puz.getHeight() returns accurate data
     */
    private static void readPosition(JSONObject playData, Puzzle puz) {
        if (!playData.has(FIELD_POSITION))
            return;

        JSONObject positionJson = playData.getJSONObject(FIELD_POSITION);

        if (positionJson.has(FIELD_POSITION_ROW)
                && positionJson.has(FIELD_POSITION_COL)
                && positionJson.has(FIELD_POSITION_ACROSS)) {

            int row = positionJson.optInt(FIELD_POSITION_ROW, -1);
            int col = positionJson.optInt(FIELD_POSITION_COL, -1);
            boolean across = positionJson.getBoolean(FIELD_POSITION_ACROSS);

            if (0 <= row && row <= puz.getHeight()
                    && 0 <= col && col <= puz.getWidth()) {
                puz.setPosition(new Position(col, row));
                puz.setAcross(across);
            }
        }
    }

    /**
     * Reads clue history from playData
     */
    private static void readClueHistory(JSONObject playData, Puzzle puz) {
        if (!playData.has(FIELD_CLUE_HISTORY))
            return;

        JSONArray historyJson = playData.getJSONArray(FIELD_CLUE_HISTORY);

        LinkedList<ClueNumDir> history = new LinkedList<>();

        for (int i = 0; i < historyJson.length(); i++) {
            JSONObject itemJson = historyJson.getJSONObject(i);
            ClueNumDir cnd = decodeClueNumDir(itemJson);
            if (cnd != null)
                history.add(cnd);
        }

        puz.setHistory(history);
    }

    /**
     * Read notes from playData
     */
    private static void readClueNotes(JSONObject playData, Puzzle puz) {
        if (!playData.has(FIELD_CLUE_NOTES))
            return;

        JSONArray notesJson = playData.getJSONArray(FIELD_CLUE_NOTES);

        for (int i = 0; i < notesJson.length(); i++) {
            JSONObject noteJson = notesJson.getJSONObject(i);
            JSONObject cndJson = noteJson.optJSONObject(FIELD_CLUE_NOTE_CLUE);
            ClueNumDir cnd = decodeClueNumDir(cndJson);

            if (cnd != null) {
                String scratch
                    = noteJson.optString(FIELD_CLUE_NOTE_SCRATCH, null);
                String text
                    = getHtmlOptString(noteJson, FIELD_CLUE_NOTE_TEXT);
                String anagramSrc
                    = noteJson.optString(FIELD_CLUE_NOTE_ANAGRAM_SRC, null);
                String anagramSol
                    = noteJson.optString(FIELD_CLUE_NOTE_ANAGRAM_SOL, null);

                if (scratch != null
                        || text != null
                        || anagramSrc != null
                        || anagramSol != null) {
                    puz.setNote(
                        cnd.getClueNumber(),
                        new Note(scratch, text, anagramSrc, anagramSol),
                        cnd.getAcross()
                    );
                }
            }
        }
    }

    /**
     * Read a JSON representation of ClueNumDir to ClueNumDir
     *
     * @return null if not right
     */
    private static ClueNumDir decodeClueNumDir(JSONObject cnd) {
        if (cnd == null)
            return null;
        if (JSONObject.NULL.equals(cnd))
            return null;
        if (!cnd.has(FIELD_CLUE_NUMBER) || !cnd.has(FIELD_CLUE_ACROSS))
            return null;

        int number = cnd.getInt(FIELD_CLUE_NUMBER);
        boolean across = cnd.getBoolean(FIELD_CLUE_ACROSS);

        return new ClueNumDir(number, across);
    }

    /**
     * Write puzzle to os using WRITE_CHARSET
     */
    public static void writePuzzle(Puzzle puz, OutputStream os)
            throws IOException {
        JSONObject json = new JSONObject();

        addIPuzHeader(json);
        addMetaData(puz, json);
        addBoxes(puz, json);
        addClues(puz, json);
        addExtensions(puz, json);

        byte[] encoded = WRITE_CHARSET.encode(json.toString()).array();
        os.write(encoded);
    }

    /**
     * Add IPuz version and kind
     */
    private static void addIPuzHeader(JSONObject puzJson) {
        puzJson.put(FIELD_VERSION, WRITE_VERSION);

        JSONArray kinds = new JSONArray();
        kinds.put(WRITE_KIND);
        puzJson.put(FIELD_KIND, kinds);
    }

    /**
     * Add basic puzzle metadata (title, author, etc).
     */
    private static void addMetaData(Puzzle puz, JSONObject puzJson) {
        safePutHTML(puzJson, FIELD_TITLE, puz.getTitle());
        safePutHTML(puzJson, FIELD_AUTHOR, puz.getAuthor());
        safePut(puzJson, FIELD_COPYRIGHT, puz.getCopyright());
        safePutHTML(puzJson, FIELD_NOTES, puz.getNotes());
        safePut(puzJson, FIELD_URL, puz.getSourceUrl());
        safePutHTML(puzJson, FIELD_PUBLISHER, puz.getSource());

        LocalDate date = puz.getDate();
        if (date != null)
            puzJson.put(FIELD_DATE, DATE_FORMATTER.format(date));
    }

    /**
     * Read all IPuz supported box information to json
     */
    private static void addBoxes(Puzzle puz, JSONObject puzJson) {
        addDimensions(puz, puzJson);
        addPuzzleCells(puz, puzJson);
        addSaved(puz, puzJson);
        addSolution(puz, puzJson);
    }

    /**
     * Add puzzle dimensions to json
     */
    private static void addDimensions(Puzzle puz, JSONObject puzJson) {
        JSONObject dimensions = new JSONObject();
        dimensions.put(FIELD_WIDTH, puz.getWidth());
        dimensions.put(FIELD_HEIGHT, puz.getHeight());
        puzJson.put(FIELD_DIMENSIONS, dimensions);
    }

    /**
     * Add the puzzle field to json
     */
    private static void addPuzzleCells(Puzzle puz, JSONObject puzJson) {
        JSONArray cellsJson = new JSONArray();

        Box[][] boxes = puz.getBoxes();

        for (int row = 0; row < boxes.length; row++) {
            JSONArray rowJson = new JSONArray();

            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];

                if (box == null) {
                    rowJson.put(DEFAULT_BLOCK);
                } else {
                    int clueNumber = box.getClueNumber();

                    if (box.isCircled()) {
                        JSONObject styleJson = new JSONObject();
                        styleJson.put(FIELD_SHAPE_BG, SHAPE_BG_CIRCLE);

                        JSONObject cellJson = new JSONObject();

                        cellJson.put(FIELD_STYLE, styleJson);

                        if (clueNumber > 0)
                            cellJson.put(FIELD_CELL, clueNumber);

                        rowJson.put(cellJson);
                    } else if (clueNumber > 0) {
                        rowJson.put(clueNumber);
                    } else {
                        rowJson.put(DEFAULT_EMPTY);
                    }
                }
            }

            cellsJson.put(rowJson);
        }

        puzJson.put(FIELD_PUZZLE, cellsJson);
    }

    /**
     * Add the saved field to json
     */
    private static void addSaved(Puzzle puz, JSONObject puzJson) {
        JSONArray cellsJson = new JSONArray();

        Box[][] boxes = puz.getBoxes();

        for (int row = 0; row < boxes.length; row++) {
            JSONArray rowJson = new JSONArray();

            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];

                if (box == null)
                    rowJson.put(DEFAULT_BLOCK);
                else if (box.isBlank())
                    rowJson.put(DEFAULT_EMPTY);
                else
                    rowJson.put(String.valueOf(box.getResponse()));
            }

            cellsJson.put(rowJson);
        }

        puzJson.put(FIELD_SAVED, cellsJson);
    }

    /**
     * Add the solution field to json if the puzzle has one
     */
    private static void addSolution(Puzzle puz, JSONObject puzJson) {
        if (!puz.hasSolution())
            return;

        JSONArray cellsJson = new JSONArray();

        Box[][] boxes = puz.getBoxes();

        for (int row = 0; row < boxes.length; row++) {
            JSONArray rowJson = new JSONArray();

            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];

                if (box == null) {
                    rowJson.put(DEFAULT_BLOCK);
                } else if (box.hasSolution()) {
                    rowJson.put(String.valueOf(box.getSolution()));
                } else {
                    rowJson.put(JSONObject.NULL);
                }
            }

            cellsJson.put(rowJson);
        }

        puzJson.put(FIELD_SOLUTION, cellsJson);
    }

    /**
     * Add the clues lists to the json
     */
    private static void addClues(Puzzle puz, JSONObject puzJson) {
        JSONObject cluesJson = new JSONObject();

        JSONArray acrossJson = getClueListJsonArray(puz.getClues(true));
        JSONArray downJson = getClueListJsonArray(puz.getClues(false));

        cluesJson.put(FIELD_CLUES_ACROSS, acrossJson);
        cluesJson.put(FIELD_CLUES_DOWN, downJson);

        puzJson.put(FIELD_CLUES, cluesJson);
    }

    /**
     * Convert a clues list into a json array and return it
     */
    private static JSONArray getClueListJsonArray(ClueList clues) {
        JSONArray cluesJson = new JSONArray();

        for (Clue clue : clues) {
            JSONArray clueJson = new JSONArray();
            clueJson.put(clue.getNumber());
            String hint = clue.getHint();
            clueJson.put(hint == null ? NULL_CLUE : htmlString(hint));

            cluesJson.put(clueJson);
        }

        return cluesJson;
    }

    /**
     * Write Puzzle features not natively supported by IPuz
     */
    private static void addExtensions(Puzzle puz, JSONObject puzJson) {
        addExtensionVolatility(puzJson);

        safePut(puzJson, FIELD_EXT_SUPPORT_URL, puz.getSupportUrl());

        JSONObject playData = new JSONObject();

        addBoxExtras(puz, playData);
        addPosition(puz, playData);
        addClueHistory(puz, playData);
        addClueNotes(puz, playData);

        playData.put(FIELD_COMPLETION_TIME, puz.getTime());
        playData.put(FIELD_PCNT_FILLED, puz.getPercentFilled());
        playData.put(FIELD_PCNT_COMPLETE, puz.getPercentComplete());
        playData.put(FIELD_UPDATABLE, puz.isUpdatable());

        puzJson.put(FIELD_EXT_PLAY_DATA, playData);
    }

    /**
     * Add Note objects about individual clues to playData
     *
     * @param playData the FIELD_EXT_PLAY_DATA object to add to
     */
    private static void addClueNotes(Puzzle puz, JSONObject playData) {
        JSONArray notesJson = new JSONArray();

        for (ClueNumDir cnd : puz.getClueNumDirs()) {
            Note note = puz.getNote(cnd.getClueNumber(), cnd.getAcross());
            if (note != null && !note.isEmpty()) {
                JSONObject noteJson = new JSONObject();

                noteJson.put(FIELD_CLUE_NOTE_CLUE, encodeClueNumDir(cnd));
                safePut(
                    noteJson,
                    FIELD_CLUE_NOTE_SCRATCH,
                    note.getCompressedScratch()
                );
                safePutHTML(
                    noteJson,
                    FIELD_CLUE_NOTE_TEXT,
                    note.getText()
                );
                safePut(
                    noteJson,
                    FIELD_CLUE_NOTE_ANAGRAM_SRC,
                    note.getCompressedAnagramSource()
                );
                safePut(
                    noteJson,
                    FIELD_CLUE_NOTE_ANAGRAM_SOL,
                    note.getCompressedAnagramSolution()
                );

                notesJson.put(noteJson);
            }
        }

        if (notesJson.length() > 0)
            playData.put(FIELD_CLUE_NOTES, notesJson);
    }

    /**
     * Encode a ClueNumDir as a JSONObject
     */
    private static JSONObject encodeClueNumDir(ClueNumDir cnd) {
        JSONObject cndJson = new JSONObject();
        cndJson.put(FIELD_CLUE_NUMBER, cnd.getClueNumber());
        cndJson.put(FIELD_CLUE_ACROSS, cnd.getAcross());
        return cndJson;
    }

    /**
     * Add clue history list to playData
     *
     * @param playData the FIELD_EXT_PLAY_DATA object to add to
     */
    private static void addClueHistory(Puzzle puz, JSONObject playData) {
        List<ClueNumDir> history = puz.getHistory();
        if (history.isEmpty())
            return;

        JSONArray historyJson = new JSONArray();
        for (ClueNumDir item : puz.getHistory()) {
            historyJson.put(encodeClueNumDir(item));
        }

        playData.put(FIELD_CLUE_HISTORY, historyJson);
    }

    /**
     * Add current highlight position to playData
     *
     * @param playData the FIELD_EXT_PLAY_DATA object to add to
     */
    private static void addPosition(Puzzle puz, JSONObject playData) {
        Position pos = puz.getPosition();
        if (pos == null)
            return;

        JSONObject positionJson = new JSONObject();

        positionJson.put(FIELD_POSITION_ROW, pos.down);
        positionJson.put(FIELD_POSITION_COL, pos.across);
        positionJson.put(FIELD_POSITION_ACROSS, puz.getAcross());

        playData.put(FIELD_POSITION, positionJson);
    }

    /**
     * Add additional info about boxes (cheated, responder) to playData
     * if there is any
     *
     * @param playData the FIELD_EXT_PLAY_DATA object to add to
     */
    private static void addBoxExtras(Puzzle puz, JSONObject playData) {
        if (!puz.hasCheated() && !puz.hasResponders())
            return;

        Box[][] boxes = puz.getBoxes();
        if (boxes == null)
            return;

        JSONArray cellsJson = new JSONArray();

        for (int row = 0; row < boxes.length; row++) {
            JSONArray rowJson = new JSONArray();
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                JSONObject boxJson = new JSONObject();

                if (box != null) {
                    if (box.isCheated())
                        boxJson.put(FIELD_BOX_CHEATED, true);
                    safePut(boxJson, FIELD_BOX_RESPONDER, box.getResponder());
                }

                rowJson.put(boxJson);
            }
            cellsJson.put(rowJson);
        }

        playData.put(FIELD_BOX_EXTRAS, cellsJson);
    }

    /**
     * Write volatility info about our extensions to json
     */
    private static void addExtensionVolatility(JSONObject puzJson) {
        JSONObject volatileJson = new JSONObject();

        for (String field : VOLATILE_EXTENSIONS)
            volatileJson.put(field, FIELD_IS_VOLATILE);

        for (String field : NON_VOLATILE_EXTENSIONS)
            volatileJson.put(field, FIELD_IS_NOT_VOLATILE);
    }

    /**
     * Puts the object if it is not null
     */
    private static void safePut(JSONObject json, String field, Object value) {
        if (value != null)
            json.put(field, value);
    }

    /**
     * Puts the String with HTML encoding if it is not null
     */
    private static void safePutHTML(JSONObject json, String field, String value) {
        if (value != null) {
            json.put(field, htmlString(value));
        }
    }

    /**
     * Determine character encoding of JSON from first byte
     *
     * Ala RFC 4627.
     *
     * @return null if baos does not contain enough bytes
     */
    private static String getCharsetName(VisibleByteArrayOutputStream baos) {
        // 00 00 00 xx  UTF-32BE
        // 00 xx 00 xx  UTF-16BE
        // xx 00 00 00  UTF-32LE
        // xx 00 xx 00  UTF-16LE
        // xx xx xx xx  UTF-8

        if (baos.size() < 4)
            return null;

        if (baos.view(0) == 0 && baos.view(1) == 0 && baos.view(2) == 0)
            return "UTF-32BE";

        if (baos.view(0) == 0 && baos.view(2) == 0)
            return "UTF-16BE";

        if (baos.view(1) == 0 && baos.view(2) == 0 && baos.view(3) == 0)
            return "UTF-32LE";

        if (baos.view(1) == 0 && baos.view(3) == 0)
            return "UTF-16LE";

        return "UTF-8";
    }

    /**
     * Returns fully qualified name of an extension field
     */
    private static String getQualifiedExtensionName(String fieldName) {
        return EXT_NAMESPACE + ":" + fieldName;
    }

    /**
     * ByteArrayOutputStream where you can check bytes written
     *
     * Avoids having to create a copy of the byte buffer just to read
     * the first four bytes when determining JSON charset.
     */
    private static class VisibleByteArrayOutputStream
        extends ByteArrayOutputStream {
        /**
         * See what's at character position
         */
        public byte view(int pos) {
            return buf[pos];
        }
    }
}
