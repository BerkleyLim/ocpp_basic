package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.clnewze.lab.www.state.ChargePointState;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeterValuesHandlerTest {

    private MeterValuesHandler handler;
    private ChargePointSession session;

    @BeforeEach
    void setUp() {
        handler = new MeterValuesHandler();
        session = new ChargePointSession("CP001", null);
        session.setCurrentUniqueId("meter-001");
        session.setState(ChargePointState.CHARGING);
    }

    @Test
    void testGetAction() {
        assertEquals("MeterValues", handler.getAction());
    }

    @Test
    void testHandle_ReturnsEmptyResponse() {
        // Given
        JsonObject payload = createMeterValuesPayload();

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
        CallResult callResult = (CallResult) result;

        assertEquals("meter-001", callResult.getUniqueId());
        assertEquals(0, callResult.getPayload().size()); // 빈 객체
    }

    @Test
    void testHandle_WithTransactionId() {
        // Given
        JsonObject payload = createMeterValuesPayload();
        payload.addProperty("transactionId", 123);

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
    }

    @Test
    void testHandle_WithMultipleSampledValues() {
        // Given
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);

        JsonArray meterValue = new JsonArray();
        JsonObject mv = new JsonObject();
        mv.addProperty("timestamp", "2024-01-15T10:00:00Z");

        JsonArray sampledValues = new JsonArray();

        JsonObject energy = new JsonObject();
        energy.addProperty("value", "5000");
        energy.addProperty("measurand", "Energy.Active.Import.Register");
        energy.addProperty("unit", "Wh");
        sampledValues.add(energy);

        JsonObject voltage = new JsonObject();
        voltage.addProperty("value", "230");
        voltage.addProperty("measurand", "Voltage");
        voltage.addProperty("unit", "V");
        sampledValues.add(voltage);

        JsonObject current = new JsonObject();
        current.addProperty("value", "16");
        current.addProperty("measurand", "Current.Import");
        current.addProperty("unit", "A");
        sampledValues.add(current);

        mv.add("sampledValue", sampledValues);
        meterValue.add(mv);
        payload.add("meterValue", meterValue);

        // When
        OcppMessage result = handler.handle(session, payload);

        // Then
        assertInstanceOf(CallResult.class, result);
    }

    private JsonObject createMeterValuesPayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("connectorId", 1);

        JsonArray meterValue = new JsonArray();
        JsonObject mv = new JsonObject();
        mv.addProperty("timestamp", "2024-01-15T10:00:00Z");

        JsonArray sampledValues = new JsonArray();
        JsonObject sample = new JsonObject();
        sample.addProperty("value", "5000");
        sampledValues.add(sample);

        mv.add("sampledValue", sampledValues);
        meterValue.add(mv);
        payload.add("meterValue", meterValue);

        return payload;
    }
}