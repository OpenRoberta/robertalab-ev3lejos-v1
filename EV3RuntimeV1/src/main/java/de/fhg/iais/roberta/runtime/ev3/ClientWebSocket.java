package de.fhg.iais.roberta.runtime.ev3;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

class ClientWebSocket extends WebSocketClient {

    public ClientWebSocket(URI serverUri) {
        super(serverUri, new Draft_17());
    }

    /**
     * Store information that it is possible to connect via websocket to the server.
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        // ok
    }

    @Override
    public void onMessage(String message) {
        // ok

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // ok
    }

    /**
     * Provide information for Hal what connection type to use. If the server was reached at least once by the websocket, Hal will continue to use the
     * websocket. Otherwise Hal will fall back to Rest calls.
     */
    @Override
    public void onError(Exception e) {
        // ok
    }
}
