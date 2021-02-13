package app.crossword.yourealwaysbe.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.content.Context;
import android.net.Uri;

import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.versions.DefaultUtil;


public abstract class AbstractDownloader implements Downloader {
    protected static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    public static File DOWNLOAD_DIR = ForkyzApplication.CROSSWORDS;
	protected static final Map<String, String> EMPTY_MAP = Collections.emptyMap();
    protected File downloadDirectory;
    protected String baseUrl;
    protected final AndroidVersionUtils utils = AndroidVersionUtils.Factory.getInstance();
    private String downloaderName;
    protected File tempFolder;
    protected LocalDate goodThrough = LocalDate.now();

    protected AbstractDownloader(String baseUrl, File downloadDirectory, String downloaderName) {
        this.baseUrl = baseUrl;
        this.downloadDirectory = downloadDirectory;
        this.downloaderName = downloaderName;
        this.tempFolder = new File(downloadDirectory, "temp");
        this.tempFolder.mkdirs();
    }

    public void setContext(Context ctx) {
        this.utils.setContext(ctx);
    }



    public String createFileName(LocalDate date) {
        return (
            date.getYear() + "-" +
            date.getMonthValue() + "-" +
            date.getDayOfMonth() + "-" +
            this.downloaderName.replaceAll(" ", "") + ".puz"
        );
    }

    public String sourceUrl(LocalDate date) {
        return this.baseUrl + this.createUrlSuffix(date);
    }

    public String toString() {
        return getName();
    }

    protected abstract String createUrlSuffix(LocalDate date);

    protected File download(LocalDate date, String urlSuffix, Map<String, String> headers){
    	System.out.println("DL From ASD");
    	return download(date, urlSuffix, headers, true);
    }
    
    protected File download(LocalDate date, String urlSuffix, Map<String, String> headers, boolean canDefer) {
        LOG.info("Mkdirs: " + this.downloadDirectory.mkdirs());
        LOG.info("Exist: " + this.downloadDirectory.exists());

        try {
            URL url = new URL(this.baseUrl + urlSuffix);
            System.out.println(url);

            File f = new File(downloadDirectory, this.createFileName(date));
            PuzzleMeta meta = new PuzzleMeta();
            meta.date = date;
            meta.source = getName();
            meta.sourceUrl = url.toString();
            meta.updatable = false;
            
            utils.storeMetas(Uri.fromFile(f), meta);
            if( canDefer ){
	            if (utils.downloadFile(url, f, headers, true, this.getName())) {
	                DownloadReceiver.metas.remove(Uri.fromFile(f));
	
	                return f;
	            } else {
	                return Downloader.DEFERRED_FILE;
	            }
            } else {
            	AndroidVersionUtils.Factory.getInstance().downloadFile(url, f, headers, true, this.getName());
            	return f;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected File download(LocalDate date, String urlSuffix) {
        return download(date, urlSuffix, EMPTY_MAP);
    }


    protected File downloadToTempFile(String fullName, LocalDate date) {
        File downloaded = new File(downloadDirectory, this.createFileName(date));

        try {
            URL url = new URL(this.baseUrl + this.createUrlSuffix(date));
            LOG.log(Level.INFO, fullName +" "+url.toExternalForm());
            AndroidVersionUtils.Factory.getInstance().downloadFile(url, downloaded, EMPTY_MAP, false, null);
        } catch (Exception e) {
            e.printStackTrace();
            downloaded.delete();
            downloaded = null;
        }

        if (downloaded == null) {
            LOG.log(Level.SEVERE, "Unable to download plain text KFS file.");

            return null;
        }

        System.out.println("Text file: " + downloaded);

        try {
            File tmpFile =  new File(this.tempFolder, "txt-tmp"+System.currentTimeMillis()+".txt"); //File.createTempFile("kfs-temp", "txt");
            downloaded.renameTo(tmpFile);
            LOG.log(Level.INFO, "Downloaded to text file "+tmpFile);
            return tmpFile;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to move KFS file to temporary location.");

            return null;
        }
    }

    @Override
    public boolean alwaysRun(){
        return false;
    }

    public LocalDate getGoodThrough(){
        return this.goodThrough;
    }

    public LocalDate getGoodFrom(){
        return LocalDate.ofEpochDay(0L);
    }
}
