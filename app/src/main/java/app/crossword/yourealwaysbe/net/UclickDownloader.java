package app.crossword.yourealwaysbe.net;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.logging.Level;

import app.crossword.yourealwaysbe.io.UclickXMLIO;
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
        super(prefix+shortName+"/data/", DOWNLOAD_DIR, fullName);
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

    public File download(LocalDate date) {
        File downloadTo = new File(this.downloadDirectory, this.createFileName(date));

        if (downloadTo.exists()) {
            return null;
        }

        File plainText = downloadToTempFile(date);

        if (plainText == null) {
            return null;
        }

        try {
        	LOG.log(Level.INFO, "TMP FILE "+plainText.getAbsolutePath());
            InputStream is = new FileInputStream(plainText);
            DataOutputStream os = new DataOutputStream(new FileOutputStream(downloadTo));   
            boolean retVal = UclickXMLIO.convertUclickPuzzle(is, os,
                    "\u00a9 " + date.getYear() + " " + copyright, date);
            os.close();
            is.close();
            plainText.delete();

            if (!retVal) {
                LOG.log(Level.SEVERE, "Unable to convert uclick XML puzzle into Across Lite format.");
                downloadTo.delete();
                downloadTo = null;
            }
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Exception converting uclick XML puzzle into Across Lite format.", ioe);
            downloadTo.delete();
            downloadTo = null;
        }

        return downloadTo;
    }

    @Override
    protected String createUrlSuffix(LocalDate date) {
        return this.shortName + nf.format(date.getYear() % 100) + nf.format(date.getMonthValue()) +
        nf.format(date.getDayOfMonth()) + "-data.xml";
    }

    private File downloadToTempFile(LocalDate date) {
        File f = new File(downloadDirectory, this.createFileName(date));

        try {
            URL url = new URL(this.baseUrl + this.createUrlSuffix(date));
            LOG.log(Level.INFO, this.fullName+" "+url.toExternalForm());
            AndroidVersionUtils.Factory.getInstance().downloadFile(url, f, EMPTY_MAP, false, null);
        } catch (Exception e) {
            e.printStackTrace();
            f = null;
        }

        if (f == null) {
            LOG.log(Level.SEVERE, "Unable to download uclick XML file.");

            return null;
        }

        try {
        	
            File tmpFile = new File(this.tempFolder, "uclick-temp"+System.currentTimeMillis()+".xml");
            f.renameTo(tmpFile);

            return tmpFile;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to move uclick XML file to temporary location.");

            return null;
        }
    }
}
