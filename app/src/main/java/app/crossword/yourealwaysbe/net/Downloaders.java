package app.crossword.yourealwaysbe.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import androidx.core.app.NotificationCompat;

import app.crossword.yourealwaysbe.BrowseActivity;
import app.crossword.yourealwaysbe.PlayActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;
import app.crossword.yourealwaysbe.util.files.DirHandle;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Downloaders {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    private Context context;
    private NotificationManager notificationManager;
    private boolean supressMessages;
    private SharedPreferences prefs;

    public Downloaders(SharedPreferences prefs,
                       NotificationManager notificationManager,
                       Context context) {
        this(prefs, notificationManager, context, true);
    }


    // Set isInteractive to true if this class can ask for user interaction when needed (e.g. to
    // refresh NYT credentials), false if otherwise.
    public Downloaders(SharedPreferences prefs,
                       NotificationManager notificationManager,
                       Context context,
                       boolean challengeForCredentials) {
        this.prefs = prefs;
        this.notificationManager = notificationManager;
        this.context = context;
        this.supressMessages = prefs.getBoolean("supressMessages", false);
    }

    public List<Downloader> getDownloaders(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<Downloader> retVal = new LinkedList<Downloader>();

        for (Downloader d : getDownloadersFromPrefs()) {
            // TODO: Downloader.getGoodThrough() should account for the day of week.
            if (Arrays.binarySearch(d.getDownloadDates(), dayOfWeek) >= 0) {
                LocalDate dGoodFrom = d.getGoodFrom();
                boolean isGoodFrom
                    = date.isEqual(dGoodFrom) || date.isAfter(dGoodFrom);
                LocalDate dGoodThrough = d.getGoodThrough();
                boolean isGoodThrough
                    = date.isBefore(dGoodThrough) || date.isEqual(dGoodThrough);

                if(isGoodFrom && isGoodThrough) {
                    retVal.add(d);
                }
            }
        }

        return retVal;
    }

    public void download(LocalDate date) {
        download(date, getDownloaders(date));
    }

    // Downloads the latest puzzles newer/equal to than the given date for the given set of
    // downloaders.
    //
    // If downloaders is null, then the full list of downloaders will be used.
    public void downloadLatestIfNewerThanDate(LocalDate oldestDate, List<Downloader> downloaders) {
        if (downloaders == null) {
            downloaders = new ArrayList<Downloader>();
        }

        if (downloaders.size() == 0) {
            downloaders.addAll(getDownloadersFromPrefs());
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            LocalDate goodThrough = d.getGoodThrough();
            DayOfWeek goodThroughDayOfWeek = goodThrough.getDayOfWeek();
            boolean isDay
                = Arrays.binarySearch(
                    d.getDownloadDates(), goodThroughDayOfWeek
                ) >= 0;
            boolean isGoodThrough
                = goodThrough.isEqual(oldestDate)
                    || goodThrough.isAfter(oldestDate);
            if (isDay && isGoodThrough) {
                LOG.info("Will try to download puzzle " + d + " @ " + goodThrough);
                puzzlesToDownload.put(d, goodThrough);
            }
        }

        if (!puzzlesToDownload.isEmpty()) {
            download(puzzlesToDownload);
        }
    }

    public void download(LocalDate date, List<Downloader> downloaders) {
        if ((downloaders == null) || (downloaders.size() == 0)) {
            downloaders = getDownloaders(date);
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            puzzlesToDownload.put(d, date);
        }

        download(puzzlesToDownload);
    }

    private void download(Map<Downloader, LocalDate> puzzlesToDownload) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        NotificationCompat.Builder not =
                new NotificationCompat.Builder(context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(context.getString(
                            R.string.puzzles_downloading
                        ))
                        .setWhen(System.currentTimeMillis());

        boolean somethingDownloaded = false;

        int nextNotificationId = 1;
        Set<String> fileNames = fileHandler.getPuzzleNames();

        for (
            Map.Entry<Downloader, LocalDate> puzzle
                : puzzlesToDownload.entrySet()
        ) {
            Downloader downloader = puzzle.getKey();
            LocalDate date = puzzle.getValue();

            String fileName = downloader.createFileName(date);

            if (downloader.alwaysRun() || !fileNames.contains(fileName)) {
                somethingDownloaded |= downloadPuzzle(
                    downloader,
                    puzzle.getValue(),
                    not,
                    nextNotificationId++
                );
            }
        }

        if (this.notificationManager != null) {
            this.notificationManager.cancel(0);
        }

        if (somethingDownloaded) {
            this.postDownloadedGeneral();
        }
    }

    /**
     * Download and save the puzzle from the downloader
     *
     * @return true if succeeded
     */
    private boolean downloadPuzzle(
        Downloader d,
        LocalDate date,
        NotificationCompat.Builder not,
        int notificationId
    ) {
        FileHandler fileHandler
            = ForkyzApplication.getInstance().getFileHandler();

        LOG.info("Downloading " + d.toString());

        try {
            String contentText = context.getString(
                R.string.puzzles_downloading_from, d.getName()
            );
            Intent notificationIntent = new Intent(context, PlayActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, notificationIntent, 0
            );

            not.setContentText(contentText)
                .setContentIntent(contentIntent);

            if (!this.supressMessages && this.notificationManager != null) {
                this.notificationManager.notify(0, not.build());
            }

            Puzzle puz = d.download(date);

            if (puz == null)
                return false;

            boolean saved = fileHandler.saveNewPuzzle(
                puz, d.createFileName(date)
            );

            if (saved) {
                if (!this.supressMessages) {
                    this.postDownloadedNotification(
                        notificationId, d.getName()
                    );
                }
                return true;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to download "+d.getName(), e);
        }
        return false;
    }

    public void supressMessages(boolean b) {
        this.supressMessages = b;
    }

    private void postDownloadedGeneral() {
        Intent notificationIntent = new Intent(Intent.ACTION_EDIT, null,
                context, BrowseActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        Notification not = new NotificationCompat.Builder(context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.puzzles_downloaded))
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis())
                .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(0, not);
        }
    }

    private void postDownloadedNotification(int i, String name) {
        Intent notificationIntent = new Intent(Intent.ACTION_EDIT, null,
                context, BrowseActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        Notification not
            = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(contentIntent)
            .setContentTitle(context.getString(
                R.string.puzzle_downloaded, name
            ))
            .setWhen(System.currentTimeMillis())
            .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(i, not);
        }
    }

    private List<Downloader> getDownloadersFromPrefs() {
        List<Downloader> downloaders = new LinkedList<>();

        if (prefs.getBoolean("downloadGuardianDailyCryptic", true)) {
            downloaders.add(new GuardianDailyCrypticDownloader());
        }

        if (prefs.getBoolean("downloadIndependentDailyCryptic", true)) {
            downloaders.add(new IndependentDailyCrypticDownloader());
        }

        if (prefs.getBoolean("downloadWsj", true)) {
            downloaders.add(new WSJFridayDownloader());
            downloaders.add(new WSJSaturdayDownloader());
        }

        if (prefs.getBoolean("downloadJonesin", true)) {
            downloaders.add(new JonesinDownloader());
        }

        if (prefs.getBoolean("downloadLat", true)) {
            downloaders.add(new UclickDownloader(
                "tmcal",
                "Los Angeles Times",
                "Rich Norris",
                "https://www.latimes.com/subscriptions/digital.html",
                Downloader.DATE_NO_SUNDAY
            ));
        }

        if (prefs.getBoolean("downloadNewsday", true)) {
            downloaders.add(new BrainsOnlyDownloader(
                "https://brainsonly.com/servlets-newsday-crossword/newsdaycrossword?date=",
                "Newsday",
                // i can't browse this site for a more specific URL
                // (GDPR)
                "https://www.newsday.com"
            ));
        }

        if (prefs.getBoolean("downloadUSAToday", true)) {
            downloaders.add(new UclickDownloader(
                "usaon",
                "USA Today",
                "USA Today",
                "https://subscribe.usatoday.com",
                Downloader.DATE_NO_SUNDAY
            ));
        }

        if (prefs.getBoolean("downloadUniversal", true)) {
            downloaders.add(new UclickDownloader(
                "fcx",
                "Universal Crossword",
                "uclick LLC",
                "http://www.uclick.com/client/spi/fcx/",
                Downloader.DATE_DAILY
            ));
        }

        return downloaders;
    }
}
