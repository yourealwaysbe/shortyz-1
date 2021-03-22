package app.crossword.yourealwaysbe.service;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.net.Downloaders;

import java.time.LocalDate;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// Currently only available on API version >=21 due to use of JobScheduler.
// It may be possible to implement this functionality using AlarmManager for lower SDK versions.
@TargetApi(21)
public class BackgroundDownloadService extends JobService {
    public static final String DOWNLOAD_PENDING_PREFERENCE = "backgroundDlPending";

    private static final Logger LOGGER =
            Logger.getLogger(BackgroundDownloadService.class.getCanonicalName());

    private static JobInfo getJobInfo(boolean requireUnmetered, boolean allowRoaming,
                                      boolean requireCharging) {
        JobInfo.Builder builder = new JobInfo.Builder(
                JobSchedulerId.BACKGROUND_DOWNLOAD.id(),
                new ComponentName("app.crossword.yourealwaysbe.forkyz",
                        BackgroundDownloadService.class.getName()));

        builder.setPeriodic(TimeUnit.HOURS.toMillis(1))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresCharging(requireCharging)
                .setPersisted(true);

        setRequiredNetworkType(builder, requireUnmetered, allowRoaming);

        return builder.build();
    }

    @Override
    public boolean onStartJob(JobParameters job) {
        LOGGER.info("Starting background download task");
        DownloadTask downloadTask = new DownloadTask(this);
        downloadTask.executeAsync(job);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    public static void updateJob(Context context) {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        boolean enable = preferences.getBoolean("backgroundDownload", false);

        if (enable) {
            scheduleJob(context);
        } else {
            cancelJob(context);
        }
    }

    @SuppressWarnings("InlinedApi")
    private static void setRequiredNetworkType(
        JobInfo.Builder builder, boolean requireUnmetered, boolean allowRoaming
    ) {
        boolean versionSupportsRoaming
            = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        if (!requireUnmetered) {
            if (allowRoaming || !versionSupportsRoaming) {
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            } else {
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NOT_ROAMING);
            }
        }
    }

    private static void scheduleJob(Context context) {
        JobScheduler scheduler =
                (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        JobInfo info = getJobInfo(
                preferences.getBoolean("backgroundDownloadRequireUnmetered", true),
                preferences.getBoolean("backgroundDownloadAllowRoaming", false),
                preferences.getBoolean("backgroundDownloadRequireCharging", false));


        LOGGER.info("Scheduling background download job: " + info);

        int result = scheduler.schedule(info);

        if (result == JobScheduler.RESULT_SUCCESS) {
            LOGGER.info("Successfully scheduled background downloads");
        } else {
            LOGGER.log(Level.WARNING, "Unable to schedule background downloads");
        }
    }

    private static void cancelJob(Context context) {
        LOGGER.info("Unscheduling background downloads");
        JobScheduler scheduler =
                (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.cancel(JobSchedulerId.BACKGROUND_DOWNLOAD.id());
    }

    private static class DownloadTask {
        private final Executor executor = Executors.newSingleThreadExecutor(); // change according to your requirements
        private final Handler handler = new Handler(Looper.getMainLooper());

        private final JobService jobService;

        public DownloadTask(JobService jobService) {
            this.jobService = jobService;
        }

        public void executeAsync(JobParameters params) {
            executor.execute(() -> { doInBackground(); });
            handler.post(() -> {
                jobService.jobFinished(params, false);
            });
        }

        private void doInBackground() {
            Context context = jobService.getApplicationContext();

            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (ForkyzApplication.getInstance().isMissingWritePermission()) {
                LOGGER.info("Skipping download, no write permission");
                return;
            }

            LOGGER.info("Downloading most recent puzzles");
            if(Looper.myLooper() == null) {
                Looper.prepare();
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final Downloaders dls = new Downloaders(prefs, nm, context, false);
            dls.downloadLatestIfNewerThanDate(LocalDate.now(), null);

            // This is used to tell BrowseActivity that puzzles may have been updated while
            // paused.
            prefs.edit()
                .putBoolean(DOWNLOAD_PENDING_PREFERENCE, true)
                .apply();
        }
    }
}
