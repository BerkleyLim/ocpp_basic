# OCPP Test Client

OCPP 서버 테스트용 클라이언트. Python, Node.js, Java 3가지 버전 제공.

## 사전 조건

OCPP 서버가 실행 중이어야 함:
```bash
# 프로젝트 루트에서
./gradlew run
```

기본 포트: `8081`

---

## Python

```bash
cd test_client/python

# 의존성 설치 (최초 1회)
pip install -r requirements.txt

# 실행
python test_client.py
```

**요구사항:** Python 3.8+

---

## Node.js

```bash
cd test_client/nodejs

# 의존성 설치 (최초 1회)
npm install

# 실행
npm start
# 또는
node test_client.js
```

**요구사항:** Node.js 16+

---

## Java

```bash
cd test_client/java

# 실행
./gradlew run
```

**요구사항:** Java 17+

---

## 테스트 시나리오

모든 클라이언트는 동일한 시나리오 수행:

1. **BootNotification** - 충전기 등록
   ```json
   → [2, "boot-001", "BootNotification", {"chargePointVendor": "TestVendor", "chargePointModel": "TestModel"}]
   ← [3, "boot-001", {"status": "Accepted", "currentTime": "...", "interval": 300}]
   ```

2. **Heartbeat** - 연결 확인
   ```json
   → [2, "hb-001", "Heartbeat", {}]
   ← [3, "hb-001", {"currentTime": "..."}]
   ```

---

## 예상 출력

```
Connected to ws://localhost:8081/ocpp/CP001
Sent: [2,"boot-001","BootNotification",{...}]
Received: [3,"boot-001",{"status":"Accepted","currentTime":"...","interval":300}]
Sent: [2,"hb-001","Heartbeat",{}]
Received: [3,"hb-001",{"currentTime":"..."}]
Disconnected
```

---

## 포트 변경

서버 포트가 다른 경우, 각 클라이언트의 URI 수정:

| 파일 | 수정 위치 |
|------|-----------|
| `python/test_client.py` | `uri = "ws://localhost:8081/ocpp/CP001"` |
| `nodejs/test_client.js` | `const uri = 'ws://localhost:8081/ocpp/CP001'` |
| `java/TestClient.java` | `String uri = "ws://localhost:8081/ocpp/CP001"` |