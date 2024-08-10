package org.asansocketserver.domain.watch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asansocketserver.domain.image.entity.Coordinate;
import org.asansocketserver.domain.image.repository.CoordinateRepository;
import org.asansocketserver.domain.watch.dto.request.WatchRequestDto;
import org.asansocketserver.domain.watch.dto.request.WatchUpdateRequestDto;
import org.asansocketserver.domain.watch.dto.response.WatchAllResponseDto;
import org.asansocketserver.domain.watch.dto.response.WatchResponseDto;
import org.asansocketserver.domain.watch.dto.web.request.WatchNoContactedRequestDto;
import org.asansocketserver.domain.watch.dto.web.request.WatchProhibitedCoordinatesUpdateRequestDto;
import org.asansocketserver.domain.watch.dto.web.request.WatchUpdateRequestForWebDto;

import org.asansocketserver.domain.watch.dto.web.response.*;
import org.asansocketserver.domain.watch.entity.Watch;
import org.asansocketserver.domain.watch.entity.WatchLive;
import org.asansocketserver.domain.watch.repository.WatchLiveRepository;
import org.asansocketserver.domain.watch.repository.WatchRepository;
import org.asansocketserver.global.error.exception.ConflictException;
import org.asansocketserver.global.error.exception.EntityNotFoundException;
//import org.asansocketserver.socket.dto.MessageType;
//import org.asansocketserver.socket.dto.SocketBaseResponse;
//import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.asansocketserver.socket.dto.MessageType;
import org.asansocketserver.socket.dto.SocketBaseResponse;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.asansocketserver.global.error.ErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class WatchService {
    private final WatchRepository watchRepository;
    private final WatchLiveRepository watchLiveRepository;
    private final SimpMessageSendingOperations sendingOperations;
    private final CoordinateRepository coordinateRepository;

    public WatchResponseDto updateWatchInfo(Long watchId, WatchUpdateRequestDto watchUpdateRequestDto) {
        Watch watch = findByWatchIdOrThrow(watchId);
        watch.updateWatch(watchUpdateRequestDto);
        return WatchResponseDto.of(watch);
    }

    public WatchAllResponseDto findAllWatch() {
        List<Watch> watchList = findAllByWatch();
        List<WatchResponseDto> watchResponseDtoList = WatchResponseDto.listOf(watchList);
        return WatchAllResponseDto.of(watchResponseDtoList);
    }

    public Long deleteWatch(Long id) {
        Optional<Watch> watch = watchRepository.findById(id);
        Optional<WatchLive> watchLive = watchLiveRepository.findById(id);
        watch.ifPresent(watchRepository::delete);
        watchLive.ifPresent(watchLiveRepository::delete);
        sendingOperations.convertAndSend("/queue/sensor/9999999", SocketBaseResponse.of(MessageType.DEL_WATCH, id));
        return id;
    }


    public WatchResponseDto findWatch(String uuid) {
        Watch watch = findByWatchOrThrow(uuid);
        return WatchResponseDto.of(watch);
    }

    public WatchResponseForWebDto findWatchForWeb(String uuid) {
        Watch watch = findByWatchOrThrow(uuid);
        return WatchResponseForWebDto.of(watch);
    }

    public List<WatchWithHostDto> getWatchWithHost() {
        List<Watch> watchList = watchRepository.findAll();

        Map<String, WatchWithHostDto> watchMap = new HashMap<>();

        WatchWithHostDto watchWithHosts;

        List<WatchIdAndNameDto> watchIdAndNameList = new ArrayList<>();

        for (Watch watch : watchList) {

            watchWithHosts = watchMap.get(watch.getName());

            if (watchWithHosts == null) {
                watchWithHosts = WatchWithHostDto.of(watch.getHost(), new ArrayList<>());
                watchMap.put(watch.getHost(), watchWithHosts);
            }

            watchIdAndNameList = watchMap.get(watch.getHost()).watchList();
            watchIdAndNameList.add(WatchIdAndNameDto.of(watch.getId(), watch.getName()));

            watchWithHosts = WatchWithHostDto.of(watch.getHost(),watchIdAndNameList);
            watchMap.put(watch.getHost(), watchWithHosts);

        }
        return new ArrayList<>(watchMap.values());
    }

    public WatchResponseDto createWatch(WatchRequestDto watchRequestDto) {
        validateDuplicateWatch(watchRequestDto);
        Watch createdWatch = createWatchAndSave(watchRequestDto);
        Long newWatchId = watchRepository.findByUuid(watchRequestDto.uuid()).get().getId();
        sendingOperations.convertAndSend("/queue/sensor/9999999", SocketBaseResponse.of(MessageType.NEW_WATCH, newWatchId));
        return WatchResponseDto.of(createdWatch);
    }

    public WatchResponseForWebDto updateWatchInfoForWeb(Long watchId, WatchUpdateRequestForWebDto watchUpdateRequestDto) {
        Watch watch = findByWatchIdOrThrow(watchId);
        watch.updateWatchForWeb(watchUpdateRequestDto);
        watchRepository.save(watch);
        System.out.println("watchUpdateRequestDto.name() = " + watchUpdateRequestDto.name());
        return WatchResponseForWebDto.of(watch);
    }

    private Watch createWatchAndSave(WatchRequestDto watchRequestDto) {
        Watch createdWatch = Watch.createWatch(watchRequestDto.uuid(), watchRequestDto.device());
        return watchRepository.save(createdWatch);
    }

    private void validateDuplicateWatch(WatchRequestDto watchRequestDto) {
        if (watchRepository.existsByUuid(watchRequestDto.uuid()))
            throw new ConflictException(DUPLICATE_WATCH_UUID);
    }

    private Watch findByWatchIdOrThrow(Long id) {
        return watchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(WATCH_NOT_FOUND));
    }

    private Watch findByWatchOrThrow(String uuid) {
        return watchRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(WATCH_UUID_NOT_FOUND));
    }

    private List<Watch> findAllByWatch() {
        return watchRepository.findAll();
    }

    @Transactional
    public WatchNoContactResponseDto updateNoContactWatchList(WatchNoContactedRequestDto requestDto) {
        Optional<Watch> watchOptional = watchRepository.findById(requestDto.getWatchId());
        if (watchOptional.isEmpty()) {
            throw new IllegalArgumentException(requestDto.getWatchId() + "번 워치는 존재하지않습니다.");
        }

        Watch watch = watchOptional.get();
        watch.getNoContactWatchList().clear();

        for (Long noContactWatchId : requestDto.getNoContactWatchIds()) {
            Optional<Watch> noContactWatchOptional = watchRepository.findById(noContactWatchId);
            if (noContactWatchOptional.isEmpty()) {
                throw new IllegalArgumentException(requestDto.getWatchId() + "번 워치는 존재하지않아 접촉 금지 대상에 지정할 수 없습니다.");
            }
            watch.addNoContactWatch(noContactWatchOptional.get());
        }

        WatchNoContactResponseDto responseDto = new WatchNoContactResponseDto();
        responseDto.setWatchId(watch.getId());
        responseDto.setNoContactWatchIds(requestDto.getNoContactWatchIds());

        return responseDto;
    }

    public WatchProhibitedCoordinatesUpdateResponseDto updateProhibitedCoordinateList(WatchProhibitedCoordinatesUpdateRequestDto requestDto) {
        Optional<Watch> watchOptional = watchRepository.findById(requestDto.watchId());
        if (watchOptional.isEmpty()) {
            throw new IllegalArgumentException(requestDto.watchId() + "번 워치는 존재하지않습니다.");
        }

        Watch watch = watchOptional.get();
        watch.getProhibitedCoordinateList().clear();

        for(Long prohibitedCoordinate : requestDto.prohibitedCoordinatesIds()){
            Optional<Coordinate> prohibitedCoordinateOptional = coordinateRepository.findById(prohibitedCoordinate);
            if (prohibitedCoordinateOptional.isEmpty()) {
                throw new IllegalArgumentException(requestDto.watchId() + "번 위치(좌표)는 존재하지않습니다.");
            }
            watch.addProhibitedCoordinate(prohibitedCoordinateOptional.get());
        }


        return WatchProhibitedCoordinatesUpdateResponseDto.of(watch.getId() , requestDto.prohibitedCoordinatesIds());


    }
}
