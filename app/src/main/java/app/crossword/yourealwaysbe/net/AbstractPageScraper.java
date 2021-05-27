package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

public class AbstractPageScraper {
    private static final String REGEX = "http://[^ ^']*\\.puz";
    private static final String REL_REGEX = "href=\"(.*\\.puz)\"";
    private static final Pattern PAT = Pattern.compile(REGEX);
    private static final Pattern REL_PAT = Pattern.compile(REL_REGEX);
    private String sourceName;
    private String url;
    private String supportUrl;
    protected boolean updateable = false;

    protected AbstractPageScraper(
        String url, String sourceName, String supportUrl
    ) {
        this.url = url;
        this.sourceName = sourceName;
        this.supportUrl = supportUrl;
    }

    protected String getContent() throws IOException {
        URL u = new URL(url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new BufferedInputStream(u.openStream())) {
            IO.copyStream(is, baos);
        }
        return new String(baos.toByteArray());
    }

    public static Puzzle download(String url) throws IOException {
        URL u = new URL(url);

        try (InputStream is = new BufferedInputStream(u.openStream())) {
            return IO.loadNative(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Map URLs to names of file at url, with file extension removed
     */
    protected static Map<String, String> mapURLsToFileNames(
        List<String> urls
    ) {
        HashMap<String, String> result = new HashMap<String, String>(
                urls.size());

        for (String url : urls) {
            String fileName = url;
            int lastSlashIdx = fileName.lastIndexOf("/");
            if (lastSlashIdx > 0)
                 fileName = fileName.substring(lastSlashIdx + 1);
            int extensionIdx = fileName.lastIndexOf(".");
            if (extensionIdx > 0)
                fileName = fileName.substring(0, extensionIdx);
            result.put(url, fileName);
        }

        return result;
    }

    protected static List<String> puzzleRelativeURLs(String baseUrl, String input)
            throws MalformedURLException {
        URL base = new URL(baseUrl);
        ArrayList<String> result = new ArrayList<String>();
        Matcher matcher = REL_PAT.matcher(input);

        while (matcher.find()) {
            result.add(new URL(base, matcher.group(1)).toString());
        }

        return result;
    }

    protected static List<String> puzzleURLs(String input) {
        ArrayList<String> result = new ArrayList<String>();
        Matcher matcher = PAT.matcher(input);

        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    public String getSourceName() {
        return this.sourceName;
    }

    public String getSupportUrl() {
        return this.supportUrl;
    }

    /**
     * Add some meta data to file and save it to the file system
     */
    private boolean processPuzzle(
        Puzzle puz, String fileName, String sourceUrl
    ) {
        final FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        try {
            // I'm not sure what purpose this has
            // Doesn't seem to be changeable from UI
            puz.setUpdatable(false);
            puz.setSource(this.sourceName);
            puz.setSourceUrl(sourceUrl);
            puz.setSupportUrl(this.supportUrl);
            puz.setDate(LocalDate.now());

            return fileHandler.saveNewPuzzle(puz, fileName) != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns a list of file names downloaded
     */
    public List<String> scrape() {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        ArrayList<String> scrapedFiles = new ArrayList<>();

        try {
            String content = this.getContent();
            List<String> urls = puzzleURLs(content);

            try {
                urls.addAll(puzzleRelativeURLs(url, content));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Map<String, String> urlsToFilenames = mapURLsToFileNames(urls);

            Set<String> existingFiles = fileHandler.getPuzzleNames();

            for (String url : urls) {
                String filename = urlsToFilenames.get(url);

                boolean exists = existingFiles.contains(filename);

                if (!exists && (scrapedFiles.size() < 3)) {
                    try {
                        Puzzle puz = download(url);
                        if (puz != null) {
                            if (this.processPuzzle(puz, filename, url))
                                scrapedFiles.add(filename);
                        }
                    } catch (Exception e) {
                        System.err.println("Exception downloading " + url
                                + " for " + this.sourceName);
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return scrapedFiles;
    }
}
