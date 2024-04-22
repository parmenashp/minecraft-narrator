package com.mitsuaky.stanleyparable.client;

import com.mitsuaky.stanleyparable.common.events.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientEventHandler {
    private static final Logger LOGGER = LogManager.getLogger(ClientEventHandler.class);

    private static final WebSocketClient wsClient = WebSocketClient.getInstance();

    public static void handle(Event event, String msg) {
        LOGGER.debug("Sending {}:{} to backend", event.getValue(), msg);
        wsClient.sendEvent(event.getValue(), msg);
    }
}
