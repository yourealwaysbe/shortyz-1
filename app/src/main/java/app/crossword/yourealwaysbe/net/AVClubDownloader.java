package app.crossword.yourealwaysbe.net;

import app.crossword.yourealwaysbe.io.IO;

import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

/**
 * The Onion AV Club
 * URL: http://herbach.dnsalias.com/Tausig/avYYMMDD.puz
 * Date = Wednesdays
 */
public class AVClubDownloader extends AbstractDownloader {
    public static final String NAME = "The Onion AV Club";
    NumberFormat nf = NumberFormat.getInstance();

    protected AVClubDownloader() {
        super("http://herbach.dnsalias.com/Tausig/", getStandardDownloadDir(), NAME);
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public DayOfWeek[] getDownloadDates() {
        return DATE_WEDNESDAY;
    }

    public String getName() {
        return NAME;
    }

    @Override
    public Downloader.DownloadResult download(LocalDate date) {
        return this.download(date, this.createUrlSuffix(date));
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return "av" + this.nf.format(date.getYear() % 100) + this.nf.format(date.getMonthValue()) +
        this.nf.format(date.getDayOfMonth()) + ".puz";
    }

    @Override
    protected Downloader.DownloadResult download(
        LocalDate date, String urlSuffix
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();
        FileHandle f = null;
        boolean success = false;
        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            System.out.println(url);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Referer", this.baseUrl);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                f = fileHandler.createFileHandle(
                    downloadDirectory,
                    this.createFileName(date),
                    FileHandler.MIME_TYPE_PUZ
                );
                if (f == null)
                    return null;
                try (OutputStream fos = fileHandler.getOutputStream(f)) {
                    IO.copyStream(connection.getInputStream(), fos);
                }
                success = true;
                return new Downloader.DownloadResult(f);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!success && f != null)
                fileHandler.delete(f);
        }

        return null;
    }
}
