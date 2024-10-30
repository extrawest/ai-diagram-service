package com.diagram.ai.controllers;

import com.diagram.ai.core.Diagram;
import com.diagram.ai.model.DiagramResponse;
import com.diagram.ai.services.DiagramProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
public class DiagramProcessingController {
    private final DiagramProcessingService service;

    @Operation(summary = "Ready to use")
    @PostMapping(
            path = "/plant/imageToDiagram",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DiagramResponse> plantImageToDiagram(@RequestPart MultipartFile file) {
        DiagramResponse result = service.plantImageToDiagram(file);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "Ready to use")
    @PostMapping( "/plant/textToDiagram")
    public ResponseEntity<DiagramResponse> plantTextToDiagram(@RequestBody Diagram.Element request) {
        DiagramResponse result = service.plantTextToDiagram(request);
        return ResponseEntity.ok().body(result);
    }

}
