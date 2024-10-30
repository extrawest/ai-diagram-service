package com.diagram.ai.core;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

public class Diagram implements Serializable {

    @Data
    public static class Participant implements Serializable {
        private String name;
        private String shape;
        private String description;
    }

    @Data
    public static class Relation implements Serializable {
        private String source;
        private String target;
        private String description;
    }

    @Data
    public static class Container  implements Serializable {
        private String name;
        private List<String> children;
        private String description;
    }

    @Data
    public static class Element  implements Serializable {
        private String type;
        private String title;
        private List<Participant> participants;
        private List<Relation> relations;
        private List<Container> containers;
        private List<String> description;
    }

}