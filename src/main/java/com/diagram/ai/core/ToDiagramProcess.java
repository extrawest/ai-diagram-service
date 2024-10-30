package com.diagram.ai.core;

import com.diagram.ai.exceptions.InvalidDataException;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.utils.CollectionsUtils.mapOf;

@Slf4j(topic = "ImageToDiagramProcess")
public class ToDiagramProcess implements ImageToDiagram {
    private static final String AGENT_DESCRIBER = "agent_describer";
    private static final String AGENT_GENERIC_PLANTUML = "agent_generic_plantuml";
    private static final String EVALUATE_RESULT = "evaluate_result";
    private static final String GENERIC = "generic";

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final String imageData;
    private final boolean isImage;

    @Getter(lazy = true)
    private final OpenAiChatModel chatModel = newLLM();

    public ToDiagramProcess(String imageData, boolean isImage, String baseUrl, String apiKey, String modelName) {
        this.imageData = imageData;
        this.isImage = isImage;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public AsyncGenerator<NodeOutput<State>> execute(Map<String, Object> inputs) throws Exception {
        var llmVision = getChatModel();

        var app = new StateGraph<>(State::new)
                .addNode(AGENT_DESCRIBER, node_async(state -> describeDiagramImage(llmVision, imageData, isImage)))
                .addNode(AGENT_GENERIC_PLANTUML, node_async(this::translateGenericDiagramDescriptionToPlantUML))
                .addConditionalEdges(
                        AGENT_DESCRIBER,
                        edge_async(state -> GENERIC),
                        mapOf(GENERIC, AGENT_GENERIC_PLANTUML)
                )
                .addNode(EVALUATE_RESULT, this::evaluateResult)
                .addEdge(AGENT_GENERIC_PLANTUML, EVALUATE_RESULT)
                .addEdge(START,AGENT_DESCRIBER)
                .addEdge(EVALUATE_RESULT, END)
                .compile();

        return app.stream(inputs);
    }

    private Map<String,Object> describeDiagramImage(ChatLanguageModel visionModel, String imageData, boolean isImage) throws Exception {
        if (isImage) {
            var systemPrompt = loadPromptTemplate("describe_diagram_image.txt").apply(mapOf());

            var imageContent = ImageContent.from(imageData, "image/png", ImageContent.DetailLevel.AUTO);
            var textContent = new TextContent(systemPrompt.text());
            var message = UserMessage.from(textContent, imageContent);

            var response = visionModel.generate(message);
            var outputParser = new DiagramOutputParser();
            Diagram.Element result = outputParser.parse(response.content().text());
            return mapOf("diagram", result);
        } else {
            var outputParser = new DiagramOutputParser();
            Diagram.Element result = outputParser.parse(imageData);
            return mapOf("diagram", result);
        }
    }

    private Map<String,Object> translateGenericDiagramDescriptionToPlantUML(State state) throws Exception {
        var diagram = state.diagram().orElseThrow(() -> new IllegalArgumentException("no diagram provided!"));

        var systemPrompt = loadPromptTemplate("convert_generic_diagram_to_plantuml.txt")
                .apply(mapOf("diagram_description", diagram));
        var response = getChatModel().generate(new SystemMessage(systemPrompt.text()));

        return mapOf("diagramCode", Collections.singletonList(response.content().text()));
    }

    private OpenAiChatModel newLLM() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(2))
                .logRequests(true)
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .maxTokens(2000)
                .build();

    }

    private CompletableFuture<Map<String,Object>> evaluateResult(State state) {
        CompletableFuture<Map<String,Object>> result = new CompletableFuture<>();

        var diagramCorrectionProcess = new DiagramCorrectionProcess(baseUrl, apiKey, modelName);

        var list = new ArrayList<NodeOutput<State>>();
        try {
            return diagramCorrectionProcess.execute(state.data())
                    .collectAsync(list, v -> log.info(v.toString()))
                    .thenApply(v -> {
                        if (list.isEmpty()) {
                            throw new InvalidDataException("no results");
                        }
                        var last = list.getLast();
                        return last.state().data();
                    });
        } catch (Exception e) {
            result.completeExceptionally(e);
        }

        return result;
    }
}
