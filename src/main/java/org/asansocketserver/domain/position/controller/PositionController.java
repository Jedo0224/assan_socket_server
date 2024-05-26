package org.asansocketserver.domain.position.controller;


import lombok.RequiredArgsConstructor;
import org.asansocketserver.domain.position.dto.PositionDTO;
import org.asansocketserver.domain.position.dto.request.GetStateDTO;
import org.asansocketserver.domain.position.dto.request.PositionNameDTO;
import org.asansocketserver.domain.position.dto.request.StateDTO;
import org.asansocketserver.domain.position.service.PositionService;
import org.asansocketserver.global.common.SuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class PositionController {
    private final PositionService positionService;


    @GetMapping("/countBeacon")
    public ResponseEntity<SuccessResponse<?>> countBeacon() {
        positionService.countBeacon();
        return SuccessResponse.ok(positionService.countBeacon());
    }

    @DeleteMapping("/deleteBeacon")
    public ResponseEntity<SuccessResponse<?>> deleteBeacon(@RequestBody PositionNameDTO positionNameDTO) {
        positionService.deleteBeacon(positionNameDTO.getPosition());
        return SuccessResponse.ok("success");
    }

    @PostMapping("/insertState")
    public ResponseEntity<SuccessResponse<?>> insertState(@RequestBody StateDTO stateDTO) {
        positionService.insertState(stateDTO);
        return SuccessResponse.ok("success");
    }

    @PostMapping("/deleteState")
    public ResponseEntity<SuccessResponse<?>> deleteState(@RequestBody StateDTO stateDTO) {
        positionService.deleteState(stateDTO);
        return SuccessResponse.ok("success");
    }

    @GetMapping("/getCollectionStatus/{id}")
    public ResponseEntity<SuccessResponse<?>> getCollectionStatus(@PathVariable Long id){
        return SuccessResponse.ok(positionService.getCollectionState(id));
    }
}



