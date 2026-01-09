package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StartTransactionHandlerTest {

    private StartTransactionHandler handler;
    private ChargePointSession session;

    @BeforeEach
    void setUp() {
        handler = new StartTransactionHandler();
        session = new ChargePointSession("CP001", null);
        session.setCurrentUniqueId("start-001");
        session.setState(ChargePointState.AVAILABLE);
    }

    @Test
    void testGetAction() {
        assertEquals("StartTransaction", handler.getAction());
    }

    @Test
    void testHandle_ReturnsTransactionId() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);
        payload.addProperty("idTag", "RFID12345678");
        payload.addProperty("meterStart", 0);
        payload.addProperty("timestamp", "2024-01-15T10:00:00Z");

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
        CallResult callResult = (CallResult) result;

        JsonObject response = callResult.getPayload();
        assertTrue(response.has("transactionId"));
        assertTrue(response.get("transactionId").getAsInt() > 0);

        JsonObject idTagInfo = response.getAsJsonObject("idTagInfo");
        assertEquals("Accepted", idTagInfo.get("status").getAsString());
    }

    @Test
    void testHandle_ChangesStateToCharging() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);
        payload.addProperty("idTag", "RFID12345678");
        payload.addProperty("meterStart", 0);

        // When
        handler.handle(session, payload);

        // Then
        assertEquals(ChargePointState.CHARGING, session.getState());
    }

    @Test
    void testHandle_TransactionIdIncreases() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);
        payload.addProperty("idTag", "RFID12345678");
        payload.addProperty("meterStart", 0);

        // When
        CallResult result1 = (CallResult) handler.handle(session, payload);
        int txId1 = result1.getPayload().get("transactionId").getAsInt();

        session.setCurrentUniqueId("start-002");
        CallResult result2 = (CallResult) handler.handle(session, payload);
        int txId2 = result2.getPayload().get("transactionId").getAsInt();

        // Then
        assertTrue(txId2 > txId1);
    }
}