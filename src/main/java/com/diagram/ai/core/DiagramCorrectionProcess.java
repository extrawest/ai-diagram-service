package com.diagram.ai.core;

import com.diagram.ai.exceptions.InvalidDataException;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.utils.CollectionsUtils.last;
import static org.bsc.langgraph4j.utils.CollectionsUtils.mapOf;

@Slf4j(topic = "DiagramCorrectionProcess")
public class DiagramCorrectionProcess implements ImageToDiagram {
    private static final String EVALUATE_RESULT = "evaluate_result";
    private static final String AGENT_REVIEW = "agent_review";

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;

    @Getter(lazy = true)
    private final OpenAiChatModel chatModel = newLLM();

    public DiagramCorrectionProcess(String baseUrl, String apiKey, String modelName) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public AsyncGenerator<NodeOutput<State>> execute(Map<String, Object> inputs) throws Exception {
        var workflow = new StateGraph<>(State::new);

        workflow.addNode(EVALUATE_RESULT, this::evaluateResult);
        workflow.addNode(AGENT_REVIEW, this::reviewResult);
        workflow.addEdge(AGENT_REVIEW, EVALUATE_RESULT);
        workflow.addConditionalEdges(
                EVALUATE_RESULT,
                edge_async(this::routeEvaluationResult),
                mapOf("OK", END, "ERROR", AGENT_REVIEW, "UNKNOWN", END)
        );
        workflow.setEntryPoint(EVALUATE_RESULT);

        var app = workflow.compile();

        return app.stream(inputs);
    }

    private CompletableFuture<Map<String,Object>> reviewResult(State state) {
        CompletableFuture<Map<String,Object>> future = new CompletableFuture<>();

        try {
            var diagramCode = last(state.diagramCode())
                    .orElseThrow(() -> new IllegalArgumentException("no diagram code provided!"));

            var error = state.evaluationError()
                    .orElseThrow(() -> new IllegalArgumentException("no evaluation error provided!"));

            Prompt systemPrompt = loadPromptTemplate("review_diagram.txt")
                    .apply(mapOf("evaluationError", error, "diagramCode", diagramCode));
            var response = getChatModel().generate(new SystemMessage(systemPrompt.text()));

            future.complete(mapOf("diagramCode", response.content().text()));
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    private CompletableFuture<Map<String,Object>> evaluateResult(State state) {
        var diagramCode = last(state.diagramCode())
                .orElseThrow(() -> new IllegalArgumentException("no diagram code provided!"));

        return PlantUMLAction.validate(diagramCode)
                .thenApply(v -> mapOf("evaluationResult", (Object) EvaluationResult.OK))
                .exceptionally(e -> {
                    if (e.getCause() instanceof PlantUMLAction.Error error) {
                        return mapOf("evaluationResult", EvaluationResult.ERROR,
                                "evaluationError", error.getMessage(),
                                "evaluationErrorType", error.getType());
                    }
                    throw new InvalidDataException(e);
                });

    }

    private String routeEvaluationResult(State state) {
        var evaluationResult = state.evaluationResult()
                .orElseThrow(() -> new IllegalArgumentException("no evaluationResult provided!"));

        if (evaluationResult == EvaluationResult.ERROR) {
            if (state.isExecutionError()) {
                log.warn("evaluation execution error: [{}]", state.evaluationError().orElse("unknown"));
                return EvaluationResult.UNKNOWN.name();
            }
            if (state.lastTwoDiagramsAreEqual()) {
                log.warn("correction failed! ");
                return EvaluationResult.UNKNOWN.name();
            }
        }

        return evaluationResult.name();
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
}
