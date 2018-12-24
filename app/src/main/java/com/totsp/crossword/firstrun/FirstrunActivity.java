package com.totsp.crossword.firstrun;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.totsp.crossword.BrowseActivity;
import com.totsp.crossword.GameHelper;
import com.totsp.crossword.shortyz.ShortyzApplication;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by rcooper on 6/27/15.
 */
public class FirstrunActivity extends AppIntro implements GameHelper.GameHelperListener {

    private static final Logger LOGGER = Logger.getLogger(FirstrunActivity.class.getCanonicalName());
    //
    // Copypasta from BaseGames Activity
    //

    // The game helper object. This class is mainly a wrapper around this object.
    protected GameHelper mHelper;

    // We expose these constants here because we don't want users of this class
    // to have to know about GameHelper at all.
    public static final int CLIENT_GAMES = GameHelper.CLIENT_GAMES;

    // Requested clients. By default, that's just the games client.
    protected int mRequestedClients = CLIENT_GAMES;

    protected String mDebugTag = "FirstrunActivity";
    protected boolean mDebugLog = false;

    GameHelper.GameHelperListener gameHelperListener;
    private SparseArray<PermissionCallback> requestedPermissions = new SparseArray<>();

    @Override
    public void init(Bundle bundle) {
        mHelper = new GameHelper(this);
        if (mDebugLog) {
            mHelper.enableDebugLog(true, mDebugTag);
        }
        mHelper.setup(this, mRequestedClients);
        addSlide(new Slide1(), this.getApplicationContext());
        setBarColor(Color.argb(100, 0, 0, 0));
    }

    @Override
    public void onSkipPressed() {
        ((ShortyzApplication) getApplication()).getSettings().edit().putBoolean("didFirstRun", true).apply();
        Intent i = new Intent(Intent.ACTION_VIEW, null, this, BrowseActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void onDonePressed() {
        ((ShortyzApplication) getApplication()).getSettings().edit().putBoolean("didFirstRun", true).apply();
        Intent i = new Intent(Intent.ACTION_VIEW, null, this, BrowseActivity.class);
        startActivity(i);
        finish();
    }

    public void setGameHelperListener(GameHelper.GameHelperListener listener){
        this.gameHelperListener = listener;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHelper.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHelper.onStop();
    }

    protected boolean isSignedIn() {
        return mHelper.isSignedIn();
    }

    protected void beginUserInitiatedSignIn() {
        mHelper.beginUserInitiatedSignIn();
    }

    @Override
    public void onSignInFailed() {
        //Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSignInSucceeded() {
        if(this.mHelper != null && !this.mHelper.getGamesClient().isConnected()){
            this.mHelper.getGamesClient().connect();
        }
        if(this.gameHelperListener != null){
            this.gameHelperListener.onSignInSucceeded();
        }
    }

    public void requestPermission(int requestCode, String permission, PermissionCallback callback){
        ActivityCompat.requestPermissions(this,
                new String[]{permission},
                requestCode);
        this.requestedPermissions.put(requestCode, callback);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        PermissionCallback callback = this.requestedPermissions.get(requestCode);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
           callback.success();
        } else {
            callback.fail();
        }
    }

    interface PermissionCallback {
        void success();
        void fail();
    }
}
