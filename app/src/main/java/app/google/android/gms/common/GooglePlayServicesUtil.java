
package com.google.android.gms.common;

import android.app.Dialog;
import android.app.Activity;
import android.content.Context;

public class GooglePlayServicesUtil {
    public static int isGooglePlayServicesAvailable() {
        return ConnectionResult.NOT_SUCCESS;
    }

    public static int isGooglePlayServicesAvailable(Context c) {
        return ConnectionResult.NOT_SUCCESS;
    }

    public static Dialog getErrorDialog(int code,
                                        Activity a,
                                        int x,
                                        Dialog.OnCancelListener l) {
        return null;
    }

    public static Dialog getErrorDialog(int code,
                                        Activity a,
                                        int x) {
        return null;
    }

    public static boolean isUserRecoverableError(int x) { return false; }
}
