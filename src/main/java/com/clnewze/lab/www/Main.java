package com.clnewze.lab.www;

import com.clnewze.lab.www.router.MessageRouter;
import com.clnewze.lab.www.session.SessionManager;
import com.clnewze.lab.www.transport.websocket.OcppWebSocketServer;

/**
 * OCPP 서버 진입점
 */
public class Main {

    public static void main(String[] args) {
        int port = 8081;

        // 컴포넌트 생성
        SessionManager sessionManager = new SessionManager();
        MessageRouter router = new MessageRouter();

        // 서버 생성 및 시작
        OcppWebSocketServer server = new OcppWebSocketServer(port, sessionManager, router);
        server.start();

        System.out.println("Press Ctrl+C to stop the server");

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down...");
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }
}