package com.mitsuaky.stanleyparable;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class APICommunicator {

    private static final String API_URL = "http://127.0.0.1:5000/event";

    public static JSONObject sendEvent(JSONObject event) {
        try {
            System.out.println("Sending event to server: " + event); // Log para verificar o evento enviado
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = event.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            System.out.println("Response from server: " + response); // Log para verificar a resposta
            return new JSONObject(response.toString());

        } catch (Exception e) {
            System.err.println("Error in sending event to server: " + e.getMessage()); // Log de erro
            e.printStackTrace();
            return null;
        }
    }
}
