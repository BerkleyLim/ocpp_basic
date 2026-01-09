package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeartbeatHandlerTest {

    private HeartbeatHandler handler;
    private ChargePointSession session;

    @BeforeEach
    void setUp() {
        handler = new HeartbeatHandler();
        session = new ChargePointSession("CP001", null);
        session.setCurrentUniqueId("hb-001");
    }

    @Test
    void testGetAction() {
        assertEquals("Heartbeat", handler.getAction());
    }

    @Test
    void testHandle_ReturnsCurrentTime() {
        // Given
        JsonObject payload = new JsonObject();

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
        CallResult callResult = (CallResult) result;

        assertEquals("hb-001", callResult.getUniqueId());

        JsonObject response = callResult.getPayload();
        assertTrue(response.has("currentTime"));
        assertNotNull(response.get("currentTime").getAsString());
    }
}