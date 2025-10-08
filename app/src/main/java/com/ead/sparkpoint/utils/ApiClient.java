package com.ead.sparkpoint.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    // Common method to handle connections
    private static String sendRequest(String endpoint, String method, String jsonInput, String token) throws Exception {
        URL url = new URL(Constants.BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");

        // If authenticated request, attach token
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        // If sending JSON (POST/PUT), set content-type and write body
        if (jsonInput != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }


        conn.setConnectTimeout(10000); // timeout safety
        conn.setReadTimeout(10000);

        // ✅ Handle success & error responses
        BufferedReader br;
        int responseCode = conn.getResponseCode(); // Get response code once
        if (responseCode >= 200 && responseCode < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line.trim());
        }

        return response.toString();
    }

    // 🔹 GET request (no body)
    public static String getRequest(Context context, String endpoint) throws Exception {
        TokenManager tokenManager = new TokenManager(context);
        String token =  tokenManager.getAccessToken();
        String response;

        try {
            response = sendRequest(endpoint, "GET", null, token);
        } catch (Exception e) {
            throw e;
        }

        if (response.contains("Authentication required") ||
                response.contains("expired") ||
                response.contains("invalid")) {
            token = tokenManager.refreshAccessToken();
            if (token != null) {
                response = sendRequest(endpoint, "GET", null, token); // retry
            } else {
                return null; // user logged out
            }
        }

        return response;
    }

    // 🔹 POST request (with auto refresh)
    public static String postRequest(Context context, String endpoint, String jsonInput) throws Exception {
        TokenManager tokenManager = new TokenManager(context);
        String token = tokenManager.getAccessToken();
        String response;
        try {
            response = sendRequest(endpoint, "POST", jsonInput, token);
        } catch (Exception e) {
            throw e;
        }

        if (response != null && response.contains("Authentication required")) {
            token = tokenManager.refreshAccessToken();
            if (token != null) {
                response = sendRequest(endpoint, "POST", jsonInput, token);
            } else {
                return null;
            }
        }
        return response;
    }

    // 🔹 PUT request (with auto refresh)
    public static String putRequest(Context context, String endpoint, String jsonInput) throws Exception {
        TokenManager tokenManager = new TokenManager(context);
        String token = tokenManager.getAccessToken();
        String response;
        try {
            response = sendRequest(endpoint, "PUT", jsonInput, token);
        } catch (Exception e) {
            throw e;
        }

        if (response.contains("Authentication required")) {
            token = tokenManager.refreshAccessToken();
            if (token != null) {
                response = sendRequest(endpoint, "PUT", jsonInput, token);
            } else {
                return null;
            }
        }
        return response;
    }

    // 🔹 PATCH request (with auto refresh)
    public static String patchRequest(Context context, String endpoint, String jsonInput) throws Exception {
        TokenManager tokenManager = new TokenManager(context);
        String token = tokenManager.getAccessToken();
        String response;
        try {
            response = sendRequest(endpoint, "PATCH", jsonInput, token);
        } catch (Exception e) {
            throw e;
        }

        if (response.contains("Authentication required")) {
            token = tokenManager.refreshAccessToken();
            if (token != null) {
                response = sendRequest(endpoint, "PATCH", jsonInput, token);
            } else {
                return null;
            }
        }
        return response;
    }

    // 🔹 DELETE request (with auto refresh)
    public static String deleteRequest(Context context, String endpoint) throws Exception {
        TokenManager tokenManager = new TokenManager(context);
        String token = tokenManager.getAccessToken();
        String response;
        try {
            response = sendRequest(endpoint, "DELETE", null, token);
        } catch (Exception e) {
            throw e;
        }

        if (response.contains("Authentication required")) {
            token = tokenManager.refreshAccessToken();
            if (token != null) {
                response = sendRequest(endpoint, "DELETE", null, token);
            } else {
                return null;
            }
        }
        return response;
    }
}
