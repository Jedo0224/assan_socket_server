package org.asansocketserver.domain.sensor.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.asansocketserver.domain.sensor.dto.request.LightRequestDto;
import org.asansocketserver.domain.sensor.entity.sensorType.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
@Document(collection = "sensor_light")
public class SensorLight {
    @Id
    @Field(name = "_id")
    private String id;
    @Field(name = "date")
    private LocalDate date;
    @Field(name = "watch_id")
    private Long watchId;
    @Field(name = "value")
    private Integer value;
    @Field(name = "timeStamp")
    private Long timeStamp;

    public static SensorLight createSensor(Long watchId, LightRequestDto lightRequestDto) {
        return SensorLight.builder()
                .date(LocalDate.now())
                .watchId(watchId)
                .value(lightRequestDto.value())
                .timeStamp(lightRequestDto.timeStamp())
                .build();
    }
}
