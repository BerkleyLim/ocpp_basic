# 구현 가이드

**순서 지키기.** 아래 순서를 반드시 지킬 것.

---

## Phase 1: 프로젝트 셋업

### 1.1 build.gradle 설정

```gradle
plugins {
    id 'java'
    id 'application'
}

group = 'ocpp.practice'
version = '1.0-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass = 'ocpp.Main'
}

repositories {
    mavenCentral()
}

dependencies {
    // JSON 파싱
    implementation 'com.google.code.gson:gson:2.10.1'

    // WebSocket 서버
    implementation 'org.java-websocket:Java-WebSocket:1.5.4'

    // 로깅
    implementation 'org.slf4j:slf4j-api:2.0.9'
    implementation 'ch.qos.logback:logback-classic:1.4.11'

    // 테스트
    testImplementation platform('org.junit:junit-bom:5.10.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
}

test {
    useJUnitPlatform()
}
```

### 1.2 패키지 구조 생성

```bash
mkdir -p src/main/java/ocpp/{protocol,action,session,state,router,transport/websocket}
```

### 체크리스트

- [ ] build.gradle 의존성 추가
- [ ] 패키지 폴더 생성
- [ ] `./gradlew build` 성공

---

## Phase 2: Protocol 계층 구현

**목표:** OCPP 메시지를 Java 객체로 표현

### 2.1 MessageType.java

```java
package ocpp.protocol;

public enum MessageType {
    CALL(2),
    CALL_RESULT(3),
    CALL_ERROR(4);

    private final int id;

    MessageType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static MessageType fromId(int id) {
        for (MessageType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type: " + id);
    }
}
```

### 2.2 OcppMessage.java

```java
package ocpp.protocol;

public interface OcppMessage {
    MessageType getType();
    String getUniqueId();
    String toJson();
}
```

### 2.3 Call.java

```java
package ocpp.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Call implements OcppMessage {
    private final String uniqueId;
    private final String action;
    private final JsonObject payload;

    public Call(String uniqueId, String action, JsonObject payload) {
        this.uniqueId = uniqueId;
        this.action = action;
        this.payload = payload;
    }

    public static Call fromJson(String json) {
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        // array[0] = 2 (MessageType)
        String uniqueId = array.get(1).getAsString();
        String action = array.get(2).getAsString();
        JsonObject payload = array.get(3).getAsJsonObject();
        return new Call(uniqueId, action, payload);
    }

    @Override
    public MessageType getType() {
        return MessageType.CALL;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    public String getAction() {
        return action;
    }

    public JsonObject getPayload() {
        return payload;
    }

    @Override
    public String toJson() {
        JsonArray array = new JsonArray();
        array.add(MessageType.CALL.getId());
        array.add(uniqueId);
        array.add(action);
        array.add(payload);
        return array.toString();
    }
}
```

### 2.4 CallResult.java

```java
package ocpp.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class CallResult implements OcppMessage {
    private final String uniqueId;
    private final JsonObject payload;

    public CallResult(String uniqueId, JsonObject payload) {
        this.uniqueId = uniqueId;
        this.payload = payload;
    }

    @Override
    public MessageType getType() {
        return MessageType.CALL_RESULT;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    public JsonObject getPayload() {
        return payload;
    }

    @Override
    public String toJson() {
        JsonArray array = new JsonArray();
        array.add(MessageType.CALL_RESULT.getId());
        array.add(uniqueId);
        array.add(payload);
        return array.toString();
    }
}
```

### 2.5 ErrorCode.java

