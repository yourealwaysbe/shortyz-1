package app.crossword.yourealwaysbe.net;

import app.crossword.yourealwaysbe.io.BrainsOnlyIO;
import app.crossword.yourealwaysbe.puz.Puzzle;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Created by keber_000 on 2/9/14.
 */
public class BrainsOnlyDownloader extends AbstractDownloader {

    private final DateTimeFormatter df
        = DateTimeFormatter.ofPattern("yyyyMMdd");

    public BrainsOnlyDownloader(
        String baseUrl, String fullName, String supportUrl
    ) {
        super(
            baseUrl,
            fullName,
            Downloader.DATE_DAILY,
            supportUrl,
            new BrainsOnlyIO());
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return df.format(date);
    }
}
