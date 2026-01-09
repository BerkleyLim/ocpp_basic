const WebSocket = require('ws');

const uri = 'ws://localhost:8081/ocpp/CP001';
const ws = new WebSocket(uri);

let messageQueue = [];
let transactionId = null;

function now() {
    return new Date().toISOString();
}

function sendAndWait(message) {
    return new Promise((resolve) => {
        messageQueue.push(resolve);
        console.log(`  → Sent: ${JSON.stringify(message)}`);
        ws.send(JSON.stringify(message));
    });
}

ws.on('open', async function open() {
    console.log(`Connected to ${uri}`);
    console.log('='.repeat(50));

    try {
        // 1. BootNotification
        console.log('\n[1] BootNotification');
        await sendAndWait([2, "boot-001", "BootNotification", {
            chargePointVendor: "TestVendor",
            chargePointModel: "TestModel",
            chargePointSerialNumber: "CP001-SN",
            firmwareVersion: "1.0.0"
        }]);

        // 2. Heartbeat
        console.log('\n[2] Heartbeat');
        await sendAndWait([2, "hb-001", "Heartbeat", {}]);

        // 3. StatusNotification
        console.log('\n[3] StatusNotification');
        await sendAndWait([2, "status-001", "StatusNotification", {
            connectorId: 1,
            errorCode: "NoError",
            status: "Available",
            timestamp: now()
        }]);

        // 4. Authorize
        console.log('\n[4] Authorize');
        await sendAndWait([2, "auth-001", "Authorize", {
            idTag: "RFID12345678"
        }]);

        // 5. StartTransaction
        console.log('\n[5] StartTransaction');
        const startResponse = await sendAndWait([2, "start-001", "StartTransaction", {
            connectorId: 1,
            idTag: "RFID12345678",
            meterStart: 0,
            timestamp: now()
        }]);
        transactionId = JSON.parse(startResponse)[2].transactionId || 1;

        // 6. MeterValues
        console.log('\n[6] MeterValues');
        await sendAndWait([2, "meter-001", "MeterValues", {
            connectorId: 1,
            transactionId: transactionId,
            meterValue: [{
                timestamp: now(),
                sampledValue: [
                    { value: "5000", measurand: "Energy.Active.Import.Register", unit: "Wh" },
                    { value: "230", measurand: "Voltage", unit: "V" },
                    { value: "16", measurand: "Current.Import", unit: "A" }
                ]
            }]
        }]);

        // 7. StopTransaction
        console.log('\n[7] StopTransaction');
        await sendAndWait([2, "stop-001", "StopTransaction", {
            transactionId: transactionId,
            meterStop: 15000,
            timestamp: now(),
            reason: "Local"
        }]);

        // 8. StatusNotification (back to Available)
        console.log('\n[8] StatusNotification (back to Available)');
        await sendAndWait([2, "status-002", "StatusNotification", {
            connectorId: 1,
            errorCode: "NoError",
            status: "Available",
            timestamp: now()
        }]);

        console.log('\n' + '='.repeat(50));
        console.log('All tests completed!');
        ws.close();

    } catch (err) {
        console.error('Error:', err.message);
        ws.close();
    }
});

ws.on('message', function message(data) {
    console.log(`  ← Received: ${data}`);
    if (messageQueue.length > 0) {
        const resolve = messageQueue.shift();
        resolve(data.toString());
    }
});

ws.on('close', function close() {
    console.log('Disconnected');
});

ws.on('error', function error(err) {
    console.error('Error:', err.message);
});