# 프로젝트 구조

## 전체 폴더 구조

```
ocpp_basic/
├── build.gradle
├── settings.gradle
├── docs/                           # 문서
│   ├── 01-OCPP-OVERVIEW.md
│   ├── 02-MESSAGE-FORMAT.md
│   ├── 03-PROJECT-STRUCTURE.md     # (현재 문서)
│   ├── 04-IMPLEMENTATION-GUIDE.md
│   └── 05-STATE-MACHINE.md
│
└── src/main/java/ocpp/
    │
    ├── Main.java                   # 진입점
    │
    ├── protocol/                   # OCPP 프로토콜 정의
    │   ├── MessageType.java        # 메시지 타입 (2, 3, 4)
    │   ├── OcppMessage.java        # 메시지 인터페이스
    │   ├── Call.java               # Call 메시지 [2]
    │   ├── CallResult.java         # CallResult 메시지 [3]
    │   ├── CallError.java          # CallError 메시지 [4]
    │   └── ErrorCode.java          # OCPP 에러 코드 enum
    │
    ├── action/                     # Action 핸들러
    │   ├── ActionHandler.java      # 핸들러 인터페이스
    │   ├── BootNotificationHandler.java
    │   ├── HeartbeatHandler.java
    │   ├── AuthorizeHandler.java
    │   ├── StartTransactionHandler.java
    │   ├── StopTransactionHandler.java
    │   └── StatusNotificationHandler.java
    │
    ├── session/                    # 세션 관리
    │   ├── ChargePointSession.java # 충전기 1대 = 1세션
    │   └── SessionManager.java     # 전체 세션 관리
    │
    ├── state/                      # 상태 머신
    │   ├── ChargePointState.java   # 상태 enum
    │   └── StateTransition.java    # 상태 전이 규칙
    │
    ├── router/                     # 메시지 라우팅
    │   └── MessageRouter.java      # Action → Handler 매핑
    │
    └── transport/                  # 전송 계층
        └── websocket/
            ├── WebSocketServer.java    # WebSocket 서버
            └── WebSocketHandler.java   # 연결/메시지 처리
```

---

## 패키지별 역할

### 1. `protocol/` - OCPP 프로토콜 정의

**책임:** OCPP 메시지의 구조를 정의

```java
// MessageType.java
public enum MessageType {
    CALL(2),
    CALL_RESULT(3),
    CALL_ERROR(4);

    private final int id;
    // ...
}
```

```java
// Call.java
public class Call implements OcppMessage {
    private String uniqueId;
    private String action;
    private JsonObject payload;

    public static Call fromJson(JsonArray array) { ... }
    public String toJson() { ... }
}
```

```java
// CallResult.java
public class CallResult implements OcppMessage {
    private String uniqueId;
    private JsonObject payload;

    public CallResult(String uniqueId, JsonObject payload) { ... }
    public String toJson() { ... }
}
```

```java
// ErrorCode.java
public enum ErrorCode {
    NOT_IMPLEMENTED("NotImplemented"),
    NOT_SUPPORTED("NotSupported"),
    INTERNAL_ERROR("InternalError"),
    PROTOCOL_ERROR("ProtocolError"),
    FORMATION_VIOLATION("FormationViolation"),
    // ...
}
```

---

### 2. `action/` - Action 핸들러

**책임:** 각 OCPP Action의 비즈니스 로직 처리

```java
// ActionHandler.java (인터페이스)
public interface ActionHandler {
    String getAction();  // "BootNotification", "Heartbeat" 등
    CallResult handle(ChargePointSession session, JsonObject payload);
}
```

```java
// BootNotificationHandler.java
public class BootNotificationHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "BootNotification";
    }

    @Override
    public CallResult handle(ChargePointSession session, JsonObject payload) {
        String vendor = payload.get("chargePointVendor").getAsString();
        String model = payload.get("chargePointModel").getAsString();

        // 세션에 정보 저장
        session.setVendor(vendor);
        session.setModel(model);
        session.setState(ChargePointState.AVAILABLE);

        // 응답 생성
        JsonObject response = new JsonObject();
        response.addProperty("status", "Accepted");
        response.addProperty("currentTime", Instant.now().toString());
        response.addProperty("interval", 300);

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}
```

