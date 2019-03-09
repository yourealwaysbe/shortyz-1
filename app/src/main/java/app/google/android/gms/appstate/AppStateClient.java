
package com.google.android.gms.appstate;

import android.content.Context;
import com.google.android.gms.common.GooglePlayServicesClient;

public class AppStateClient {
    static public class Builder {
        public Builder(Context c,
                       GooglePlayServicesClient.ConnectionCallbacks x,
                       GooglePlayServicesClient.OnConnectionFailedListener y) {
            // nothing
        }

        public Builder setGravityForPopups(int gravity) { return this; }
        public Builder setScopes(String[] s) { return this; }
        public AppStateClient build() { return new AppStateClient(); }
        public AppStateClient create() { return new AppStateClient(); }
    }
    public void connect() { }
    public boolean isConnected() { return false; }
    public void disconnect() { }
    public void reconnect() { }
}
