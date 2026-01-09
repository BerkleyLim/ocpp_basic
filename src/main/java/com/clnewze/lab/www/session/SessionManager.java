package com.clnewze.lab.www.session;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 전체 세션 관리
 * chargePointId로 세션을 찾고 관리
 */
public class SessionManager {

    private final Map<String, ChargePointSession> sessions = new ConcurrentHashMap<>();

    /**
     * 세션 추가
     */
    public void addSession(ChargePointSession session) {
        sessions.put(session.getChargePointId(), session);
    }

    /**
     * 세션 제거
     */
    public void removeSession(String chargePointId) {
        sessions.remove(chargePointId);
    }

    /**
     * 세션 조회
     */
    public ChargePointSession getSession(String chargePointId) {
        return sessions.get(chargePointId);
    }

    /**
     * 전체 세션 목록
     */
    public Collection<ChargePointSession> getAllSessions() {
        return sessions.values();
    }

    /**
     * 현재 연결된 세션 수
     */
    public int getSessionCount() {
        return sessions.size();
    }
}