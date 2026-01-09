package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BootNotificationHandlerTest {

    private BootNotificationHandler handler;
    private ChargePointSession session;

    @BeforeEach
    void setUp() {
        handler = new BootNotificationHandler();
        session = new ChargePointSession("CP001", null);
        session.setCurrentUniqueId("test-001");
    }

    @Test
    void testGetAction() {
        assertEquals("BootNotification", handler.getAction());
    }

    @Test
    void testHandle_ReturnsAccepted() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("chargePointVendor", "TestVendor");
        payload.addProperty("chargePointModel", "TestModel");

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
        CallResult callResult = (CallResult) result;

        assertEquals("test-001", callResult.getUniqueId());

        JsonObject response = callResult.getPayload();
        assertEquals("Accepted", response.get("status").getAsString());
        assertTrue(response.has("currentTime"));
        assertEquals(300, response.get("interval").getAsInt());
    }

    @Test
    void testHandle_UpdatesSession() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("chargePointVendor", "TestVendor");
        payload.addProperty("chargePointModel", "TestModel");

        // When
        handler.handle(session, payload);

        // Then
        assertEquals("TestVendor", session.getVendor());
        assertEquals("TestModel", session.getModel());
        assertEquals(ChargePointState.AVAILABLE, session.getState());
    }
}