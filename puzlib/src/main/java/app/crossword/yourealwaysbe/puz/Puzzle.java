package app.crossword.yourealwaysbe.puz;

import app.crossword.yourealwaysbe.puz.Playboard.Position;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Logger;

public class Puzzle implements Serializable{
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    @FunctionalInterface
    public static interface ClueNumDirConsumer {
        public void accept(int number, boolean across) throws Exception;
    }

    private String author;
    private String copyright;
    private String notes;
    private String title;
    private MutableClueList acrossClues = new MutableClueList();
    private MutableClueList downClues = new MutableClueList();
    private int numberOfClues;
    private LocalDate pubdate = LocalDate.now();
    private String source;
    private String sourceUrl = "";
    private String supportUrl;
    private Box[][] boxes;
    private boolean updatable;
    private int height;
    private int width;
    private long playedTime;
    private boolean scrambled;
    public short solutionChecksum;

    // current play position data needed for saving state...
    private Position position;
    private boolean across = true;

    private SortedMap<Integer, Note> acrossNotes = new TreeMap<>();
    private SortedMap<Integer, Note> downNotes = new TreeMap<>();

    private LinkedList<ClueNumDir> historyList = new LinkedList<>();

    // Temporary fields used for unscrambling.
    public int[] unscrambleKey;
    public byte[] unscrambleTmp;
    public byte[] unscrambleBuf;

    public void addClue(Clue clue) {
        if (clue.getIsAcross())
            this.acrossClues.addClue(clue);
        else
            this.downClues.addClue(clue);
    }

    /**
     * Get clue lists
     *
     * Note: no reason to assume there is a numbered clue for every
     * numbered position on the board. This is not always the case when
     * clues span multiple entries.
     */
    public ClueList getClues(boolean across) {
        return across ? this.acrossClues : this.downClues;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    private static Box checkedGet(Box[][] boxes, int row, int col) {
        try {
            return boxes[row][col];
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static boolean somethingAbove(Box[][] boxes, int row, int col) {
        return checkedGet(boxes, row - 1, col) != null;
    }

    private static boolean somethingBelow(Box[][]boxes, int row, int col) {
        return checkedGet(boxes, row + 1, col) != null;
    }

    private static boolean somethingLeftOf(Box[][] boxes, int row, int col) {
        return checkedGet(boxes, row, col - 1) != null;
    }

    private static boolean somethingRightOf(Box[][] boxes, int row, int col) {
        return checkedGet(boxes, row, col + 1) != null;
    }

    /**
     * Will automatically fill in clue numbers
     *
     * Follows standard crossword rules. Overrides any existing
     * numbering on the boxes. This may be something that needs to
     * change in future. Also sets height and width.
     *
     * @param boxes boxes in row, col order, null means black square.
     * Must be a true grid.
     * @throws IllegalArgumentException if the boxes are not a grid, or
     * contain numbering inconsistent with the "standard" crossword
     * numbering system.
     */
    public void setBoxes(Box[][] boxes) {
        this.boxes = boxes;

        int clueCount = 1;

        this.height = boxes.length;
        this.width = height > 0 ? boxes[0].length : 0;

        for (int row = 0; row < boxes.length; row++) {
            if (boxes[row].length != width) {
                throw new IllegalArgumentException(
                    "Boxes do not form a grid"
                );
            }

            for (int col = 0; col < boxes[row].length; col++) {
                if (boxes[row][col] == null) {
                    continue;
                }

                boolean tickedClue = false;
                if (!somethingAbove(boxes, row, col)) {
                    if (somethingBelow(boxes, row, col)) {
                        boxes[row][col].setDown(true);
                        tickedClue = true;
                    }
                }

                if (!somethingLeftOf(boxes, row, col)) {
                    if (somethingRightOf(boxes, row, col)) {
                        boxes[row][col].setAcross(true);
                        tickedClue = true;
                    }
                }

                int boxNumber = boxes[row][col].getClueNumber();

                if (tickedClue) {
                    if (boxNumber > 0 && boxNumber != clueCount) {
                        throw new IllegalArgumentException(
                            "Box clue number " + boxNumber
                                + " does not match expected "
                                + clueCount
                        );
                    }

                    boxes[row][col].setClueNumber(clueCount);
                    clueCount++;
                } else {
                    if (boxNumber > 0) {
                        throw new IllegalArgumentException(
                            "Box numbered " + boxNumber
                                + " expected not to be numbered"
                        );
                    }
                }
            }
        }

        // set partOfAcross numbers
        int maxRowLen = -1;
        for (int row = 0; row < boxes.length; row++) {
            int lastAcross = -1;
            int acrossPosition = -1;
            maxRowLen = java.lang.Math.max(maxRowLen, boxes[row].length);
            for (int col = 0; col < boxes[row].length; col++) {
                if (boxes[row][col] == null) {
                    lastAcross = -1;
                    continue;
                }

                if (boxes[row][col].isAcross()) {
                    lastAcross = boxes[row][col].getClueNumber();
                    acrossPosition = 0;
                }

                if (lastAcross > 0) {
                    boxes[row][col].setPartOfAcrossClueNumber(lastAcross);
                    boxes[row][col].setAcrossPosition(acrossPosition);
                    acrossPosition++;
                }
            }
        }

        // set partOfDown numbers
        for (int col = 0; col < maxRowLen; col++) {
            int lastDown = -1;
            int downPosition = -1;
            for (int row = 0; row < boxes.length; row++) {
                if (col >= boxes[row].length || boxes[row][col] == null) {
                    lastDown = -1;
                    continue;
                }

                if (boxes[row][col].isDown()) {
                    lastDown = boxes[row][col].getClueNumber();
                    downPosition = 0;
                }

                if (lastDown > -1) {
                    boxes[row][col].setPartOfDownClueNumber(lastDown);
                    boxes[row][col].setDownPosition(downPosition);
                    downPosition++;
                }
            }
        }

    }

    public Box[][] getBoxes() {
        return boxes;
    }

    /**
     * Assumes height and width has been set
     *
     * See setBoxes for more details
     */
    public void setBoxesFromList(Box[] boxesList, int width, int height) {
        int i = 0;
        Box[][] boxes = new Box[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boxes[y][x] = boxesList[i++];
            }
        }

        setBoxes(boxes);
    }

    public Box[] getBoxesList() {
        Box[] result = new Box[boxes.length * boxes[0].length];
        int i = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                result[i++] = boxes[x][y];
            }
        }

        return result;
    }

