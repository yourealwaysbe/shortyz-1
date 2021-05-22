package app.crossword.yourealwaysbe.net;

import java.time.DayOfWeek;
import java.time.LocalDate;

import android.content.Context;

import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

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
    DayOfWeek[] DATE_WEEKDAY = new DayOfWeek[] {
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    };

    public static class DownloadResult {
        private FileHandle fileHandle;

        public DownloadResult(FileHandle fileHandle) {
            this.fileHandle = fileHandle;
        }

        public FileHandle getFileHandle() { return fileHandle; }
    }

    void setContext(Context context);

    DayOfWeek[] getDownloadDates();

    DirHandle getDownloadDir();

    String getName();

    /**
     * Returns a URL where the user may support the crossword source
     *
     * @return null if no reasonable support/source URL (discouraged)
     */
    String getSupportUrl();

    String createFileName(LocalDate date);

    DownloadResult download(LocalDate date);

    String sourceUrl(LocalDate date);

    boolean alwaysRun();

    LocalDate getGoodThrough();

    LocalDate getGoodFrom();
}
