package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.logging.Level;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.io.UclickXMLIO;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.versions.DefaultUtil;

/**
 * Uclick XML Puzzles
 * URL: http://picayune.uclick.com/comics/[puzzle]/data/[puzzle]YYMMDD-data.xml
 * crnet (Newsday) = Daily
 * usaon (USA Today) = Monday-Saturday (not holidays)
 * fcx (Universal) = Daily
 * lacal (LA Times Sunday Calendar) = Sunday
 */
public class UclickDownloader extends AbstractDownloader {
    DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
    NumberFormat nf = NumberFormat.getInstance();
    private String copyright;
    private String fullName;
    private String shortName;
    private DayOfWeek[] days;

    public UclickDownloader(String prefix, String shortName, String fullName, String copyright, DayOfWeek[] days){
        super(prefix+shortName+"/data/", getStandardDownloadDir(), fullName);
        this.shortName = shortName;
        this.fullName = fullName;
        this.copyright = copyright;
        this.days = days;
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(0);
    }

    public UclickDownloader(String shortName, String fullName, String copyright, DayOfWeek[] days) {
        this("http://picayune.uclick.com/comics/",shortName, fullName, copyright, days);

    }

    public DayOfWeek[] getDownloadDates() {
        return days;
    }

    public String getName() {
        return fullName;
    }

    public Downloader.DownloadResult download(LocalDate date) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        FileHandle plainText = null;
        FileHandle downloadTo = null;
        boolean success = true;

        try {
            plainText = downloadToTempFile(date);
            if (plainText == null)
                return null;

            String fileName = this.createFileName(date);

            downloadTo = fileHandler.createFileHandle(
                this.downloadDirectory,
                this.createFileName(date),
                FileHandler.MIME_TYPE_PUZ
            );
            if (downloadTo == null)
                return null;

            try (
                InputStream is = fileHandler.getInputStream(plainText);
                DataOutputStream os = new DataOutputStream(
                    fileHandler.getOutputStream(downloadTo)
                );
            ) {
                boolean converted = UclickXMLIO.convertUclickPuzzle(
                    is, os, "\u00a9 " + date.getYear() + " " + copyright, date
                );
                os.close();
                is.close();

                if (!converted) {
                    LOG.log(
                        Level.SEVERE,
                        "Unable to convert uclick XML puzzle into Across Lite format."
                    );
                } else {
                    success = true;
                }
            } catch (IOException ioe) {
                LOG.log(
                    Level.SEVERE,
                    "Exception converting uclick XML puzzle into Across Lite format.",
                    ioe
                );
            }
        } finally {
            if (plainText != null)
                fileHandler.delete(plainText);
            if (!success && downloadTo != null)
                fileHandler.delete(downloadTo);
        }


        return success ? new Downloader.DownloadResult(downloadTo) : null;
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return this.shortName + nf.format(date.getYear() % 100) + nf.format(date.getMonthValue()) +
        nf.format(date.getDayOfMonth()) + "-data.xml";
    }

    private FileHandle downloadToTempFile(LocalDate date) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        FileHandle tmpFile = null;
        boolean success = false;

        try {
            tmpFile = fileHandler.createFileHandle(
                this.tempFolder,
                "uclick-temp"+System.currentTimeMillis()+".xml",
                FileHandler.MIME_TYPE_GENERIC_XML
            );
            if (tmpFile == null) {
                LOG.log(Level.SEVERE, "Unable to download uclick XML file.");
                return null;
            }

            URL url = new URL(this.baseUrl + this.createUrlSuffix(date));
            success = AndroidVersionUtils.Factory.getInstance().downloadFile(
                url, tmpFile, EMPTY_MAP, false, null
            );
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Unable to download uclick XML file.");
        } finally {
            if (!success && tmpFile != null)
                fileHandler.delete(tmpFile);
        }

        return success ? tmpFile : null;
    }
}
