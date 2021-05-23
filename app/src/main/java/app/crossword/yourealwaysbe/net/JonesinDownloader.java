package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.LocalDate;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Jonesin' Crosswords Downloader
 * URL: https://herbach.dnsalias.com/Jonesin/jzYYMMDD.puz
 * Date = Thursdays
 */
public class JonesinDownloader extends AbstractDownloader {
    private static final String NAME = "Jonesin' Crosswords";
    private static final String SUPPORT_URL = "https://crosswordnexus.com/jonesin/";
    NumberFormat nf = NumberFormat.getInstance();

    public JonesinDownloader() {
        super(
            "https://herbach.dnsalias.com/Jonesin/",
            NAME,
            DATE_THURSDAY,
            SUPPORT_URL,
            new IO()
        );
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "jz" + (date.getYear() % 100) + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) + FileHandler.FILE_EXT_PUZ;
    }
}
