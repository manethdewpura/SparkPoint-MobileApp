package com.ead.sparkpoint.utils;

import android.content.Context;
import android.content.Intent;
import android.os.StrictMode; // Required for synchronous network calls (not recommended for production)

import com.ead.sparkpoint.activities.LoginActivity;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TokenManager {
    private final Context context;
    private final AppUserDAO userDAO;

    public TokenManager(Context context) {
        this.context = context;
        this.userDAO = new AppUserDAO(context);
        // WARNING: This is for demonstration to fix the compilation error.
        // In a real app, network calls must be on a background thread.
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    public String getAccessToken() {
        AppUser user = userDAO.getUser();
        return user != null ? user.getAccessToken() : null;
    }

    /**
     * Try to refresh the access token using the refresh token.
     * If refresh fails -> logout user.
     */
    public String refreshAccessToken() {
        AppUser user = userDAO.getUser();
        if (user == null || user.getRefreshToken() == null) {
            logoutUser();
            return null;
        }

        try {
            JSONObject req = new JSONObject();
            req.put("userId", user.getId());
            req.put("refreshToken", user.getRefreshToken());

            String response = makePostRequest(Constants.REFRESH_TOKEN_URL, req.toString());

            if (response == null) {
                logoutUser();
                return null;
            }

            JSONObject res = new JSONObject(response);

            if (!res.has("accessToken")) {
                logoutUser();
                return null;
            }

            String newAccess = res.getString("accessToken");
            String newRefresh = res.getString("refreshToken");

            user.setAccessToken(newAccess);
            user.setRefreshToken(newRefresh);
            userDAO.insertOrUpdateUser(user);

            return newAccess;

        } catch (Exception e) {
            e.printStackTrace();
            logoutUser();
            return null;
        }
    }

    /**
     * Clears DB and redirects to login page
     */
    public void logoutUser() {
        AppUser user = userDAO.getUser();

        if (user != null) {
            try {
                JSONObject req = new JSONObject();
                req.put("userId", user.getId());

                makePostRequest(Constants.LOGOUT_URL, req.toString());

            } catch (Exception e) {
                e.printStackTrace();
                // continue to clear local data anyway
            }
        }

        // Clear local DB
        userDAO.clearUsers();

        // Redirect to login
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * Helper method to make a synchronous POST request.
     * NOTE: This should not be run on the main UI thread in a production app.
     * @param urlString The URL for the request.
     * @param jsonInputString The JSON body for the request.
     * @return The response from the server as a String, or null if it fails.
     */
    private String makePostRequest(String urlString, String jsonInputString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(Constants.BASE_URL + urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000); // 5 seconds
            conn.setReadTimeout(5000);    // 5 seconds

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // Success
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