    /**
     * Iterate over clue starts in board order
     *
     * Left to right, top to bottom, across before down
     */
    public Iterable<ClueNumDir> getClueNumDirs() {
        return new Iterable<ClueNumDir>() {
            public Iterator<ClueNumDir> iterator() {
                return new Iterator<ClueNumDir>() {
                    private final int width = getWidth();
                    private final int height = getHeight();
                    private final Box[][] boxes = getBoxes();

                    // next position (0, 0, across) -> (0, 0, down) -> (0, 1,
                    // across) -> ...
                    private int row = 0;
                    private int col = 0;
                    private boolean across = true;

                    { moveToNext(); }

                    @Override
                    public boolean hasNext() {
                        return row < height;
                    }

                    @Override
                    public ClueNumDir next() {
                        int number = boxes[row][col].getClueNumber();
                        ClueNumDir result = new ClueNumDir(number, across);
                        moveOneStep();
                        moveToNext();
                        return result;
                    }

                    /**
                     * Find next clue/dir position including current position
                     */
                    private void moveToNext() {
                        while (row < height) {
                            Box box = boxes[row][col];
                            if (box != null && box.getClueNumber() > 0) {
                                if (across && box.isAcross())
                                    return;
                                else if (!across && box.isDown())
                                    return;
                            }
                            moveOneStep();
                        }
                    }

                    /**
                     * Move to next position, one step, not to next clue/dir
                     * position
                     */
                    private void moveOneStep() {
                        if (across) {
                            across = false;
                        } else {
                            across = true;
                            col = (col + 1) % width;
                            if (col == 0)
                                row += 1;
                        }
                    }
                };
            }
        };
    }

    /**
     * Initialize the temporary unscramble buffers.  Returns the scrambled solution.
     */
    public byte[] initializeUnscrambleData() {
        unscrambleKey = new int[4];
        unscrambleTmp = new byte[9];

        byte[] solution = getSolutionDown();
        unscrambleBuf = new byte[solution.length];

        return solution;
    }

