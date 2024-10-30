package com.diagram.ai.core;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import net.sourceforge.plantuml.ErrorUmlType;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.state.AgentState;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.bsc.langgraph4j.utils.CollectionsUtils.*;

public interface ImageToDiagram {
    class State extends AgentState {
        public State(Map<String, Object> initData) {
            super(initData);
        }

        public Optional<Diagram.Element> diagram() {
            return value("diagram");
        }

        public List<String> diagramCode() {
            return this.<List<String>>value("diagramCode").orElseGet(Collections::emptyList);
        }

        public Optional<EvaluationResult> evaluationResult() {
            return value("evaluationResult");
        }

        public Optional<String> evaluationError() {
            return value("evaluationError");
        }

        public Optional<ErrorUmlType> evaluationErrorType() {
            return value("evaluationErrorType");
        }

        public boolean isExecutionError() {
            return evaluationErrorType()
                    .map(type -> type == ErrorUmlType.EXECUTION_ERROR)
                    .orElse(false);
        }

        public boolean lastTwoDiagramsAreEqual() {
            if (diagramCode().size() < 2) {
                return false;
            }

            String last = last(diagramCode())
                    .map(String::trim)
                    .orElseThrow(() -> new IllegalStateException("last() is null!"));
            String prev = lastMinus(diagramCode(), 1)
                    .map(String::trim)
                    .orElseThrow(() -> new IllegalStateException("last(-1) is null!"));

            return last.equals(prev);
        }
    }

    enum EvaluationResult {
        OK,
        ERROR,
        UNKNOWN
    }

    OpenAiChatModel getChatModel();

    default PromptTemplate loadPromptTemplate(String resourceName) throws Exception {
        final ClassLoader classLoader = getClass().getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream(resourceName);

        if (inputStream == null) {
            throw new IllegalArgumentException("File not found: " + resourceName);
        }

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 4*1024)) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
            return PromptTemplate.from(result.toString());
        }
    }

    AsyncGenerator<NodeOutput<State>> execute(Map<String, Object> inputs) throws Exception;

}
