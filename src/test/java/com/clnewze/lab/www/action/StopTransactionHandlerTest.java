package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StopTransactionHandlerTest {

    private StopTransactionHandler handler;
    private ChargePointSession session;

    @BeforeEach
    void setUp() {
        handler = new StopTransactionHandler();
        session = new ChargePointSession("CP001", null);
        session.setCurrentUniqueId("stop-001");
        session.setState(ChargePointState.CHARGING);
    }

    @Test
    void testGetAction() {
        assertEquals("StopTransaction", handler.getAction());
    }

    @Test
    void testHandle_ReturnsSuccess() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("transactionId", 123);
        payload.addProperty("meterStop", 15000);
        payload.addProperty("timestamp", "2024-01-15T11:00:00Z");

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
        CallResult callResult = (CallResult) result;
        assertEquals("stop-001", callResult.getUniqueId());
    }

    @Test
    void testHandle_ChangesStateToAvailable() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("transactionId", 123);
        payload.addProperty("meterStop", 15000);

        // When
        handler.handle(session, payload);

        // Then
        assertEquals(ChargePointState.AVAILABLE, session.getState());
    }

    @Test
    void testHandle_WithIdTag_ReturnsIdTagInfo() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("transactionId", 123);
        payload.addProperty("meterStop", 15000);
        payload.addProperty("idTag", "RFID12345678");

        // When
        CallResult result = (CallResult) handler.handle(session, payload);

        // Then
        JsonObject response = result.getPayload();
        assertTrue(response.has("idTagInfo"));
        assertEquals("Accepted", response.getAsJsonObject("idTagInfo").get("status").getAsString());
    }

    @Test
    void testHandle_WithoutIdTag_NoIdTagInfo() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("transactionId", 123);
        payload.addProperty("meterStop", 15000);

        // When
        CallResult result = (CallResult) handler.handle(session, payload);

        // Then
        JsonObject response = result.getPayload();
        assertFalse(response.has("idTagInfo"));
    }
}