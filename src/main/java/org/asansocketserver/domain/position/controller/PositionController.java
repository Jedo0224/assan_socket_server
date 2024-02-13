package org.asansocketserver.domain.position.controller;


import lombok.RequiredArgsConstructor;
import org.asansocketserver.domain.position.dto.PosDataDTO;
import org.asansocketserver.domain.position.dto.PositionDTO;
import org.asansocketserver.domain.position.dto.StateDTO;
import org.asansocketserver.domain.position.service.PositionService;
import org.asansocketserver.global.common.SuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class PositionController {
    private final PositionService positionService;

    @DeleteMapping("/deleteBeacon")
    public ResponseEntity<SuccessResponse<?>> deleteBeacon(PositionDTO positionDTO) {
        positionService.deleteBeacon(positionDTO.getPosition());
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
}



