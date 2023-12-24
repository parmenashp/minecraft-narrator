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

    private static final String API_URL = "http://127.0.0.1:5000/event";
    private static final Logger LOGGER = LogManager.getLogger(APICommunicator.class);

    public static CompletableFuture<JsonObject> sendEventAsync(JsonObject event) {
        LOGGER.info("Making async API call to server: " + event);
        CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> sendEvent(event));
        future.completeOnTimeout(null, 1, TimeUnit.SECONDS);
        return future;
    }

    public static JsonObject sendEvent(JsonObject event) {
        HttpURLConnection connection = null;
        try {
            connection = initializeConnection();
            LOGGER.info("Sending event to server: " + event);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = event.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            } catch (Exception ex) {
                LOGGER.error("Exception during API call: " + ex.getMessage(), ex);
                return null;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                LOGGER.info("Response from server: " + response.toString());
                return JsonParser.parseString(response.toString()).getAsJsonObject();
            } catch (Exception ex) {
                LOGGER.error("Exception during API call: " + ex.getMessage(), ex);
                return null;
            }
        } catch (Exception ex) {
            LOGGER.error("Exception during API call: " + ex.getMessage(), ex);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection initializeConnection() throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);
        return connection;
    }
}
