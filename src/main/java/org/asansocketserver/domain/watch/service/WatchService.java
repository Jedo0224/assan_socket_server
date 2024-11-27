package org.asansocketserver.domain.watch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asansocketserver.domain.sensor.entity.sensorData.SensorData;
import org.asansocketserver.domain.sensor.repository.SensorDataRepository;
import org.asansocketserver.domain.image.dto.CoordinateIDAndPositionDTO;
import org.asansocketserver.domain.image.dto.ImageIDAndPositionAndCoordinateDTO;
import org.asansocketserver.domain.image.entity.Coordinate;
import org.asansocketserver.domain.image.repository.CoordinateRepository;
import org.asansocketserver.domain.watch.dto.request.WatchRequestDto;
import org.asansocketserver.domain.watch.dto.request.WatchUpdateRequestDto;
import org.asansocketserver.domain.watch.dto.response.WatchAllResponseDto;
import org.asansocketserver.domain.watch.dto.response.WatchResponseDto;
import org.asansocketserver.domain.watch.dto.web.request.WatchNoContactedRequestDto;
import org.asansocketserver.domain.watch.dto.web.request.WatchProhibitedCoordinatesUpdateRequestDto;
import org.asansocketserver.domain.watch.dto.web.request.WatchTransferDto;
import org.asansocketserver.domain.watch.dto.web.request.WatchUpdateRequestForWebDto;
import org.asansocketserver.domain.watch.dto.web.response.*;
import org.asansocketserver.domain.watch.entity.Watch;
import org.asansocketserver.domain.watch.entity.WatchCoordinateProhibition;
import org.asansocketserver.domain.watch.entity.WatchLive;
import org.asansocketserver.domain.watch.entity.WatchNoContact;
import org.asansocketserver.domain.watch.repository.WatchLiveRepository;
import org.asansocketserver.domain.watch.repository.WatchNoContactRepository;
import org.asansocketserver.domain.watch.repository.WatchRepository;
import org.asansocketserver.global.error.exception.ConflictException;
import org.asansocketserver.global.error.exception.EntityNotFoundException;
//import org.asansocketserver.socket.dto.MessageType;
//import org.asansocketserver.socket.dto.SocketBaseResponse;
//import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.asansocketserver.socket.dto.MessageType;
import org.asansocketserver.socket.dto.SocketBaseResponse;
import org.bson.types.ObjectId;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.asansocketserver.global.error.ErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class WatchService {
    private final WatchRepository watchRepository;
    private final WatchLiveRepository watchLiveRepository;
    private final SimpMessageSendingOperations sendingOperations;
    private final WatchNoContactRepository watchNoContactRepository;
    private final CoordinateRepository coordinateRepository;
    private final SensorDataRepository sensorDataRepository;

    public WatchResponseDto updateWatchInfo(Long watchId, WatchUpdateRequestDto watchUpdateRequestDto) {
        Watch watch = findByWatchIdOrThrow(watchId);
        watch.updateWatch(watchUpdateRequestDto);

        boolean isDuplicateName = watchRepository.existsByNameAndIdNot(watchUpdateRequestDto.name(), watchId);
        if (isDuplicateName) {
            throw new IllegalArgumentException("중복 이름이 존재합니다.");
        }

        Optional<SensorData> sensorData = sensorDataRepository.findByWatchIdAndDate(watch.getId(), LocalDate.now());

        if (sensorData.isPresent()) {
            sensorData.get().updatedWatchName(watchUpdateRequestDto.name());
            sensorDataRepository.save(sensorData.get());
        }

        return WatchResponseDto.of(watch);
    }

    public WatchAllResponseDto findAllWatch() {
        List<Watch> watchList = findAllByWatch();
        List<WatchResponseDto> watchResponseDtoList = WatchResponseDto.listOf(watchList);
        return WatchAllResponseDto.of(watchResponseDtoList);
    }

    public WatchAllResponseForWebDto findAllWatchForWeb() {
        List<Watch> watchList = findAllByWatch();
        List<WatchResponseForWebDto> watchResponseDtoList = WatchResponseForWebDto.listOf(watchList);
        return WatchAllResponseForWebDto.of(watchResponseDtoList);
    }

    public Long deleteWatch(Long id) {

        Optional<Watch> watch = watchRepository.findById(id);
        Optional<WatchLive> watchLive = watchLiveRepository.findById(id);

        if (watch.isPresent()) {
            watchNoContactRepository.deleteAllByWatch(watch.get());
            watchNoContactRepository.deleteAllByNoContactWatch(watch.get());

            watchRepository.delete(watch.get());
        }



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

    public WatchResponseForWebDto findWatchByIdForWeb(Long watchId) {
        Watch watch = findByWatchIdOrThrow(watchId);
        return WatchResponseForWebDto.of(watch);
    }

//    public List<WatchWithHostDto> getWatchWithHost() {
//        List<Watch> watchList = watchRepository.findAll();
//
//        Map<String, WatchWithHostDto> watchMap = new HashMap<>();
//
//        WatchWithHostDto watchWithHosts;
//
//        List<WatchIdAndNameDto> watchIdAndNameList = new ArrayList<>();
//
//        for (Watch watch : watchList) {
//
//            watchWithHosts = watchMap.get(watch.getHost());
//
//            if (watchWithHosts == null) {
//                watchWithHosts = WatchWithHostDto.of(watch.getHost(), new ArrayList<>());
//                watchMap.put(watch.getHost(), watchWithHosts);
//            }
//
//            watchIdAndNameList = watchMap.get(watch.getHost()).watchList();
//            watchIdAndNameList.add(WatchIdAndNameDto.of(watch.getId(), watch.getName()));
//
//            watchWithHosts = WatchWithHostDto.of(watch.getHost(),watchIdAndNameList);
//            watchMap.put(watch.getHost(), watchWithHosts);
//
//        }
//        return new ArrayList<>(watchMap.values());
//    }

    public List<WatchIdAndNameDto> getWatchForNoContact() {
        List<Watch> watchList = watchRepository.findAll();
        List<WatchIdAndNameDto> watchIdAndNameDtos = new ArrayList<>();
        for (Watch watch : watchList) {
            WatchIdAndNameDto watchIdAndNameDto = new WatchIdAndNameDto(watch.getId(), watch.getName());
            watchIdAndNameDtos.add(watchIdAndNameDto);
        }
        return watchIdAndNameDtos;
    }

    public NoContactAndProhibitedIdWithNameDto getNoContactAndProhibitedIdWithName(Long watchId) {
        Watch watch = findByWatchIdOrThrow(watchId);

        List<WatchIdAndNameDto> watchIdAndNameDtos = getNoContactWatchDtos(watch);
        List<ImageIDAndPositionAndCoordinateDTO> coordinateIDAndPositionDtos = getProhibitedCoordinateDtos(watch);

        return NoContactAndProhibitedIdWithNameDto.of(watchIdAndNameDtos, coordinateIDAndPositionDtos);
    }

    private List<WatchIdAndNameDto> getNoContactWatchDtos(Watch watch) {
        return watch.getNoContactWatchList().stream()
                .map(watchNoContact -> WatchIdAndNameDto.of(
                        watchNoContact.getNoContactWatch().getId(),
                        watchNoContact.getNoContactWatch().getName()
                ))
                .collect(Collectors.toList());
    }

    private List<ImageIDAndPositionAndCoordinateDTO> getProhibitedCoordinateDtos(Watch watch) {
        return watch.getProhibitedCoordinateList().stream()
                .map(watchCoordinateProhibition -> ImageIDAndPositionAndCoordinateDTO.of(
                        watchCoordinateProhibition.getCoordinate().getImage().getId(),
                        watchCoordinateProhibition.getCoordinate().getId(),
                        watchCoordinateProhibition.getCoordinate().getPosition()
                ))
                .collect(Collectors.toList());
    }


    public WatchResponseDto createWatch(WatchRequestDto watchRequestDto) {
        validateDuplicateWatch(watchRequestDto);
        Watch createdWatch = createWatchAndSave(watchRequestDto);
        Long newWatchId = watchRepository.findByUuid(watchRequestDto.uuid()).get().getId();
        sendingOperations.convertAndSend("/queue/sensor/9999999", SocketBaseResponse.of(MessageType.NEW_WATCH, newWatchId));
        return WatchResponseDto.of(createdWatch);
    }


    public WatchResponseForWebDto updateWatchInfoForWeb(WatchUpdateRequestForWebDto watchUpdateRequestDto) {
        Long watchId = watchUpdateRequestDto.watchId();

        boolean isDuplicateName = watchRepository.existsByNameAndIdNot(watchUpdateRequestDto.name(), watchId);
        if (isDuplicateName) {
            throw new IllegalArgumentException("중복 이름이 존재합니다.");
        }

        List<Long> noContactWatchIds = watchUpdateRequestDto.noContactWatchIds();
        List<Long> prohibitedCoordinatesIds = watchUpdateRequestDto.prohibitedCoordinatesIds();

        Watch watch = findByWatchIdOrThrow(watchId);
        watch.updateWatchForWeb(watchUpdateRequestDto);

        Optional<SensorData> sensorData = sensorDataRepository.findByWatchIdAndDate(watchId, LocalDate.now());
        if (sensorData.isPresent()) {
            sensorData.get().updatedWatchName(watchUpdateRequestDto.name());
            sensorDataRepository.save(sensorData.get());
        }

        updateNoContactWatchList(watchId, noContactWatchIds);
        updateProhibitedCoordinateList(watchId, prohibitedCoordinatesIds);

        watchRepository.save(watch);
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
    public WatchNoContactResponseDto updateNoContactWatchList(Long watchId, List<Long> noContactWatchIds) {
        Optional<Watch> watchOptional = watchRepository.findById(watchId);
        if (watchOptional.isEmpty()) {
            throw new IllegalArgumentException(watchId + "번 워치는 존재하지않습니다.");
        }

        Watch watch = watchOptional.get();
        watch.getNoContactWatchList().clear();

        for (Long noContactWatchId : noContactWatchIds) {
            Optional<Watch> noContactWatchOptional = watchRepository.findById(noContactWatchId);
            if (noContactWatchOptional.isEmpty()) {
                throw new IllegalArgumentException(watchId + "번 워치는 존재하지않아 접촉 금지 대상에 지정할 수 없습니다.");
            }
            watch.addNoContactWatch(noContactWatchOptional.get());
        }

        WatchNoContactResponseDto responseDto = new WatchNoContactResponseDto();
        responseDto.setWatchId(watch.getId());
        responseDto.setNoContactWatchIds(noContactWatchIds);

        return responseDto;
    }

    public WatchProhibitedCoordinatesUpdateResponseDto updateProhibitedCoordinateList(Long watchId, List<Long> prohibitedCoordinatesIds) {
        Optional<Watch> watchOptional = watchRepository.findById(watchId);
        if (watchOptional.isEmpty()) {
            throw new IllegalArgumentException(watchId + "번 워치는 존재하지않습니다.");
        }

        Watch watch = watchOptional.get();
        watch.getProhibitedCoordinateList().clear();

        for(Long prohibitedCoordinate : prohibitedCoordinatesIds){
            Optional<Coordinate> prohibitedCoordinateOptional = coordinateRepository.findById(prohibitedCoordinate);
            if (prohibitedCoordinateOptional.isEmpty()) {
                throw new IllegalArgumentException(watchId + "번 위치(좌표)는 존재하지않습니다.");
            }
            watch.addProhibitedCoordinate(prohibitedCoordinateOptional.get());
        }


        return WatchProhibitedCoordinatesUpdateResponseDto.of(watch.getId() , prohibitedCoordinatesIds);


    }

    public WatchResponseForWebDto transferWatchInfo(WatchTransferDto requestDto) {
        Watch sendWatch = findByWatchIdOrThrow(requestDto.sendInfoId());
        Watch receiveWatch = findByWatchIdOrThrow(requestDto.receiveInfoId());

        String sendWatchName = sendWatch.getName();

        // 필요한 정보들을 이월
        receiveWatch.updateWatchForTransfer(sendWatch);

        watchNoContactRepository.deleteAllByWatch(sendWatch);
        watchNoContactRepository.deleteAllByNoContactWatch(sendWatch);



        // 업데이트된 receiveWatch 저장
        watchRepository.save(receiveWatch);

        // 이월 후 sendWatch 삭제
        watchRepository.delete(sendWatch);

        // 한국 시간(LocalDate.now())을 UTC로 변환하여 조회
        LocalDateTime nowInKST = LocalDateTime.now();
        ZonedDateTime utcDateTime = nowInKST.atZone(ZoneId.of("Asia/Seoul")).withZoneSameInstant(ZoneOffset.UTC);
        LocalDate utcDate = utcDateTime.toLocalDate();

        Optional<SensorData> sensorData = sensorDataRepository.findByWatchIdAndDate(requestDto.receiveInfoId(), utcDate);
        if (sensorData.isPresent()) {
            SensorData existingSensorData = sensorData.get();

            if (existingSensorData.getId() == null) {
                throw new IllegalStateException("SensorData _id is null. Cannot update without _id.");
            }


            existingSensorData.updatedWatchName(sendWatchName);
            sensorDataRepository.save(existingSensorData);  // 업데이트된 엔티티를 저장
        }

        return WatchResponseForWebDto.of(receiveWatch);
    }

}
