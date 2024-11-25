package org.asansocketserver.domain.sensor.repository;


import org.asansocketserver.domain.sensor.entity.sensorData.SensorData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SensorDataRepository extends MongoRepository<SensorData, String>, SensorDataRepositoryCustom {
    boolean existsByWatchIdAndDate(Long watchId, LocalDate date);


    List<SensorData> findAllByWatchIdAndDateBetween(Long patientId, LocalDate localDate, LocalDate localDate1);
    @Query(value = "{ 'watch_id': ?0, 'date': ?1 }", fields = "{ 'name': 1, 'watch_id': 1, '_id': 1 }")
    Optional<SensorData> findByWatchIdAndDate(Long watchId, LocalDate date);


    List<SensorData> findAllByNameAndDate(String name, LocalDate now);

    List<SensorData> findAllByNameAndDateBetween(String name, LocalDate localDate, LocalDate localDate1);

    SensorData findByWatchId(Long aLong);
}
