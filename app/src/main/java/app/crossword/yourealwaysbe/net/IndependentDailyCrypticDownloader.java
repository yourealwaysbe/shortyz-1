package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;

import android.net.Uri;

/**
 * Independent Daily Cryptic downloader
 * https://puzzles.independent.co.uk/games/webgl-cryptic-crossword-independent
 * Date = Daily
 */
public class IndependentDailyCrypticDownloader
        extends AbstractCrosswordCompilerDownloader {
    private static final String NAME = "The Independent's Cryptic Crossword";
    private static final String SUPPORT_URL
        = "https://www.independent.co.uk/donations";

    public IndependentDailyCrypticDownloader() {
        super(
            "https://ams.cdn.arkadiumhosted.com/assets/gamesfeed/independent/daily-crossword/",
            getStandardDownloadDir(),
            NAME,
            "The Independent"
        );
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_DAILY;
    }

    public String getName() {
        return NAME;
    }

    @Override
    public String getSupportUrl() {
        return SUPPORT_URL;
    }

    protected String createUrlSuffix(LocalDate date) {
        return String.format(Locale.US,
                             "c_%02d%02d%02d.xml",
                             date.getYear() % 100,
                             date.getMonthValue(),
                             date.getDayOfMonth());
    }
}
