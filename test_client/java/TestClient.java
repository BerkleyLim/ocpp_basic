import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCPP 테스트 클라이언트 (Java)
 *
 * 실행 방법:
 * cd test_client/java
 * ./gradlew run
 */
public class TestClient extends WebSocketClient {

    private final CountDownLatch latch = new CountDownLatch(1);
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private int transactionId = 0;

    public TestClient(URI serverUri) {
        super(serverUri);
    }

    private String now() {
        return Instant.now().toString();
    }

    private String sendAndWait(String message) throws InterruptedException {
        System.out.println("  → Sent: " + message);
        send(message);
        String response = responseQueue.take();
        System.out.println("  ← Received: " + response);
        return response;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to " + getURI());
        System.out.println("==================================================");

        new Thread(() -> {
            try {
                runTests();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                close();
            }
        }).start();
    }

    private void runTests() throws InterruptedException {
        // 1. BootNotification
        System.out.println("\n[1] BootNotification");
        sendAndWait("[2,\"boot-001\",\"BootNotification\"," +
                "{\"chargePointVendor\":\"TestVendor\",\"chargePointModel\":\"TestModel\"," +
                "\"chargePointSerialNumber\":\"CP001-SN\",\"firmwareVersion\":\"1.0.0\"}]");

        // 2. Heartbeat
        System.out.println("\n[2] Heartbeat");
        sendAndWait("[2,\"hb-001\",\"Heartbeat\",{}]");

        // 3. StatusNotification
        System.out.println("\n[3] StatusNotification");
        sendAndWait("[2,\"status-001\",\"StatusNotification\"," +
                "{\"connectorId\":1,\"errorCode\":\"NoError\",\"status\":\"Available\"," +
                "\"timestamp\":\"" + now() + "\"}]");

        // 4. Authorize
        System.out.println("\n[4] Authorize");
        sendAndWait("[2,\"auth-001\",\"Authorize\",{\"idTag\":\"RFID12345678\"}]");

        // 5. StartTransaction
        System.out.println("\n[5] StartTransaction");
        String startResponse = sendAndWait("[2,\"start-001\",\"StartTransaction\"," +
                "{\"connectorId\":1,\"idTag\":\"RFID12345678\",\"meterStart\":0," +
                "\"timestamp\":\"" + now() + "\"}]");

        // transactionId 추출
        Pattern pattern = Pattern.compile("\"transactionId\":(\\d+)");
        Matcher matcher = pattern.matcher(startResponse);
        if (matcher.find()) {
            transactionId = Integer.parseInt(matcher.group(1));
        }

        // 6. MeterValues
        System.out.println("\n[6] MeterValues");
        sendAndWait("[2,\"meter-001\",\"MeterValues\"," +
                "{\"connectorId\":1,\"transactionId\":" + transactionId + "," +
                "\"meterValue\":[{\"timestamp\":\"" + now() + "\"," +
                "\"sampledValue\":[" +
                "{\"value\":\"5000\",\"measurand\":\"Energy.Active.Import.Register\",\"unit\":\"Wh\"}," +
                "{\"value\":\"230\",\"measurand\":\"Voltage\",\"unit\":\"V\"}," +
                "{\"value\":\"16\",\"measurand\":\"Current.Import\",\"unit\":\"A\"}" +
                "]}]}]");

        // 7. StopTransaction
        System.out.println("\n[7] StopTransaction");
        sendAndWait("[2,\"stop-001\",\"StopTransaction\"," +
                "{\"transactionId\":" + transactionId + ",\"meterStop\":15000," +
                "\"timestamp\":\"" + now() + "\",\"reason\":\"Local\"}]");

        // 8. StatusNotification (back to Available)
        System.out.println("\n[8] StatusNotification (back to Available)");
        sendAndWait("[2,\"status-002\",\"StatusNotification\"," +
                "{\"connectorId\":1,\"errorCode\":\"NoError\",\"status\":\"Available\"," +
                "\"timestamp\":\"" + now() + "\"}]");

        System.out.println("\n==================================================");
        System.out.println("All tests completed!");
    }

    @Override
    public void onMessage(String message) {
        responseQueue.offer(message);
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