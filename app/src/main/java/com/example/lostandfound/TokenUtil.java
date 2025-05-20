package com.example.lostandfound;


import android.content.Context;
import android.util.Log;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

public class TokenUtil {

    private static final String TAG = "TokenUtil";

    public interface AccessTokenCallback {
        void onTokenReceived(String token);

        void onError(Exception e);
    }

    public static void fetchAccessToken(Context context, AccessTokenCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream serviceAccount = context.getAssets().open("service_account_key.json");

                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount)
                        .createScoped("https://www.googleapis.com/auth/firebase.messaging");
                credentials.refreshIfExpired();
                AccessToken token = credentials.getAccessToken();
                String accessToken = token.getTokenValue();

                Log.i(TAG, "Access Token retrieved: " + accessToken);
                callback.onTokenReceived(accessToken);

            } catch (IOException e) {
                Log.e(TAG, "Failed to retrieve access token: " + e.getMessage());
                callback.onError(e);
            }
        });
    }
}
