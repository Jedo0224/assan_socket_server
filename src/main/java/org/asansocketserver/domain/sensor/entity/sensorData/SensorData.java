package org.asansocketserver.domain.sensor.entity.sensorData;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
@Document(collection = "sensor_data")
public class SensorData {
    @MongoId
    @Field(name = "_id")
    private ObjectId id;
    @Field(name = "date")
    private LocalDate date;
    @Field(name = "watch_id")
    private Long watchId;
    @Field(name = "name")
    private String name;
    @Builder.Default
    List<SensorRow> sensorRowList = new ArrayList<>();

    public static SensorData createSensorData(Long watchId , String name) {
        return SensorData.builder()
                .date(LocalDate.now())
                .watchId(watchId)
                .name(name)
                .build();
    }

    public void updatedWatchName(String newName) {
        this.name = newName;
    }

    public void updateDate(LocalDate today)
        {this.date = today;
    }
}
