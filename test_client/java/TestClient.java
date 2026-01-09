import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

/**
 * OCPP 테스트 클라이언트 (Java)
 *
 * 실행 방법:
 * cd test_client/java
 * ./gradlew run
 */
public class TestClient extends WebSocketClient {

    private int messageCount = 0;
    private final CountDownLatch latch = new CountDownLatch(1);

    public TestClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to " + getURI());

        // 1. BootNotification
        String bootReq = "[2,\"boot-001\",\"BootNotification\"," +
                "{\"chargePointVendor\":\"TestVendor\",\"chargePointModel\":\"TestModel\"}]";
        System.out.println("Sent: " + bootReq);
        send(bootReq);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        messageCount++;

        if (messageCount == 1) {
            // 2. Heartbeat
            String hbReq = "[2,\"hb-001\",\"Heartbeat\",{}]";
            System.out.println("Sent: " + hbReq);
            send(hbReq);
        } else if (messageCount == 2) {
            close();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected: " + reason);
        latch.countDown();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Error: " + ex.getMessage());
        latch.countDown();
    }

    public void await() throws InterruptedException {
        latch.await();
    }

    public static void main(String[] args) throws Exception {
        String uri = "ws://localhost:8081/ocpp/CP001";
        TestClient client = new TestClient(new URI(uri));
        client.connect();
        client.await();
    }
}