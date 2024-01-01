package com.mitsuaky.stanleyparable;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class APICommunicator {
    private static final String API_URL = "http://127.0.0.1:5000/";
    private static final Logger LOGGER = LogManager.getLogger(APICommunicator.class);

    public static CompletableFuture<JsonObject> sendRequestAsync(String method, String path, JsonObject event) throws RuntimeException {
        LOGGER.info("Making async API call to server: " + event);
        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> sendRequest(method, path, event));
        future.orTimeout(20, TimeUnit.SECONDS);
        return future;
    }

    public static JsonObject sendRequest(String method, String path, JsonObject event) throws RuntimeException {
        HttpURLConnection connection = null;
        try {
            connection = initializeConnection(method, path);
            LOGGER.info("Sending event to server: " + event);
            if (event != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = event.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                } catch (Exception ex) {
                    throw new RuntimeException("Could not send event to server: " + ex.getMessage(), ex);
                }
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return JsonParser.parseString(response.toString()).getAsJsonObject();
            } catch (Exception ex) {
                throw new RuntimeException("Could not read response from server: " + ex.getMessage(), ex);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not send request to server: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection initializeConnection(String method, String path) throws Exception {
        URL url = new URL(API_URL + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);
        return connection;
    }
}
