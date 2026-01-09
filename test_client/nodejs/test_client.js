const WebSocket = require('ws');

const uri = 'ws://localhost:8081/ocpp/CP001';
const ws = new WebSocket(uri);

ws.on('open', function open() {
    console.log(`Connected to ${uri}`);

    // 1. BootNotification
    const bootReq = [2, "boot-001", "BootNotification", {
        chargePointVendor: "TestVendor",
        chargePointModel: "TestModel"
    }];
    console.log(`Sent: ${JSON.stringify(bootReq)}`);
    ws.send(JSON.stringify(bootReq));
});

let messageCount = 0;

ws.on('message', function message(data) {
    console.log(`Received: ${data}`);
    messageCount++;

    if (messageCount === 1) {
        // 2. Heartbeat
        const hbReq = [2, "hb-001", "Heartbeat", {}];
        console.log(`Sent: ${JSON.stringify(hbReq)}`);
        ws.send(JSON.stringify(hbReq));
    } else if (messageCount === 2) {
        ws.close();
    }
});

ws.on('close', function close() {
    console.log('Disconnected');
});

ws.on('error', function error(err) {
    console.error('Error:', err.message);
});