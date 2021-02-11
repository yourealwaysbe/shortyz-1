package app.crossword.yourealwaysbe.net;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;

import android.content.Context;


public interface Downloader {
    // These lists must be sorted for binary search.
    DayOfWeek[] DATE_SUNDAY = new DayOfWeek[] { DayOfWeek.SUNDAY };
    DayOfWeek[] DATE_MONDAY = new DayOfWeek[] { DayOfWeek.MONDAY };
    DayOfWeek[] DATE_TUESDAY = new DayOfWeek[] { DayOfWeek.TUESDAY };
    DayOfWeek[] DATE_WEDNESDAY = new DayOfWeek[] { DayOfWeek.WEDNESDAY };
    DayOfWeek[] DATE_THURSDAY = new DayOfWeek[] { DayOfWeek.THURSDAY };
    DayOfWeek[] DATE_FRIDAY = new DayOfWeek[] { DayOfWeek.FRIDAY };
    DayOfWeek[] DATE_SATURDAY = new DayOfWeek[] { DayOfWeek.SATURDAY };
    DayOfWeek[] DATE_DAILY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    };
    DayOfWeek[] DATE_NO_SUNDAY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    };

    File DEFERRED_FILE = new File(".");

    void setContext(Context context);

    DayOfWeek[] getDownloadDates();

    String getName();

    String createFileName(LocalDate date);

    File download(LocalDate date);

    String sourceUrl(LocalDate date);

    boolean alwaysRun();


    LocalDate getGoodThrough();

    LocalDate getGoodFrom();
}
