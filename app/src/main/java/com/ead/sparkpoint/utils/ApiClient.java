package com.ead.sparkpoint.utils;

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
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
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

    // 🔹 PATCH request
    public static String patchRequest(String endpoint, String jsonInput, String token) throws Exception {
        return sendRequest(endpoint, "PATCH", jsonInput, token);
    }

    // 🔹 DELETE request
    public static String deleteRequest(String endpoint, String token) throws Exception {
        return sendRequest(endpoint, "DELETE", null, token);
    }
}
