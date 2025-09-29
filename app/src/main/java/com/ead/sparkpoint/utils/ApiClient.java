package com.ead.sparkpoint.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiClient {
    private static final String BASE_URL = "http://100.114.75.113/SparkPoint/api/";

    // Common method to handle connections
    private static String sendRequest(String endpoint, String method, String jsonInput, String token) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");

        // If sending JSON (POST/PUT), set content-type and write body
        if (jsonInput != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
        }

        // If authenticated request, attach token
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        conn.setConnectTimeout(10000); // timeout safety
        conn.setReadTimeout(10000);

        // ✅ Handle success & error responses
        BufferedReader br;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        } else {
            br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line.trim());
        }

        return response.toString();
    }

    // 🔹 GET request (no body)
    public static String getRequest(String endpoint, String token) throws Exception {
        return sendRequest(endpoint, "GET", null, token);
    }

    // 🔹 POST request
    public static String postRequest(String endpoint, String jsonInput, String token) throws Exception {
        return sendRequest(endpoint, "POST", jsonInput, token);
    }

    // 🔹 PUT request
    public static String putRequest(String endpoint, String jsonInput, String token) throws Exception {
        return sendRequest(endpoint, "PUT", jsonInput, token);
    }

    // 🔹 DELETE request
    public static String deleteRequest(String endpoint, String token) throws Exception {
        return sendRequest(endpoint, "DELETE", null, token);
    }
}