```java
// HeartbeatHandler.java
public class HeartbeatHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "Heartbeat";
    }

    @Override
    public CallResult handle(ChargePointSession session, JsonObject payload) {
        JsonObject response = new JsonObject();
        response.addProperty("currentTime", Instant.now().toString());

        return new CallResult(session.getCurrentUniqueId(), response);
    }
}
```

---

### 3. `session/` - 세션 관리

**책임:** 충전기 연결 상태 및 정보 관리

```java
// ChargePointSession.java
public class ChargePointSession {
    private final String chargePointId;      // 충전기 식별자
    private final WebSocket connection;       // WebSocket 연결

    private ChargePointState state;           // 현재 상태
    private String vendor;                    // 제조사
    private String model;                     // 모델명

    private String currentUniqueId;           // 현재 처리 중인 요청 ID
    private Map<String, PendingCall> pendingCalls;  // 응답 대기 중인 요청

    public void send(OcppMessage message) {
        connection.send(message.toJson());
    }

    // getters, setters
}
```

```java
// SessionManager.java
public class SessionManager {
    private final Map<String, ChargePointSession> sessions = new ConcurrentHashMap<>();

    public void addSession(String chargePointId, ChargePointSession session) {
        sessions.put(chargePointId, session);
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
}
```

---

### 4. `state/` - 상태 머신

**책임:** 충전기 상태 정의 및 상태 전이 규칙

```java
// ChargePointState.java
public enum ChargePointState {
    DISCONNECTED,    // 연결 끊김
    CONNECTED,       // WebSocket 연결됨 (Boot 전)
    REJECTED,        // Boot 거부됨
    AVAILABLE,       // 충전 가능
    PREPARING,       // 충전 준비 중
    CHARGING,        // 충전 중
    FINISHING,       // 충전 완료 처리 중
    RESERVED,        // 예약됨
    UNAVAILABLE,     // 사용 불가
    FAULTED          // 오류 상태
}
```

```java
// StateTransition.java
public class StateTransition {

    public static boolean canTransition(ChargePointState from, ChargePointState to) {
        // 상태 전이 규칙 정의
        return switch (from) {
            case DISCONNECTED -> to == CONNECTED;
            case CONNECTED -> to == AVAILABLE || to == REJECTED;
            case AVAILABLE -> to == PREPARING || to == UNAVAILABLE || to == FAULTED;
            case PREPARING -> to == CHARGING || to == AVAILABLE || to == FAULTED;
            case CHARGING -> to == FINISHING || to == FAULTED;
            case FINISHING -> to == AVAILABLE || to == FAULTED;
            // ...
            default -> false;
        };
    }
}
```

---

### 5. `router/` - 메시지 라우팅

**책임:** Action 이름으로 적절한 Handler 찾아서 실행

```java
// MessageRouter.java
public class MessageRouter {
    private final Map<String, ActionHandler> handlers = new HashMap<>();

    public MessageRouter() {
        // 핸들러 등록
        register(new BootNotificationHandler());
        register(new HeartbeatHandler());
        register(new AuthorizeHandler());
        register(new StartTransactionHandler());
        register(new StopTransactionHandler());
        register(new StatusNotificationHandler());
    }

    private void register(ActionHandler handler) {
        handlers.put(handler.getAction(), handler);
    }

    public OcppMessage route(ChargePointSession session, Call call) {
        ActionHandler handler = handlers.get(call.getAction());

        if (handler == null) {
            return new CallError(
                call.getUniqueId(),
                ErrorCode.NOT_IMPLEMENTED,
                "Unknown action: " + call.getAction()
            );
        }

        try {
            session.setCurrentUniqueId(call.getUniqueId());
            return handler.handle(session, call.getPayload());
        } catch (Exception e) {
            return new CallError(
                call.getUniqueId(),
                ErrorCode.INTERNAL_ERROR,
                e.getMessage()
            );
        }
    }
}
```

