package org.asansocketserver.domain.position.mongorepository;

import org.asansocketserver.domain.position.entity.Position;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

public interface PositionMongoRepository extends MongoRepository<Position, String>, PositionCustomRepository{
    boolean existsByWatchIdAndDate(Long watchId, LocalDate date);
    Position findOneByWatchIdAndDate(Long watchId, LocalDate date);
}
