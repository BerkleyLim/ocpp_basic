package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizeHandlerTest {

    private AuthorizeHandler handler;
    private ChargePointSession session;

    @BeforeEach
    void setUp() {
        handler = new AuthorizeHandler();
        session = new ChargePointSession("CP001", null);
        session.setCurrentUniqueId("auth-001");
    }

    @Test
    void testGetAction() {
        assertEquals("Authorize", handler.getAction());
    }

    @Test
    void testHandle_ValidTag_ReturnsAccepted() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("idTag", "RFID12345678");

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
        CallResult callResult = (CallResult) result;

        JsonObject response = callResult.getPayload();
        assertTrue(response.has("idTagInfo"));

        JsonObject idTagInfo = response.getAsJsonObject("idTagInfo");
        assertEquals("Accepted", idTagInfo.get("status").getAsString());
    }

    @Test
    void testHandle_BlockedTag_ReturnsBlocked() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("idTag", "BLOCKED_USER");

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        CallResult callResult = (CallResult) result;
        JsonObject idTagInfo = callResult.getPayload().getAsJsonObject("idTagInfo");
        assertEquals("Blocked", idTagInfo.get("status").getAsString());
    }

    @Test
    void testHandle_ExpiredTag_ReturnsExpired() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("idTag", "EXPIRED_CARD");

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        CallResult callResult = (CallResult) result;
        JsonObject idTagInfo = callResult.getPayload().getAsJsonObject("idTagInfo");
        assertEquals("Expired", idTagInfo.get("status").getAsString());
    }

    @Test
    void testHandle_InvalidTag_ReturnsInvalid() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("idTag", "INVALID_TAG");

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        CallResult callResult = (CallResult) result;
        JsonObject idTagInfo = callResult.getPayload().getAsJsonObject("idTagInfo");
        assertEquals("Invalid", idTagInfo.get("status").getAsString());
    }
}