    private byte[] getSolutionDown() {
        StringBuilder ans = new StringBuilder();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (boxes[y][x] != null) {
                    ans.append(boxes[y][x].getSolution());
                }
            }
        }
        return ans.toString().getBytes();
    }

    public void setUnscrambledSolution(byte[] solution) {
        int i = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (boxes[y][x] != null) {
                    boxes[y][x].setSolution((char) solution[i++]);
                }
            }
        }
        setScrambled(false);
        setUpdatable(false);
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setDate(LocalDate date) {
        this.pubdate = date;
    }

    public LocalDate getDate() {
        return pubdate;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public int getNumberOfClues() {
        return this.acrossClues.size() + this.downClues.size();
    }

    public int getPercentComplete() {
        int total = 0;
        int correct = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] != null) {
                    total++;

                    if (boxes[x][y].getResponse() == boxes[x][y].getSolution()) {
                        correct++;
                    }
                }
            }
        }
        if(total == 0){
            return 0;
        }
        return (correct * 100) / (total);
    }

    public int getPercentFilled() {
        int total = 0;
        int filled = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] != null) {
                    total++;

                    if (!boxes[x][y].isBlank()) {
                        filled++;
                    }
                }
            }
        }

        return (filled * 100) / (total);
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSupportUrl(String supportUrl) {
        this.supportUrl = supportUrl;
    }

    public String getSupportUrl() {
        return supportUrl;
    }

    public void setTime(long time) {
        this.playedTime = time;
    }

    public long getTime() {
        return this.playedTime;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    /**
     * Set whether current position is across
     */
    public void setAcross(boolean across) {
        this.across = across;
    }

    /**
     * Get whether current position is across
     */
    public boolean getAcross() {
        return across;
    }

    public void setScrambled(boolean scrambled) {
        this.scrambled = scrambled;
    }

    public boolean isScrambled() {
        return scrambled;
    }

    public void setSolutionChecksum(short checksum) {
        this.solutionChecksum = checksum;
    }

    public short getSolutionChecksum() {
        return solutionChecksum;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns null if no note
     */
    public Note getNote(int clueNum, boolean isAcross) {
        if (isAcross)
            return acrossNotes.get(clueNum);
        else
            return downNotes.get(clueNum);
    }

    /**
     * Set note for a clue only if clue exists in puzzle
     */
    public void setNote(int clueNum, Note note, boolean isAcross) {
        if (!getClues(isAcross).hasClue(clueNum))
            return;

        if (isAcross)
            acrossNotes.put(clueNum, note);
        else
            downNotes.put(clueNum, note);
    }

    /**
     * Returns true if some box has a solution set
     */
    public boolean hasSolution() {
        if (boxes == null)
            return false;

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null && box.hasSolution())
                    return true;
            }
        }

        return false;
    }

    /**
     * Returns true if some box is circled
     */
    public boolean hasCircled() {
        if (boxes == null)
            return false;

        for (int row = 0; row < boxes.length; row++) {
            for (int col = 0; col < boxes[row].length; col++) {
                Box box = boxes[row][col];
                if (box != null && box.isCircled())
                    return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (Puzzle.class != obj.getClass()) {
            return false;
        }

        Puzzle other = (Puzzle) obj;

        if (!acrossClues.equals(other.acrossClues)) {
            return false;
        }

        if (!downClues.equals(other.downClues)) {
            return false;
        }

        if (author == null) {
            if (other.author != null) {
                return false;
            }
        } else if (!author.equals(other.author)) {
            return false;
        }

        Box[][] b1 = boxes;
        Box[][] b2 = other.boxes;
        boolean boxEq = true;

        for (int x = 0; x < b1.length; x++) {
            for (int y = 0; y < b1[x].length; y++) {
                boxEq = boxEq
                    ? ((b1[x][y] == b2[x][y]) || b1[x][y].equals(b2[x][y]))
                    : boxEq;
            }
        }

        if (!boxEq) {
            return false;
        }

        if (copyright == null) {
            if (other.copyright != null) {
                return false;
            }
        } else if (!copyright.equals(other.copyright)) {
            return false;
        }

        if (height != other.height) {
            return false;
        }

        if (notes == null) {
            if (other.notes != null) {
                return false;
            }
        } else if (!notes.equals(other.notes)) {
            return false;
        }

        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }

        if (width != other.width) {
            return false;
        }

        if (scrambled != other.scrambled) {
            return false;
        }

        if (solutionChecksum != other.solutionChecksum) {
            return false;
        }

        if (!acrossNotes.equals(other.acrossNotes)) {
            return false;
        }

        if (!downNotes.equals(other.downNotes)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + acrossClues.hashCode();
        result = (prime * result) + downClues.hashCode();
        result = (prime * result) + ((author == null) ? 0 : author.hashCode());
        result = (prime * result) + Arrays.hashCode(boxes);
        result = (prime * result) +
            ((copyright == null) ? 0 : copyright.hashCode());
        result = (prime * result) + height;
        result = (prime * result) + ((notes == null) ? 0 : notes.hashCode());
        result = (prime * result) + ((title == null) ? 0 : title.hashCode());
        result = (prime * result) + width;
        result = (prime *result) + acrossNotes.hashCode();
        result = (prime *result) + downNotes.hashCode();

        return result;
    }

    @Override
    public String toString() {
        return "Puzzle " + boxes.length + " x " + boxes[0].length + " " +
        this.title;
    }

    public void updateHistory(int clueNumber, boolean across) {
        if (getClues(across).hasClue(clueNumber)) {
            ClueNumDir item = new ClueNumDir(clueNumber, across);
            // if a new item, not equal to most recent
            if (historyList.isEmpty() ||
                !item.equals(historyList.getFirst())) {
                historyList.remove(item);
                historyList.addFirst(item);
            }
        }
    }

    public void setHistory(List<ClueNumDir> newHistory) {
        historyList.clear();
        for (ClueNumDir item : newHistory) {
            int number = item.getClueNumber();
            if (getClues(item.getAcross()).hasClue(number))
                historyList.add(item);
        }
    }

    public List<ClueNumDir> getHistory() {
        return historyList;
    }

    public static class ClueNumDir {
        private int clueNumber;
        private boolean across;

        public ClueNumDir(int clueNumber, boolean across) {
            this.clueNumber = clueNumber;
            this.across = across;
        }

        public int getClueNumber() { return clueNumber; }
        public boolean getAcross() { return across; }

        public boolean equals(Object o) {
            if (o instanceof ClueNumDir) {
                ClueNumDir other = (ClueNumDir) o;
                return clueNumber == other.clueNumber && across == other.across;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(clueNumber, across);
        }

        public String toString() {
            return clueNumber + (across ? "a" : "d");
        }
    }
}
