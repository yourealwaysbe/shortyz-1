package app.crossword.yourealwaysbe.net;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;

import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * Wall Street Journal
 * URL: https://mazerlm.home.comcast.net/~mazerlm/wsjYYMMDD.puz
 * Date = Fridays
 */
public class WSJFridayDownloader extends AbstractDownloader {
    private static final String NAME = "Wall Street Journal";
    private static final String SUPPORT_URL = "https://subscribe.wsj.com";
    NumberFormat nf = NumberFormat.getInstance();

    public WSJFridayDownloader() {
        super("https://herbach.dnsalias.com/wsj/", getStandardDownloadDir(), NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_FRIDAY;
    }

    public String getName() {
        return NAME;
    }

    @Override
    public String getSupportUrl() {
        return SUPPORT_URL;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        return super.download(date, this.createUrlSuffix(date));
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "wsj" + nf.format(date.getYear() % 100) + nf.format(date.getMonthValue()) + nf.format(date.getDayOfMonth()) +
        FileHandler.FILE_EXT_PUZ;
    }


}
