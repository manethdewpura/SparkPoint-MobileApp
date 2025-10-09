package com.ead.sparkpoint.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    // Single background executor for network operations
    private static final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    // A generic method to submit a network task to the executor and wait for its result.
    private static String sendRequest(String endpoint, String method, String jsonInput, String token) throws Exception {
        Callable<String> task = () -> executeRequest(endpoint, method, jsonInput, token);
        Future<String> future = networkExecutor.submit(task);
        try {
            // Wait for a maximum of 15 seconds for the request to complete.
            return future.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true);// Attempt to cancel the task if it times out.
            throw e;
        }
    }

    /**
     * Performs the actual network I/O. This method should only be called from a background thread.
     */
    private static String executeRequest(String endpoint, String method, String jsonInput, String token) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(Constants.BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Accept", "application/json");

            // Add the Authorization header if a token is provided.
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            // Write the JSON body for POST, PUT, and PATCH requests.
            if (jsonInput != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Read the response from either the input stream (for success) or error stream (for failure).
            BufferedReader br;
            int responseCode = conn.getResponseCode();
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
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Performs an HTTP GET request with automatic token refresh.
     */
    public static String getRequest(Context context, String endpoint) throws Exception {
        TokenManager tokenManager = new TokenManager(context);
        String token =  tokenManager.getAccessToken();
        String response;

        try {
            response = sendRequest(endpoint, "GET", null, token);
        } catch (Exception e) {
            throw e;
        }

        // If the initial request fails due to authentication, try refreshing the token and retry.
        if (response.contains("Authentication required") ) {
            token = tokenManager.refreshAccessToken();
            if (token != null) {
                response = sendRequest(endpoint, "GET", null, token); // retry
            } else {
                return null; // user logged out
            }
        }

        return response;
    }

    /**
     * Performs an HTTP POST request with automatic token refresh.
     */
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

    /**
     * Performs an HTTP PUT request with automatic token refresh.
     */
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

    /**
     * Performs an HTTP PATCH request with automatic token refresh.
     */
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

    /**
     * Performs an HTTP DELETE request with automatic token refresh.
     */
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
