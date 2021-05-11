package app.crossword.yourealwaysbe.io;

import app.crossword.yourealwaysbe.io.charset.MacRoman;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Puzzle;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Converts a puzzle from the plaintext format used by King Features Syndicate
 * puzzles to the Across Lite .puz format.  The format is:
 *
 * -Grid shape and clue numbers (redundant)
 * -Solution grid
 * -Across Clues
 * -Down Clues
 *
 * Each section begins with a { character, and each line except the last in a section
 * ends with a | character.  The charset used is Mac Roman.
 *
 * For an example puzzle in this format, see:
 * src/test/resources/premiere-20100704.txt.
 */
public class KingFeaturesPlaintextIO implements PuzzleParser {

    @Override
    public Puzzle parseInput(InputStream is) {
        return parsePuzzle(is);
    }

    /**
     * Take an InputStream containing a plaintext puzzle parsed Puzzle if
     * succeeded, or null if it fails
     * (for example, if the plaintext file is not in a valid format).
     */
    public static Puzzle parsePuzzle(InputStream is) {
        Puzzle puz = new Puzzle();

        Scanner scanner = new Scanner(new InputStreamReader(is, new MacRoman()));

        if (!scanner.hasNextLine()) {
            System.err.println("File empty.");
            return null;
        }

        String line = scanner.nextLine();
        if (!line.startsWith("{") || !scanner.hasNextLine()) {
            System.err.println("First line format incorrect.");
            return null;
        }

        // Skip over redundant grid information.
        line = scanner.nextLine();
        while (!line.startsWith("{")) {
            if (!scanner.hasNextLine()) {
                System.err.println("Unexpected EOF - Grid information.");
                return null;
            }
            line = scanner.nextLine();
        }

        // Process solution grid.
        List<char[]> solGrid = new ArrayList<char[]>();
        line = line.substring(1, line.length()-2);
        String[] rowString = line.split(" ");
        int width = rowString.length;
        do {
            if (line.endsWith(" |")) {
                line = line.substring(0, line.length()-2);
            }
            rowString = line.split(" ");
            if (rowString.length != width) {
                System.err.println("Not a square grid.");
                return null;
            }

            char[] row = new char[width];
            for (int x = 0; x < width; x++) {
                row[x] = rowString[x].charAt(0);
            }
            solGrid.add(row);

            if (!scanner.hasNextLine()) {
                System.err.println("Unexpected EOF - Solution grid.");
                return null;
            }
            line = scanner.nextLine();
        } while (!line.startsWith("{"));

        // Convert solution grid into Box grid.
        int height = solGrid.size();
        Box[][] boxes = new Box[height][width];
        for (int x = 0; x < height; x++) {
            char[] row = solGrid.get(x);
            for (int y = 0; y < width; y++) {
                if (row[y] != '#') {
                    boxes[x][y] = new Box();
                    boxes[x][y].setSolution(row[y]);
                    boxes[x][y].setBlank();
                }
            }
        }

        puz.setBoxes(boxes);

        // Process clues.
        line = line.substring(1);
        int clueNum;
        do {
            if (line.endsWith(" |")) {
                line = line.substring(0, line.length()-2);
            }
            clueNum = 0;
            int i = 0;
            while (line.charAt(i) != '.') {
                if (clueNum != 0) {
                    clueNum *= 10;
                }
                clueNum += line.charAt(i) - '0';
                i++;
            }
            String clue = line.substring(i+2).trim();
            puz.addClue(new Clue(clueNum, true, clue));
            if (!scanner.hasNextLine()) {
                System.err.println("Unexpected EOF - Across clues.");
                return null;
            }
            line = scanner.nextLine();
        } while (!line.startsWith("{"));

        int maxClueNum = clueNum;

        line = line.substring(1);
        boolean finished = false;
        do {
            if (line.endsWith(" |")) {
                line = line.substring(0, line.length()-2);
            } else {
                finished = true;
            }
            clueNum = 0;
            int i = 0;
            while (line.charAt(i) != '.') {
                if (clueNum != 0) {
                    clueNum *= 10;
                }
                clueNum += line.charAt(i) - '0';
                i++;
            }
            String clue = line.substring(i+2).trim();
            puz.addClue(new Clue(clueNum, false, clue));
            if(!finished) {
                if (!scanner.hasNextLine()) {
                    System.err.println("Unexpected EOF - Down clues.");
                    return null;
                }
                line = scanner.nextLine();
            }
        } while (!finished);

        maxClueNum = clueNum > maxClueNum ? clueNum : maxClueNum;

        // Makeshift title
        puz.setTitle("King Features Puzzle");
        puz.setNotes("");

        return puz;
    }

    /**
     * Take an InputStream containing a plaintext puzzle to a DataOutputStream containing
     * the generated .puz file.  Returns true if the process succeeded, or false if it fails
     * (for example, if the plaintext file is not in a valid format).
     */
    public static boolean convertKFPuzzle(
        InputStream is, DataOutputStream os,
        String title, String author, String copyright, LocalDate date
    ) {
        Puzzle puz = parsePuzzle(is);
        if (puz == null)
            return false;

        puz.setTitle(title);
        puz.setAuthor(author);
        puz.setDate(date);
        puz.setCopyright(copyright);

        try {
            IO.saveNative(puz, os);
        } catch (IOException e) {
            System.err.println("Unable to dump puzzle to output stream.");
            return false;
        }

        return true;
    }
}
