package com.clnewze.lab.www.session;

import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.state.ChargePointState;
import org.java_websocket.WebSocket;

/**
 * 충전기 1대 = 1세션
 * WebSocket 연결과 충전기 상태 정보를 관리
 */
public class ChargePointSession {

    private final String chargePointId;
    private final WebSocket connection;

    private ChargePointState state = ChargePointState.CONNECTED;
    private String vendor;
    private String model;
    private String currentUniqueId;

    public ChargePointSession(String chargePointId, WebSocket connection) {
        this.chargePointId = chargePointId;
        this.connection = connection;
    }

    /**
     * 메시지 전송
     */
    public void send(OcppMessage message) {
        if (connection.isOpen()) {
            connection.send(message.toJson());
        }
    }

    // Getters
    public String getChargePointId() {
        return chargePointId;
    }

    public WebSocket getConnection() {
        return connection;
    }

    public ChargePointState getState() {
        return state;
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }

    public String getCurrentUniqueId() {
        return currentUniqueId;
    }

    // Setters
    public void setState(ChargePointState state) {
        this.state = state;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setCurrentUniqueId(String currentUniqueId) {
        this.currentUniqueId = currentUniqueId;
    }
}