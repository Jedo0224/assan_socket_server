package org.asansocketserver.domain.sensor.dto.request;

public record HeartRateRequestDto(
        Integer value,
        String timeStamp
) {
}
