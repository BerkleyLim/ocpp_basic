package com.clnewze.lab.www.transport.websocket;

import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.router.MessageRouter;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.session.SessionManager;
import com.clnewze.lab.www.state.ChargePointState;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * OCPP WebSocket 서버
 * 충전기 연결을 받고 메시지를 처리
 */
public class OcppWebSocketServer extends WebSocketServer {

    private final SessionManager sessionManager;
    private final MessageRouter router;

    public OcppWebSocketServer(int port, SessionManager sessionManager, MessageRouter router) {
        super(new InetSocketAddress(port));
        this.sessionManager = sessionManager;
        this.router = router;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // URL 경로에서 chargePointId 추출
        // ws://localhost:8080/ocpp/CP001 → CP001
        String path = handshake.getResourceDescriptor();
        String chargePointId = extractChargePointId(path);

        System.out.println("[Connected] " + chargePointId + " from " + conn.getRemoteSocketAddress());

        // 세션 생성
        ChargePointSession session = new ChargePointSession(chargePointId, conn);
        session.setState(ChargePointState.CONNECTED);
        sessionManager.addSession(session);

        // 세션 정보를 WebSocket에 첨부 (나중에 찾기 위해)
        conn.setAttachment(chargePointId);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String chargePointId = conn.getAttachment();
        ChargePointSession session = sessionManager.getSession(chargePointId);

        System.out.println("[Received] " + chargePointId + ": " + message);

        // 라우터로 메시지 처리
        OcppMessage response = router.route(session, message);

        if (response != null) {
            String responseJson = response.toJson();
            System.out.println("[Send] " + chargePointId + ": " + responseJson);
            conn.send(responseJson);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String chargePointId = conn.getAttachment();

        System.out.println("[Disconnected] " + chargePointId + " - " + reason);

        // 세션 정리
        ChargePointSession session = sessionManager.getSession(chargePointId);
        if (session != null) {
            session.setState(ChargePointState.DISCONNECTED);
            sessionManager.removeSession(chargePointId);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String chargePointId = conn != null ? conn.getAttachment() : "unknown";
        System.err.println("[Error] " + chargePointId + ": " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("===========================================");
        System.out.println("OCPP WebSocket Server started on port " + getPort());
        System.out.println("Waiting for charge point connections...");
        System.out.println("===========================================");
    }

    /**
     * URL 경로에서 chargePointId 추출
     * /ocpp/CP001 → CP001
     * /CP001 → CP001
     */
    private String extractChargePointId(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }
}