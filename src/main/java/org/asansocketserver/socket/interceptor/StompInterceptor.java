package org.asansocketserver.socket.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.asansocketserver.domain.position.entity.Position;
import org.asansocketserver.domain.position.mongorepository.PositionMongoRepository;
import org.asansocketserver.domain.sensor.entity.sensorData.SensorData;
import org.asansocketserver.domain.sensor.repository.SensorDataRepository;
import org.asansocketserver.domain.sensor.scheduler.SensorScheduler;
import org.asansocketserver.domain.watch.entity.Watch;
import org.asansocketserver.domain.watch.entity.WatchLive;
import org.asansocketserver.domain.watch.repository.WatchLiveRepository;
import org.asansocketserver.domain.watch.repository.WatchRepository;
import org.asansocketserver.socket.error.SocketException;
import org.asansocketserver.socket.error.SocketNotFoundException;
import org.asansocketserver.socket.error.SocketUnauthorizedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.asansocketserver.socket.error.SocketErrorCode.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompInterceptor implements ChannelInterceptor  {
    public final static Long monitoringId = 9999999L;
    private final WatchRepository watchRepository;
    private final SensorDataRepository sensorDataRepository;
    private final PositionMongoRepository positionMongoRepository;
    private final WatchLiveRepository watchLiveRepository;
    private final SensorScheduler sensorScheduler;
    private final CacheManager cacheManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
//        log.info("[command]:: watchId : " + command);

        if (StompCommand.SUBSCRIBE.equals(command)) {
            sensorScheduler.broadcastWatchList();
        }

        if (StompCommand.CONNECT.equals(command)) {
            Long watchId = getWatchByAuthorizationHeader(accessor);
            log.info(watchId.toString());
            setWatchIdFromStompHeader(accessor, watchId);
            Optional<Watch> watch = watchRepository.findById(watchId);
            if (!watchId.equals(monitoringId)) {
                createWatchLiveAndSave(watchId);
                createSensorDataAndSave(watchId , watch.get().getName());
                createPositionAndSave(watchId);
            }
            log.info("[CONNECT]:: watchId : " + watchId);
            sensorScheduler.broadcastWatchList();



        } else if (StompCommand.DISCONNECT.equals(command)) {
            Long watchId = (Long) getWatchIdFromStompHeader(accessor);
            if (existWatchInRedis(watchId) && !watchId.equals(monitoringId)) {
                deleteWatchIdFromStompHeader(accessor);
                deleteWatchInRedis(watchId);

                Cache cache = cacheManager.getCache("notifications");
                if (cache instanceof ConcurrentMapCache) {
                    ConcurrentMap<Object, Object> nativeCache = ((ConcurrentMapCache) cache).getNativeCache();
                    nativeCache.keySet().removeIf(key -> key.toString().matches("^" + watchId + ":.*"));
                }


                Optional<Watch> watch = watchRepository.findById(watchId);
                watch.ifPresent(value -> {
                    value.updateCurrentLocation(null);
                    watchRepository.save(value);
                });

                sensorScheduler.sendDisconnectWatch(watchId);
            }
            sensorScheduler.broadcastWatchList();
            log.info("DISCONNECTED watchId : {}", watchId);
        }



        return message;
    }

    private Long getWatchByAuthorizationHeader(StompHeaderAccessor accessor) {
        String authHeaderValue = accessor.getFirstNativeHeader("Authorization");
        long longAuthHeaderValue = Long.parseLong(Objects.requireNonNull(authHeaderValue));
        if (longAuthHeaderValue != monitoringId) {
            validateAuthorizationHeader(authHeaderValue);
            validateExistWatch(authHeaderValue);
        }
        return longAuthHeaderValue;
    }

    private Object getWatchIdFromStompHeader(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = getSessionAttributes(accessor);
        Object value = sessionAttributes.get("watchId");
        if (Objects.isNull(value))
            throw new SocketException(SOCKET_SERVER_ERROR);
        return value;
    }

    private void setWatchIdFromStompHeader(StompHeaderAccessor accessor, Object value) {
        Map<String, Object> sessionAttributes = getSessionAttributes(accessor);
        if (Objects.isNull(sessionAttributes)) return;
        sessionAttributes.put("watchId", value);
    }

    private void deleteWatchIdFromStompHeader(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = getSessionAttributes(accessor);
        if (Objects.isNull(sessionAttributes)) return;
        sessionAttributes.remove("watchId");
    }

    private Map<String, Object> getSessionAttributes(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (Objects.isNull(sessionAttributes))
            return null;
        return sessionAttributes;
    }

    private void createWatchLiveAndSave(Long watchId) {
        WatchLive watchLive = WatchLive.createWatchLive(watchId);
        watchLiveRepository.save(watchLive);
    }

    private void createSensorDataAndSave(Long watchId ,String watchName) {
        if (sensorDataRepository.existsByWatchIdAndDate(watchId, LocalDate.now())) return;
        SensorData sensorData = SensorData.createSensorData(watchId, watchName);
        sensorDataRepository.save(sensorData);
    }

    private void createPositionAndSave(Long watchId) {
        if (positionMongoRepository.existsByWatchIdAndDate(watchId, LocalDate.now())) return;
        Position position = Position.of(watchId);
        positionMongoRepository.save(position);
    }

    private void validateAuthorizationHeader(String authHeaderValue) {
        if (Objects.isNull(authHeaderValue) || authHeaderValue.isBlank())
            throw new SocketUnauthorizedException(BLINK_AUTHORIZED_HEADER);
    }

    private void validateExistWatch(String authHeaderValue) {
        if (!watchRepository.existsById(Long.parseLong(authHeaderValue)))
            throw new SocketNotFoundException(WATCH_NOT_FOUND);
    }

    private boolean existWatchInRedis(Long watchId) {
        return watchLiveRepository.existsById(watchId);
    }

    private void deleteWatchInRedis(Long watchId) {
        watchLiveRepository.deleteById(watchId);
    }




}

