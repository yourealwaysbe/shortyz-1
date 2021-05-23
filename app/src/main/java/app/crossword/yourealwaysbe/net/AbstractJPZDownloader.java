
package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;

import app.crossword.yourealwaysbe.io.JPZIO;

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

