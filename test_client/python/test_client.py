import asyncio
import websockets
import json
from datetime import datetime

async def test_ocpp():
    uri = "ws://localhost:8081/ocpp/CP001"

    async with websockets.connect(uri) as ws:
        print(f"Connected to {uri}")
        print("=" * 50)

        # 1. BootNotification
        print("\n[1] BootNotification")
        boot_req = [2, "boot-001", "BootNotification", {
            "chargePointVendor": "TestVendor",
            "chargePointModel": "TestModel",
            "chargePointSerialNumber": "CP001-SN",
            "firmwareVersion": "1.0.0"
        }]
        await ws.send(json.dumps(boot_req))
        print(f"  → Sent: {json.dumps(boot_req)}")
        print(f"  ← Received: {await ws.recv()}")

        # 2. Heartbeat
        print("\n[2] Heartbeat")
        hb_req = [2, "hb-001", "Heartbeat", {}]
        await ws.send(json.dumps(hb_req))
        print(f"  → Sent: {json.dumps(hb_req)}")
        print(f"  ← Received: {await ws.recv()}")

        # 3. StatusNotification (Available)
        print("\n[3] StatusNotification")
        status_req = [2, "status-001", "StatusNotification", {
            "connectorId": 1,
            "errorCode": "NoError",
            "status": "Available",
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }]
        await ws.send(json.dumps(status_req))
        print(f"  → Sent: {json.dumps(status_req)}")
        print(f"  ← Received: {await ws.recv()}")

        # 4. Authorize
        print("\n[4] Authorize")
        auth_req = [2, "auth-001", "Authorize", {
            "idTag": "RFID12345678"
        }]
        await ws.send(json.dumps(auth_req))
        print(f"  → Sent: {json.dumps(auth_req)}")
        print(f"  ← Received: {await ws.recv()}")

        # 5. StartTransaction
        print("\n[5] StartTransaction")
        start_req = [2, "start-001", "StartTransaction", {
            "connectorId": 1,
            "idTag": "RFID12345678",
            "meterStart": 0,
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }]
        await ws.send(json.dumps(start_req))
        print(f"  → Sent: {json.dumps(start_req)}")
        response = await ws.recv()
        print(f"  ← Received: {response}")

        # transactionId 추출
        transaction_id = json.loads(response)[2].get("transactionId", 1)

        # 6. MeterValues
        print("\n[6] MeterValues")
        meter_req = [2, "meter-001", "MeterValues", {
            "connectorId": 1,
            "transactionId": transaction_id,
            "meterValue": [{
                "timestamp": datetime.utcnow().isoformat() + "Z",
                "sampledValue": [
                    {"value": "5000", "measurand": "Energy.Active.Import.Register", "unit": "Wh"},
                    {"value": "230", "measurand": "Voltage", "unit": "V"},
                    {"value": "16", "measurand": "Current.Import", "unit": "A"}
                ]
            }]
        }]
        await ws.send(json.dumps(meter_req))
        print(f"  → Sent: {json.dumps(meter_req)}")
        print(f"  ← Received: {await ws.recv()}")

        # 7. StopTransaction
        print("\n[7] StopTransaction")
        stop_req = [2, "stop-001", "StopTransaction", {
            "transactionId": transaction_id,
            "meterStop": 15000,
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "reason": "Local"
        }]
        await ws.send(json.dumps(stop_req))
        print(f"  → Sent: {json.dumps(stop_req)}")
        print(f"  ← Received: {await ws.recv()}")

        # 8. StatusNotification (Available)
        print("\n[8] StatusNotification (back to Available)")
        status_req2 = [2, "status-002", "StatusNotification", {
            "connectorId": 1,
            "errorCode": "NoError",
            "status": "Available",
            "timestamp": datetime.utcnow().isoformat() + "Z"
        }]
        await ws.send(json.dumps(status_req2))
        print(f"  → Sent: {json.dumps(status_req2)}")
        print(f"  ← Received: {await ws.recv()}")

        print("\n" + "=" * 50)
        print("All tests completed!")

if __name__ == "__main__":
    asyncio.run(test_ocpp())