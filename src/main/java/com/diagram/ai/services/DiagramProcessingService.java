package com.diagram.ai.services;

import com.diagram.ai.core.Diagram;
import com.diagram.ai.core.ImageToDiagram;
import com.diagram.ai.core.ToDiagramProcess;
import com.diagram.ai.exceptions.InvalidDataException;
import com.diagram.ai.model.DiagramResponse;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.utils.CollectionsUtils.mapOf;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiagramProcessingService {
    private static final Gson GSON = new Gson();

    @Value("${ai.base-url}")
    private String baseUrl;
    @Value("${ai.api-key}")
    private String apiKey;
    @Value("${ai.model-name}")
    private String modelName;

    public DiagramResponse plantImageToDiagram(MultipartFile file) {
        try {
            return toDiagram(Base64.getEncoder().encodeToString(file.getBytes()), true);
        } catch (IOException e) {
            throw new InvalidDataException(e);
        }
    }

    public DiagramResponse plantTextToDiagram(Diagram.Element element) {
        return toDiagram(GSON.toJson(element), false);
    }

    private DiagramResponse toDiagram(String diagramDescription, boolean isImage) {
        try {
            var agentExecutor = new ToDiagramProcess(diagramDescription, isImage, baseUrl, apiKey, modelName);
            var result = agentExecutor.execute(mapOf());

            AtomicReference<ImageToDiagram.State> state = new AtomicReference<>();

            result.stream().forEach(stateNodeOutput -> {
                if (Objects.nonNull(stateNodeOutput.state())) {
                    state.set(stateNodeOutput.state());
                }
            });

            return new DiagramResponse(state.get().diagramCode().getFirst());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new InvalidDataException(e);
        }
    }
}
