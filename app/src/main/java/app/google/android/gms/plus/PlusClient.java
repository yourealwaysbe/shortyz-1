
package com.google.android.gms.plus;

import android.content.Context;
import com.google.android.gms.common.GooglePlayServicesClient;

public class PlusClient {
    static public class Builder {
        public Builder(Context c,
                       GooglePlayServicesClient.ConnectionCallbacks x,
                       GooglePlayServicesClient.OnConnectionFailedListener y) {
            // nothing
        }

        public Builder setGravityForPopups(int gravity) { return this; }
        public Builder setScopes(String[] s) { return this; }
        public PlusClient build() { return new PlusClient(); }
        public PlusClient create() { return new PlusClient(); }
    }
    public void connect() { }
    public boolean isConnected() { return false; }
    public void clearDefaultAccount() { }
    public void disconnect() { }
}
