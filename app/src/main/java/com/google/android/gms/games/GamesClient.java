
package com.google.android.gms.games;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.GooglePlayServicesClient;

public class GamesClient {
    public static final String EXTRA_INVITATION = "extra_invitation";

    static public class Builder {
        public Builder(Context c,
                       GooglePlayServicesClient.ConnectionCallbacks x,
                       GooglePlayServicesClient.OnConnectionFailedListener y) {
            // nothing
        }

        public Builder setGravityForPopups(int gravity) { return this; }
        public Builder setScopes(String[] s) { return this; }
        public GamesClient create() { return new GamesClient(); }
        public GamesClient build() { return new GamesClient(); }
    }

    public void connect() { }
    public boolean isConnected() { return false; }
    public void signOut() { }
    public void disconnect() { }
    public void reconnect() { }
    public void unlockAchievement(String x) { }
    public void incrementAchievement(String x, int y) { }
    public Intent getAchievementsIntent() { return null; }
}
