package org.asansocketserver.domain.position.util;

import org.asansocketserver.domain.position.entity.Position;
import org.asansocketserver.domain.position.entity.PositionData;

import java.util.List;
import java.util.Objects;

public class PositionExtractValue {
    public static PositionData getPositionDateFromPositionAndTime(Position position, String currentTime) {
        if (Objects.isNull(position))
            return null;
        List<PositionData> positionDataList = position.getPositionDataList();
        return positionDataList.stream()
                .filter(p -> currentTime.equals(p.getTimeStamp()))
                .findAny()
                .orElse(null);
    }
}
