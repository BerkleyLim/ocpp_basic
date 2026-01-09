package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusNotificationHandlerTest {

    private StatusNotificationHandler handler;
    private ChargePointSession session;

    @BeforeEach
    void setUp() {
        handler = new StatusNotificationHandler();
        session = new ChargePointSession("CP001", null);
        session.setCurrentUniqueId("status-001");
        session.setState(ChargePointState.CONNECTED);
    }

    @Test
    void testGetAction() {
        assertEquals("StatusNotification", handler.getAction());
    }

    @Test
    void testHandle_ReturnsEmptyResponse() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);
        payload.addProperty("errorCode", "NoError");
        payload.addProperty("status", "Available");

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
        CallResult callResult = (CallResult) result;

        assertEquals("status-001", callResult.getUniqueId());
        assertEquals(0, callResult.getPayload().size()); // 빈 객체
    }

    @Test
    void testHandle_Available_UpdatesState() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);
        payload.addProperty("errorCode", "NoError");
        payload.addProperty("status", "Available");

        // When
        handler.handle(session, payload);

        // Then
        assertEquals(ChargePointState.AVAILABLE, session.getState());
    }

    @Test
    void testHandle_Charging_UpdatesState() {
        // Given
        session.setState(ChargePointState.PREPARING);
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);
        payload.addProperty("errorCode", "NoError");
        payload.addProperty("status", "Charging");

        // When
        handler.handle(session, payload);

        // Then
        assertEquals(ChargePointState.CHARGING, session.getState());
    }

    @Test
    void testHandle_Faulted_UpdatesState() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);
        payload.addProperty("errorCode", "GroundFailure");
        payload.addProperty("status", "Faulted");

        // When
        handler.handle(session, payload);

        // Then
        assertEquals(ChargePointState.FAULTED, session.getState());
    }
}