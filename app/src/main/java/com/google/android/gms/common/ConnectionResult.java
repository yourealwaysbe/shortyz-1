
package com.google.android.gms.common;

import android.app.Activity;
import android.content.IntentSender.SendIntentException;

public class ConnectionResult {
    public static final int NOT_SUCCESS = -1;
    public static final int SUCCESS = 0;
    public static final int DEVELOPER_ERROR = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final int INVALID_ACCOUNT = 3;
    public static final int LICENSE_CHECK_FAILED = 4;
    public static final int NETWORK_ERROR = 5;
    public static final int RESOLUTION_REQUIRED = 6;
    public static final int SERVICE_DISABLED = 7;
    public static final int SERVICE_INVALID = 8;
    public static final int SERVICE_MISSING = 9;
    public static final int SERVICE_VERSION_UPDATE_REQUIRED = 10;
    public static final int SIGN_IN_REQUIRED = 11;

    public int getErrorCode() { return -1; }
    public boolean hasResolution() { return false; }
    public void startResolutionForResult(Activity x, int y) throws SendIntentException { }
}
