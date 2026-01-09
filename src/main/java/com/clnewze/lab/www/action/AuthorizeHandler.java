package com.clnewze.lab.www.action;

import com.clnewze.lab.www.protocol.CallResult;
import com.clnewze.lab.www.protocol.OcppMessage;
import com.clnewze.lab.www.session.ChargePointSession;
import com.google.gson.JsonObject;

/**
 * Authorize 핸들러
 * RFID 태그 인증 요청 처리
 */
public class AuthorizeHandler implements ActionHandler {

    @Override
    public String getAction() {
        return "Authorize";
    }

    @Override
    public OcppMessage handle(ChargePointSession session, JsonObject payload) {
        String idTag = payload.get("idTag").getAsString();

        System.out.println("[Authorize] " + session.getChargePointId() +
                " - IdTag: " + idTag);

        // 실제로는 DB에서 idTag 검증
        // 연습용이므로 무조건 Accepted
        String status = validateIdTag(idTag);

        // 응답 생성
        JsonObject idTagInfo = new JsonObject();
        idTagInfo.addProperty("status", status);
        // 선택: expiryDate, parentIdTag

        JsonObject response = new JsonObject();
        response.add("idTagInfo", idTagInfo);

        return new CallResult(session.getCurrentUniqueId(), response);
    }

    /**
     * IdTag 검증 (연습용: 무조건 Accepted)
     * 실제로는 DB 조회
     */
    private String validateIdTag(String idTag) {
        // 테스트용: "BLOCKED"로 시작하면 차단
        if (idTag.startsWith("BLOCKED")) {
            return "Blocked";
        }
        // 테스트용: "EXPIRED"로 시작하면 만료
        if (idTag.startsWith("EXPIRED")) {
            return "Expired";
        }
        // 테스트용: "INVALID"로 시작하면 유효하지 않음
        if (idTag.startsWith("INVALID")) {
            return "Invalid";
        }
        // 그 외: 승인
        return "Accepted";
    }
}