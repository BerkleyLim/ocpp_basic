import asyncio
import websockets
import json

async def test_ocpp():
    uri = "ws://localhost:8081/ocpp/CP001"

    async with websockets.connect(uri) as ws:
        print(f"Connected to {uri}")

        # 1. BootNotification
        boot_req = [2, "boot-001", "BootNotification", {
            "chargePointVendor": "TestVendor",
            "chargePointModel": "TestModel"
        }]
        await ws.send(json.dumps(boot_req))
        print(f"Sent: {boot_req}")
        print(f"Received: {await ws.recv()}")

        # 2. Heartbeat
        hb_req = [2, "hb-001", "Heartbeat", {}]
        await ws.send(json.dumps(hb_req))
        print(f"Sent: {hb_req}")
        print(f"Received: {await ws.recv()}")

if __name__ == "__main__":
    asyncio.run(test_ocpp())