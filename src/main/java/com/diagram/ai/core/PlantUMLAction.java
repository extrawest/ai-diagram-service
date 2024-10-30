package com.diagram.ai.core;

import lombok.Getter;
import net.sourceforge.plantuml.*;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.error.PSystemError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlantUMLAction {

    private PlantUMLAction() {}

    @Getter
    public static class Error extends Exception {
        private final ErrorUmlType type;

        public Error(String message, ErrorUmlType type) {
            super(message);
            this.type = type;
        }
    }

    public static <T> CompletableFuture<T> validate(String code) {
        CompletableFuture<T> result = new CompletableFuture<>();

        SourceStringReader reader = new SourceStringReader(code);

        final List<BlockUml> blocks = reader.getBlocks();
        if (blocks.size() != 1) {
            result.completeExceptionally(new IllegalArgumentException("Invalid PlantUML code"));
            return result;
        }

        final Diagram system = blocks.getFirst().getDiagram();

        if (system instanceof PSystemError error) {
            ErrorUml err = error.getFirstError();

            try (ByteArrayOutputStream png = new ByteArrayOutputStream()) {
                reader.outputImage(png, 0, new FileFormatOption(FileFormat.UTXT));
                result.completeExceptionally(new Error(png.toString(), err.getType()));
            } catch (IOException e) {
                result.completeExceptionally(e);
            }
        } else {
            result.complete(null);
        }

        return result;
    }
}
