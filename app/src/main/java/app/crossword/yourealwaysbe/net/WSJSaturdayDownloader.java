package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Created by rcooper on 9/28/15.
 */
public class WSJSaturdayDownloader extends WSJFridayDownloader {

    @Override
    public DayOfWeek[] getDownloadDates() {
        return DATE_SATURDAY;
    }

    public LocalDate getGoodFrom(){
        return LocalDate.of(115, 8, 19);
    }
}
