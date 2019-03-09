
package com.google.android.gms.auth;

import java.lang.RuntimeException;

import android.content.Intent;

// Use of RuntimeException is a hack to avoid javac complaining that a
// UserRecoverableAuthException is never thrown by an Oauth credential
// getToken call.
public class UserRecoverableAuthException extends RuntimeException {

    public static Intent getIntent() { return null; }
}
