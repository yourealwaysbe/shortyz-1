package app.crossword.yourealwaysbe.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.core.app.NotificationCompat;

import app.crossword.yourealwaysbe.PlayActivity;
import app.crossword.yourealwaysbe.BrowseActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.util.files.FileHandle;
import app.crossword.yourealwaysbe.util.files.FileHandler;

import java.util.ArrayList;
import java.util.List;


public class Scrapers {
    private ArrayList<AbstractPageScraper> scrapers = new ArrayList<AbstractPageScraper>();
    private Context context;
    private NotificationManager notificationManager;
    private boolean supressMessages;

    public Scrapers(SharedPreferences prefs, NotificationManager notificationManager, Context context) {
        this.notificationManager = notificationManager;
        this.context = context;

        if (prefs.getBoolean("scrapeCru", false)) {
            scrapers.add(new CruScraper());
        }

        if (prefs.getBoolean("scrapeKegler", false)) {
            scrapers.add(new KeglerScraper());
        }

        this.supressMessages = prefs.getBoolean("supressMessages", false);
    }

    public void scrape() {
        int i = 1;
        String contentTitle = context.getString(R.string.puzzles_scraping);

        NotificationCompat.Builder not
            = new NotificationCompat.Builder(
                    context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
                )
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(contentTitle)
                .setWhen(System.currentTimeMillis());

        for (AbstractPageScraper scraper : scrapers) {
            try {
                String contentText = context.getString(
                    R.string.puzzles_downloading_from, scraper.getSourceName()
                );
                Intent notificationIntent
                    = new Intent(context, PlayActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent, 0
                );
                not.setContentText(contentText).setContentIntent(contentIntent);

                if (!this.supressMessages && this.notificationManager != null) {
                    this.notificationManager.notify(0, not.build());
                }

                List<String> downloaded = scraper.scrape();

                if (!this.supressMessages) {
                    for (String name : downloaded) {
                        postDownloadedNotification(i++, name);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (this.notificationManager != null) {
            this.notificationManager.cancel(0);
        }
    }

    public void supressMessages(boolean b) {
        this.supressMessages = b;
    }

    private void postDownloadedNotification(int i, String name) {
        Intent notificationIntent = new Intent(
            Intent.ACTION_EDIT, null, context, BrowseActivity.class
        );
        PendingIntent contentIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, 0
        );

        Notification not = new NotificationCompat.Builder(
                context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
            )
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(
                R.string.puzzle_downloaded, name
            ))
            .setContentIntent(contentIntent)
            .setWhen(System.currentTimeMillis())
            .build();

        if (this.notificationManager != null) {
            this.notificationManager.notify(i, not);
        }
    }
}
