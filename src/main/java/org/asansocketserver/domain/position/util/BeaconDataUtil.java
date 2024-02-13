package org.asansocketserver.domain.position.util;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.asansocketserver.domain.position.dto.BeaconDataDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BeaconDataUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<BeaconDataDTO> parseBeaconData(String BeaconDataJson) {
        try {
            // JSON 문자열이 객체 리스트를 나타내는지 확인
            if (BeaconDataJson.trim().startsWith("[")) {
                return objectMapper.readValue(BeaconDataJson, new TypeReference<List<BeaconDataDTO>>() {
                });
            } else {
                // JSON 문자열이 단일 객체를 나타내면, 이를 리스트에 추가
                BeaconDataDTO singleData = objectMapper.readValue(BeaconDataJson, BeaconDataDTO.class);
                return Collections.singletonList(singleData);
            }
        } catch (Exception e) {
            // JSON 파싱 중 오류 발생 시 처리
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
