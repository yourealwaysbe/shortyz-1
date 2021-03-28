package app.crossword.yourealwaysbe.net;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.BrainsOnlyIO;
import app.crossword.yourealwaysbe.io.KingFeaturesPlaintextIO;
import app.crossword.yourealwaysbe.io.UclickXMLIO;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Created by keber_000 on 2/9/14.
 */
public class BrainsOnlyDownloader extends AbstractDownloader {

    private final DateTimeFormatter df
        = DateTimeFormatter.ofPattern("yyMMdd");
    private final String fullName;

    public BrainsOnlyDownloader(String baseUrl, String fullName) {
        super(baseUrl, getStandardDownloadDir(), fullName);
        this.fullName = fullName;
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return df.format(date);
    }

    public DayOfWeek[] getDownloadDates() {
        return Downloader.DATE_DAILY;
    }

    public String getName() {
        return this.fullName;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        String fileName = this.createFileName(date);

        FileHandle downloadTo = null;
        FileHandle plainText = null;
        boolean success = false;

        try {
            plainText = downloadToTempFile(this.getName(), date);
            if (plainText == null)
                return null;

            downloadTo = fileHandler.createFileHandle(
                this.downloadDirectory, fileName, FileHandler.MIME_TYPE_PUZ
            );
            if (downloadTo == null)
                return null;

            boolean converted = false;

            try (
                InputStream is = fileHandler.getBufferedInputStream(plainText);
                DataOutputStream os = new DataOutputStream(
                    fileHandler.getBufferedOutputStream(downloadTo)
                );
            ) {
                converted = BrainsOnlyIO.convertBrainsOnly(is, os, date);
            } catch (Exception ioe) {
                LOG.log(
                    Level.SEVERE,
                    "Exception converting KFS puzzle into Across Lite format.",
                    ioe
                );
            }

            if (!converted) {
                LOG.log(Level.SEVERE, "Unable to convert KFS puzzle into Across Lite format.");
            } else {
                success = true;
            }
        } finally {
            if (!success && downloadTo != null)
                fileHandler.delete(downloadTo);
            if (plainText != null)
                fileHandler.delete(plainText);
        }

        return success ? new Downloader.DownloadResult(downloadTo) : null;
    }

}
