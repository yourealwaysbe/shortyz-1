package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.Duration;
import java.time.Period;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

/**
 * Guardian Daily Cryptic downloader
 * URL: https://www.theguardian.com/crosswords/cryptic/
 * Date = Daily
 */
public class GuardianDailyCrypticDownloader extends AbstractDownloader {
    private static final String NAME = "Guardian Daily Cryptic";

    private static final int BASE_CW_NUMBER = 28012;
    private static final LocalDate BASE_CW_DATE = LocalDate.of(2019, 12, 24);

    private static final int CW_WIDTH = 15;
    private static final int CW_HEIGHT = 15;

    public GuardianDailyCrypticDownloader() {
        super("https://www.theguardian.com/crosswords/cryptic/", DOWNLOAD_DIR, NAME);
    }

    public int[] getDownloadDates() {
        return DATE_DAILY;
    }

    public String getName() {
        return NAME;
    }

    public File download(Date date) {
        return download(date, this.createUrlSuffix(date), EMPTY_MAP);
    }

    protected File download(Date date,
                            String urlSuffix,
                            Map<String, String> headers,
                            boolean canDefer) {
        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            JSONObject cw = getCrosswordJSON(url);

            if (cw == null) {
                return canDefer ? Downloader.DEFERRED_FILE : null;
            }

            Puzzle puz = readPuzzleFromJSON(cw);

            File f = new File(downloadDirectory, this.createFileName(date));
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
            puz.setVersion(IO.VERSION_STRING);
            IO.saveNative(puz, dos);
            dos.close();

            PuzzleMeta meta = new PuzzleMeta();
            meta.date = puz.getDate();
            meta.source = getName();
            meta.sourceUrl = url.toString();
            meta.updatable = true;

            utils.storeMetas(Uri.fromFile(f), meta);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected String createUrlSuffix(Date date) {
        // plus one because of exclusivity of calcs
        long dateNumDays = date.getTime() / (1000 * 60 * 60 * 24L) + 1;
        LocalDate localDate = LocalDate.ofEpochDay(dateNumDays);

        Duration diff = Duration.between(BASE_CW_DATE.atStartOfDay(),
                                         localDate.atStartOfDay());

        long daysDiff = diff.toDays();
        int yearsDiff = Period.between(BASE_CW_DATE, localDate).getYears();

        long cwNumOffset = daysDiff;

        // no Sundays
        cwNumOffset -= (daysDiff / 7);

        // no Christmas
        cwNumOffset -= yearsDiff;
        if (localDate.isAfter(BASE_CW_DATE))
            cwNumOffset -= 1;

        long cwNum = BASE_CW_NUMBER + cwNumOffset;

        return Long.toString(cwNum);
    }

    private static JSONObject getCrosswordJSON(URL url)
            throws IOException, JSONException {
        try {
            LOG.info("Downloading " + url);
            Document doc = Jsoup.connect(url.toString()).get();
            String cwJson = doc.select(".js-crossword")
                               .attr("data-crossword-data");

            if (!cwJson.isEmpty())
                return new JSONObject(cwJson);
        } catch (HttpStatusException e) {
            LOG.info("Could not download " + url);
        }
        return null;
    }


    private static Puzzle readPuzzleFromJSON(JSONObject json) throws JSONException {
        Puzzle puz = new Puzzle();

        puz.setTitle(json.getString("name"));
        puz.setAuthor(json.getJSONObject("creator").getString("name"));
        puz.setCopyright("Guardian / " + puz.getAuthor());
        puz.setDate(new Date(json.getInt("date")));

        puz.setWidth(CW_WIDTH);
        puz.setHeight(CW_HEIGHT);

        puz.setBoxes(getBoxes(json));
        puz.setRawClues(getRawClues(json));

        return puz;
    }

    private static Box[][] getBoxes(JSONObject json) throws JSONException {
        Box[][] boxes = new Box[CW_HEIGHT][CW_WIDTH];

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            JSONObject position = entry.getJSONObject("position");
            int x = position.getInt("x");
            int y = position.getInt("y");

            if (x < 0 || x >= CW_WIDTH || y < 0 || y >= CW_HEIGHT)
                continue;

            int num = entry.getInt("number");
            String clueSol = entry.getString("solution");
            String direction = entry.getString("direction");

            int dx = 0;
            int dy = 0;
            if (direction.equals("across"))
                dx = 1;
            else
                dy = 1;

            int boxX = x;
            int boxY = y;
            for (int j = 0; j < clueSol.length(); j++) {
                if (boxX >= CW_WIDTH || boxY >= CW_HEIGHT)
                    break;

                if (boxes[boxY][boxX] == null)
                    boxes[boxY][boxX] = new Box();
                boxes[boxY][boxX].setSolution(clueSol.charAt(j));

                boxX += dx;
                boxY += dy;
            }

            boxes[y][x].setClueNumber(num);
        }

        return boxes;
    }

    private static String[] getRawClues(JSONObject json) throws JSONException {
        Map<Integer, String> acrossClues = new HashMap<>();
        Map<Integer, String> downClues = new HashMap<>();
        int maxClueNum = 0;

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            int num = entry.getInt("number");
            boolean across = entry.getString("direction").equals("across");
            String clue = entry.getString("clue");

            if (across)
                acrossClues.put(num, clue);
            else
                downClues.put(num, clue);

            maxClueNum = Math.max(maxClueNum, num);
        }

        String[] rawClues = new String[entries.length()];

        int cluePos = 0;
        for (int i = 0; i <= maxClueNum; i++) {
            if (acrossClues.containsKey(i))
                rawClues[cluePos++] = acrossClues.get(i);
            if (downClues.containsKey(i))
                rawClues[cluePos++] = downClues.get(i);
        }

        return rawClues;
    }
}