---

### 6. `transport/websocket/` - 전송 계층

**책임:** WebSocket 연결 관리 및 메시지 송수신

```java
// WebSocketServer.java
public class WebSocketServer {
    private final int port;
    private final SessionManager sessionManager;
    private final MessageRouter router;

    public void start() {
        // WebSocket 서버 시작
        // Java-WebSocket 라이브러리 사용
    }

    public void stop() {
        // 서버 종료
    }
}
```

```java
// WebSocketHandler.java
public class WebSocketHandler {
    private final SessionManager sessionManager;
    private final MessageRouter router;

    public void onOpen(WebSocket conn, String chargePointId) {
        // 새 세션 생성
        ChargePointSession session = new ChargePointSession(chargePointId, conn);
        session.setState(ChargePointState.CONNECTED);
        sessionManager.addSession(chargePointId, session);
    }

    public void onMessage(WebSocket conn, String message) {
        // 메시지 파싱 및 라우팅
        ChargePointSession session = findSession(conn);
        OcppMessage request = parseMessage(message);

        if (request instanceof Call call) {
            OcppMessage response = router.route(session, call);
            session.send(response);
        }
    }

    public void onClose(WebSocket conn) {
        // 세션 정리
        ChargePointSession session = findSession(conn);
        session.setState(ChargePointState.DISCONNECTED);
        sessionManager.removeSession(session.getChargePointId());
    }
}
```

---

## 의존성 방향

```
Main
  │
  ▼
WebSocketServer ──────► SessionManager
  │                          ▲
  ▼                          │
WebSocketHandler ────────────┘
  │
  ▼
MessageRouter
  │
  ▼
ActionHandler (여러 개)
  │
  ▼
ChargePointSession ──► ChargePointState
```

**핵심 원칙:**
- 상위 → 하위 방향으로만 의존
- `protocol/`은 다른 패키지에 의존하지 않음 (순수 데이터 정의)
- `action/`은 `session/`에 의존 (핸들러가 세션 상태 변경)
- `router/`는 `action/`을 알고 있음 (핸들러 등록)
- `transport/`는 전체를 조립

---

## 왜 이 구조인가?

### 1. protocol과 transport 분리

```
protocol/  ← OCPP 메시지 정의 (WebSocket 몰라도 됨)
transport/ ← WebSocket 통신 (OCPP 몰라도 됨)
```

**이유:** 나중에 transport를 바꿔도 (예: MQTT) protocol은 그대로

### 2. action 핸들러 분리

```
router/MessageRouter.java  ← 어떤 핸들러 호출할지 결정
action/XxxHandler.java     ← 실제 비즈니스 로직
```

**이유:**
- 새 Action 추가 시 핸들러만 만들면 됨
- 각 핸들러가 독립적 (테스트 용이)

### 3. session과 state 분리

```
session/ChargePointSession.java  ← 충전기 정보 + 연결
state/ChargePointState.java      ← 상태만
```

**이유:**
- 상태 전이 로직을 한 곳에서 관리
- 상태만 따로 테스트 가능

---

## build.gradle 의존성

```gradle
dependencies {
    // JSON 파싱
    implementation 'com.google.code.gson:gson:2.10.1'

    // WebSocket 서버
    implementation 'org.java-websocket:Java-WebSocket:1.5.4'

    // 로깅
    implementation 'org.slf4j:slf4j-api:2.0.9'
    implementation 'ch.qos.logback:logback-classic:1.4.11'

    // 테스트
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}
```

---

## 다음 단계

→ [04-IMPLEMENTATION-GUIDE.md](./04-IMPLEMENTATION-GUIDE.md): 구현 순서 가이드