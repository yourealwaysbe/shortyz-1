package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;

@TargetApi(Build.VERSION_CODES.O)
public class OreoUtil extends LollipopUtil {

    @Override
    public void createNotificationChannel(Context context) {
        Log.i(OreoUtil.class.getSimpleName(), "Creating notification channel");
        CharSequence name
            = context.getString(R.string.download_notification_channel_name);
        String description
            = context.getString(R.string.download_notification_channel_desc);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
                , name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

    }
}
