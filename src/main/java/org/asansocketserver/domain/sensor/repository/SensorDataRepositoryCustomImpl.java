package org.asansocketserver.domain.sensor.repository;

import lombok.RequiredArgsConstructor;
import org.asansocketserver.domain.sensor.entity.sensorData.SensorData;
import org.asansocketserver.domain.sensor.entity.sensorData.SensorRow;
import org.asansocketserver.domain.watch.entity.Watch;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.util.Objects;

@RequiredArgsConstructor
public class SensorDataRepositoryCustomImpl implements SensorDataRepositoryCustom {
    private final MongoTemplate mongoTemplate;
    @Override
    public void updateAccelerometerData(Watch watch, SensorRow sensorRow) {
        if (isContainsSensorRow(watch, sensorRow))
            addSensorRowData(watch, sensorRow);
        else
            updateAccelerometerValue(watch, sensorRow);
    }

    @Override
    public void updateBarometerData(Watch watch, SensorRow sensorRow) {
        if (isContainsSensorRow(watch, sensorRow))
            addSensorRowData(watch, sensorRow);
        else
            updateBarometerValue(watch, sensorRow);
    }

    @Override
    public void updateGyroscopeData(Watch watch, SensorRow sensorRow) {
        if (isContainsSensorRow(watch, sensorRow))
            addSensorRowData(watch, sensorRow);
        else
            updateGyroscopeValue(watch, sensorRow);
    }

    @Override
    public void updateHeartRateData(Watch watch, SensorRow sensorRow) {
        if (isContainsSensorRow(watch, sensorRow))
            addSensorRowData(watch, sensorRow);
        else
            updateHeartRateValue(watch, sensorRow);
    }

    @Override
    public void updateLightData(Watch watch, SensorRow sensorRow) {
        if (isContainsSensorRow(watch, sensorRow))
            addSensorRowData(watch, sensorRow);
        else
            updateLightValue(watch, sensorRow);
    }

    private void addSensorRowData(Watch watch, SensorRow sensorRow) {
        Query query = new Query();
        Update update = new Update();
        query.addCriteria(Criteria.where("watchId").is(watch.getId())
                .and("date").is(LocalDate.now()));
        update.addToSet("sensorRowList", sensorRow);
        mongoTemplate.updateFirst(query, update, SensorData.class);
    }

    private boolean isContainsSensorRow(Watch watch, SensorRow sensorRow) {
        Query query = new Query(Criteria.where("watchId").is(watch.getId())
                .and("date").is(LocalDate.now())
                .and("sensorRowList.timeStamp").is(sensorRow.getTimestamp()));

        SensorData result = mongoTemplate.findOne(query, SensorData.class);
        return Objects.isNull(result);
    }

    private void updateAccelerometerValue(Watch watch, SensorRow sensorRow) {
        Query query = new Query(Criteria.where("watchId").is(watch.getId())
                .and("date").is(LocalDate.now())
                .and("sensorRowList.timeStamp").is(sensorRow.getTimestamp()));
        Update update = new Update()
                .set("sensorRowList.$.accX", sensorRow.getAccX())
                .set("sensorRowList.$.accY", sensorRow.getAccY())
                .set("sensorRowList.$.accZ", sensorRow.getAccZ());
        mongoTemplate.updateFirst(query, update, SensorData.class);
    }

    private void updateBarometerValue(Watch watch, SensorRow sensorRow) {
        Query query = new Query(Criteria.where("watchId").is(watch.getId())
                .and("date").is(LocalDate.now())
                .and("sensorRowList.timeStamp").is(sensorRow.getTimestamp()));
        Update update = new Update()
                .set("sensorRowList.$.barometerValue", sensorRow.getBarometerValue());
        mongoTemplate.updateFirst(query, update, SensorData.class);
    }

    private void updateGyroscopeValue(Watch watch, SensorRow sensorRow) {
        Query query = new Query(Criteria.where("watchId").is(watch.getId())
                .and("date").is(LocalDate.now())
                .and("sensorRowList.timeStamp").is(sensorRow.getTimestamp()));
        Update update = new Update()
                .set("sensorRowList.$.gyroX", sensorRow.getGyroX())
                .set("sensorRowList.$.gyroY", sensorRow.getGyroY())
                .set("sensorRowList.$.gyroZ", sensorRow.getGyroZ());
        mongoTemplate.updateFirst(query, update, SensorData.class);
    }

    private void updateHeartRateValue(Watch watch, SensorRow sensorRow) {
        Query query = new Query(Criteria.where("watchId").is(watch.getId())
                .and("date").is(LocalDate.now())
                .and("sensorRowList.timeStamp").is(sensorRow.getTimestamp()));
        Update update = new Update()
                .set("sensorRowList.$.heartRateValue", sensorRow.getHeartRateValue());
        mongoTemplate.updateFirst(query, update, SensorData.class);
    }

    private void updateLightValue(Watch watch, SensorRow sensorRow) {
        Query query = new Query(Criteria.where("watchId").is(watch.getId())
                .and("date").is(LocalDate.now())
                .and("sensorRowList.timeStamp").is(sensorRow.getTimestamp()));
        Update update = new Update()
                .set("sensorRowList.$.lightValue", sensorRow.getLightValue());
        mongoTemplate.updateFirst(query, update, SensorData.class);
    }
}