```java
package ocpp.protocol;

public enum ErrorCode {
    NOT_IMPLEMENTED("NotImplemented"),
    NOT_SUPPORTED("NotSupported"),
    INTERNAL_ERROR("InternalError"),
    PROTOCOL_ERROR("ProtocolError"),
    SECURITY_ERROR("SecurityError"),
    FORMATION_VIOLATION("FormationViolation"),
    PROPERTY_CONSTRAINT_VIOLATION("PropertyConstraintViolation"),
    OCCURRENCE_CONSTRAINT_VIOLATION("OccurrenceConstraintViolation"),
    TYPE_CONSTRAINT_VIOLATION("TypeConstraintViolation"),
    GENERIC_ERROR("GenericError");

    private final String value;

    ErrorCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

### 2.6 CallError.java

```java
package ocpp.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class CallError implements OcppMessage {
    private final String uniqueId;
    private final ErrorCode errorCode;
    private final String errorDescription;
    private final JsonObject errorDetails;

    public CallError(String uniqueId, ErrorCode errorCode, String errorDescription) {
        this(uniqueId, errorCode, errorDescription, new JsonObject());
    }

    public CallError(String uniqueId, ErrorCode errorCode, String errorDescription, JsonObject errorDetails) {
        this.uniqueId = uniqueId;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorDetails = errorDetails;
    }

    @Override
    public MessageType getType() {
        return MessageType.CALL_ERROR;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public String toJson() {
        JsonArray array = new JsonArray();
        array.add(MessageType.CALL_ERROR.getId());
        array.add(uniqueId);
        array.add(errorCode.getValue());
        array.add(errorDescription);
        array.add(errorDetails);
        return array.toString();
    }
}
```

### 체크리스트

- [ ] MessageType enum
- [ ] OcppMessage interface
- [ ] Call 클래스 (fromJson, toJson)
- [ ] CallResult 클래스
- [ ] CallError 클래스
- [ ] ErrorCode enum

---

## Phase 3: Session 계층 구현

**목표:** 충전기 연결 상태 관리

### 3.1 ChargePointState.java

```java
package ocpp.state;

public enum ChargePointState {
    DISCONNECTED,
    CONNECTED,
    AVAILABLE,
    PREPARING,
    CHARGING,
    FINISHING,
    RESERVED,
    UNAVAILABLE,
    FAULTED
}
```

### 3.2 ChargePointSession.java

```java
package ocpp.session;

import ocpp.protocol.OcppMessage;
import ocpp.state.ChargePointState;
import org.java_websocket.WebSocket;

public class ChargePointSession {
    private final String chargePointId;
    private final WebSocket connection;

    private ChargePointState state = ChargePointState.CONNECTED;
    private String vendor;
    private String model;
    private String currentUniqueId;

    public ChargePointSession(String chargePointId, WebSocket connection) {
        this.chargePointId = chargePointId;
        this.connection = connection;
    }

    public void send(OcppMessage message) {
        if (connection.isOpen()) {
            connection.send(message.toJson());
        }
    }

    // Getters and Setters
    public String getChargePointId() { return chargePointId; }
    public WebSocket getConnection() { return connection; }

    public ChargePointState getState() { return state; }
    public void setState(ChargePointState state) { this.state = state; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getCurrentUniqueId() { return currentUniqueId; }
    public void setCurrentUniqueId(String uniqueId) { this.currentUniqueId = uniqueId; }
}
```

### 3.3 SessionManager.java

```java
package ocpp.session;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Map<String, ChargePointSession> sessions = new ConcurrentHashMap<>();

    public void addSession(ChargePointSession session) {
        sessions.put(session.getChargePointId(), session);
    }

    public void removeSession(String chargePointId) {
        sessions.remove(chargePointId);
    }

    public ChargePointSession getSession(String chargePointId) {
        return sessions.get(chargePointId);
    }

    public Collection<ChargePointSession> getAllSessions() {
        return sessions.values();
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
```

### 체크리스트

- [ ] ChargePointState enum
- [ ] ChargePointSession 클래스
- [ ] SessionManager 클래스

---

## Phase 4: Action Handler 구현

**목표:** BootNotification, Heartbeat 처리

### 4.1 ActionHandler.java

```java
package ocpp.action;

import com.google.gson.JsonObject;
import ocpp.protocol.OcppMessage;
import ocpp.session.ChargePointSession;

public interface ActionHandler {
    String getAction();
    OcppMessage handle(ChargePointSession session, JsonObject payload);
}
```

### 4.2 BootNotificationHandler.java

```java
package ocpp.action;

import com.google.gson.JsonObject;
import ocpp.protocol.CallResult;
import ocpp.protocol.OcppMessage;
import ocpp.session.ChargePointSession;
import ocpp.state.ChargePointState;

import java.time.Instant;

public class BootNotificationHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "BootNotification";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        // 충전기 정보 추출
        String vendor = payload.get("chargePointVendor").getAsString();
        String model = payload.get("chargePointModel").getAsString();

        // 세션에 정보 저장
        session.setVendor(vendor);
        session.setModel(model);
        session.setState(ChargePointState.AVAILABLE);

        System.out.println("[BootNotification] " + session.getChargePointId() +
                " - Vendor: " + vendor + ", Model: " + model);

        // 응답 생성
        JsonObject response = new JsonObject();
        response.addProperty("status", "Accepted");
        response.addProperty("currentTime", Instant.now().toString());
        response.addProperty("interval", 300);  // 5분마다 Heartbeat

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}
```

### 4.3 HeartbeatHandler.java

```java
package ocpp.action;

import com.google.gson.JsonObject;
import ocpp.protocol.CallResult;
import ocpp.protocol.OcppMessage;
import ocpp.session.ChargePointSession;

import java.time.Instant;

public class HeartbeatHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "Heartbeat";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        System.out.println("[Heartbeat] " + session.getChargePointId());

        JsonObject response = new JsonObject();
        response.addProperty("currentTime", Instant.now().toString());

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}
```

### 체크리스트

- [ ] ActionHandler 인터페이스
- [ ] BootNotificationHandler 구현
- [ ] HeartbeatHandler 구현

---

## Phase 5: Router 구현

**목표:** Action 이름으로 적절한 Handler 찾기

### 5.1 MessageRouter.java

```java
package ocpp.router;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import ocpp.action.ActionHandler;
import ocpp.action.BootNotificationHandler;
import ocpp.action.HeartbeatHandler;
import ocpp.protocol.*;
import ocpp.session.ChargePointSession;

import java.util.HashMap;
import java.util.Map;

public class MessageRouter {
    private final Map<String, ActionHandler> handlers = new HashMap<>();

    public MessageRouter() {
        register(new BootNotificationHandler());
        register(new HeartbeatHandler());
        // 추가 핸들러는 여기에 등록
    }

    private void register(ActionHandler handler) {
        handlers.put(handler.getAction(), handler);
    }

    public OcppMessage route(ChargePointSession session, String rawMessage) {
        try {
            // 1. JSON 파싱
            JsonArray array = JsonParser.parseString(rawMessage).getAsJsonArray();
            int messageTypeId = array.get(0).getAsInt();

            // 2. Call 메시지만 처리 (서버가 받는 요청)
            if (messageTypeId != MessageType.CALL.getId()) {
                System.out.println("Ignoring non-Call message: " + messageTypeId);
                return null;
            }

            // 3. Call 파싱
            Call call = Call.fromJson(rawMessage);
            session.setCurrentUniqueId(call.getUniqueId());

            // 4. 핸들러 찾기
            ActionHandler handler = handlers.get(call.getAction());
            if (handler == null) {
                return new CallError(
                        call.getUniqueId(),
                        ErrorCode.NOT_IMPLEMENTED,
                        "Unknown action: " + call.getAction()
                );
            }

            // 5. 핸들러 실행
            return handler.handle(session, call.getPayload());

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            return new CallError(
                    "",
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }
}
```

### 체크리스트

- [ ] MessageRouter 클래스
- [ ] 핸들러 등록 로직
- [ ] 에러 처리

---

## Phase 6: WebSocket 서버 구현

**목표:** 충전기 연결 받고 메시지 주고받기

### 6.1 OcppWebSocketServer.java

```java
package ocpp.transport.websocket;

import ocpp.protocol.OcppMessage;
import ocpp.router.MessageRouter;
import ocpp.session.ChargePointSession;
import ocpp.session.SessionManager;
import ocpp.state.ChargePointState;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

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
        System.out.println("OCPP WebSocket Server started on port " + getPort());
        System.out.println("Waiting for charge point connections...");
    }

    private String extractChargePointId(String path) {
        // /ocpp/CP001 → CP001
        // /CP001 → CP001
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }
}
```

### 6.2 Main.java

```java
package ocpp;

import ocpp.router.MessageRouter;
import ocpp.session.SessionManager;
import ocpp.transport.websocket.OcppWebSocketServer;

public class Main {
    public static void main(String[] args) {
        int port = 8080;

        SessionManager sessionManager = new SessionManager();
        MessageRouter router = new MessageRouter();

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
```

### 체크리스트

- [ ] OcppWebSocketServer 클래스
- [ ] Main 클래스
- [ ] ChargePointId 추출 로직
- [ ] 세션 연결/해제 처리

---

## Phase 7: 테스트

### 7.1 서버 실행

```bash
./gradlew run
```

출력:
```
OCPP WebSocket Server started on port 8080
Waiting for charge point connections...
```

### 7.2 테스트 클라이언트 (Python)

`test_client.py` 생성:

```python
import asyncio
import websockets
import json

async def test_ocpp():
    uri = "ws://localhost:8080/ocpp/CP001"

    async with websockets.connect(uri, subprotocols=["ocpp1.6"]) as ws:
        print(f"Connected to {uri}")

        # 1. BootNotification
        boot_req = [
            2,
            "boot-001",
            "BootNotification",
            {
                "chargePointVendor": "TestVendor",
                "chargePointModel": "TestModel"
            }
        ]
        await ws.send(json.dumps(boot_req))
        print(f"Sent: {boot_req}")

        response = await ws.recv()
        print(f"Received: {response}")

        # 2. Heartbeat
        heartbeat_req = [
            2,
            "hb-001",
            "Heartbeat",
            {}
        ]
        await ws.send(json.dumps(heartbeat_req))
        print(f"Sent: {heartbeat_req}")

        response = await ws.recv()
        print(f"Received: {response}")

if __name__ == "__main__":
    asyncio.run(test_ocpp())
```

실행:
```bash
pip install websockets
python test_client.py
```

### 7.3 예상 결과

**서버 로그:**
```
OCPP WebSocket Server started on port 8080
[Connected] CP001 from /127.0.0.1:xxxxx
[Received] CP001: [2,"boot-001","BootNotification",{...}]
[BootNotification] CP001 - Vendor: TestVendor, Model: TestModel
[Send] CP001: [3,"boot-001",{"status":"Accepted",...}]
[Received] CP001: [2,"hb-001","Heartbeat",{}]
[Heartbeat] CP001
[Send] CP001: [3,"hb-001",{"currentTime":"..."}]
```

**클라이언트 로그:**
```
Connected to ws://localhost:8080/ocpp/CP001
Sent: [2, "boot-001", "BootNotification", {...}]
Received: [3,"boot-001",{"status":"Accepted","currentTime":"...","interval":300}]
Sent: [2, "hb-001", "Heartbeat", {}]
Received: [3,"hb-001",{"currentTime":"..."}]
```

### 체크리스트

- [ ] 서버 정상 시작
- [ ] BootNotification 요청/응답 성공
- [ ] Heartbeat 요청/응답 성공
- [ ] 연결 해제 시 세션 정리

---

## Phase 8: 추가 Action 구현 (선택)

Phase 7까지 완료했으면 **기본 목표 달성**.

추가로 구현하면 좋은 것들:

| 순서 | Action | 난이도 | 설명 |
|------|--------|--------|------|
| 1 | StatusNotification | 쉬움 | 상태 변경 알림 |
| 2 | Authorize | 쉬움 | RFID 인증 |
| 3 | StartTransaction | 중간 | 충전 시작 |
| 4 | StopTransaction | 중간 | 충전 종료 |
| 5 | MeterValues | 중간 | 계량 데이터 |
| 6 | RemoteStartTransaction | 어려움 | 서버→충전기 요청 |

---

## 절대 하지 말 것

- [ ] Spring Boot 추가 ❌
- [ ] DB 연동 ❌
- [ ] 인증/보안 로직 ❌
- [ ] 모든 Action 한번에 구현 ❌
- [ ] steve 코드 참고해서 구조 따라하기 ❌

---

## 다음 단계

→ [05-STATE-MACHINE.md](./05-STATE-MACHINE.md): 상태 머신 상세