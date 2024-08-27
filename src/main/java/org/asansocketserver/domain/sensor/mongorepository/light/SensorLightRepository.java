package org.asansocketserver.domain.sensor.mongorepository.light;

import org.asansocketserver.domain.sensor.entity.SensorHeartRate;
import org.asansocketserver.domain.sensor.entity.SensorLight;
import org.asansocketserver.domain.sensor.mongorepository.SensorCustomRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface SensorLightRepository extends MongoRepository<SensorLight, String>, SensorLightCustomRepository {
    boolean existsByWatchIdAndDate(Long watchId, LocalDate date);

    List<SensorLight> findAllByWatchIdAndDate(Long watchId, LocalDate date);

    List<SensorLight> findAllByWatchIdAndDateBetween(int patientId, LocalDate localDate, LocalDate localDate1);


    SensorLight findTopByWatchIdAndDateOrderByTimestampDesc(Long id, LocalDate now);
}