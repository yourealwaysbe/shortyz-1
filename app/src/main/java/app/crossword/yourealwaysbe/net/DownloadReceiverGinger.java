package app.crossword.yourealwaysbe.net;

import android.annotation.TargetApi;
import android.app.DownloadManager.Query;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.util.files.FileHandler;

public class DownloadReceiverGinger extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        DownloadManager mgr = (DownloadManager) ctx
                .getSystemService(Context.DOWNLOAD_SERVICE);
        long id = intent.getLongExtra("extra_download_id", -1);

        if(!FileHandler.MIME_TYPE_PUZ.equals(
            mgr.getMimeTypeForDownloadedFile(id))) {
            return;
        }
        Uri uri = null;
        Cursor c = null;
        try {
            Query q = new Query();
            q.setFilterById(id);
            q.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);

            c = mgr.query(q);

            c.moveToFirst();
            String uriString = c.getString(c
                    .getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            uri = Uri.parse(uriString);
            c.close();
            if (uri == null || !uri.toString().endsWith(FileHandler.FILE_EXT_PUZ)) {
                return;
            }
        } catch (CursorIndexOutOfBoundsException e) {
            c.close();
            return;
        }
        System.out.println("===RECEIVED: " + uri);

        try {
            FileHandler fileHandler
                = ForkyzApplication.getInstance().getFileHandler();

            DownloadReceiver.Metas metas = DownloadReceiver.metas.remove(uri);

            Downloaders.processDownloadedPuzzle(
                metas.getParentDir(),
                fileHandler.getFileHandle(uri),
                metas.getPuzMeta()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}