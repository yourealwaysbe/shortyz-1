
package app.crossword.yourealwaysbe.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Level;

import android.net.Uri;

import app.crossword.yourealwaysbe.io.JPZIO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.files.DirHandle;

/**
 * Abstract for puzzle sources using .JPZ XML format
 */
public abstract class AbstractJPZDownloader extends AbstractDownloader {
    public AbstractJPZDownloader(
        String baseUrl,
        String downloaderName,
        DayOfWeek[] days,
        String supportUrl
    ) {
        super(
            baseUrl,
            downloaderName,
            days,
            supportUrl,
            new JPZIO()
        );
    }
}

