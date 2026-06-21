package com.aegisflow.workflow.service;

import com.aegisflow.workflow.dto.EdgeDto;
import com.aegisflow.workflow.dto.NodeDto;
import com.aegisflow.workflow.dto.ValidationResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DagValidationServiceTest {

    private DagValidationService service;

    @BeforeEach
    void setUp() {
        service = new DagValidationService();
    }

    @Test
    void shouldPassValidDag() {
        List<NodeDto> nodes = List.of(
                new NodeDto(null, "start", "Start", "HTTP", null, null, null, 0.0, 0.0),
                new NodeDto(null, "process", "Process", "JAVA", null, null, null, 100.0, 100.0),
                new NodeDto(null, "end", "End", "HTTP", null, null, null, 200.0, 200.0)
        );
        List<EdgeDto> edges = List.of(
                new EdgeDto("start", "process"),
                new EdgeDto("process", "end")
        );

        ValidationResultDto result = service.validate(nodes, edges);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldDetectCycle() {
        List<NodeDto> nodes = List.of(
                new NodeDto(null, "a", "A", "HTTP", null, null, null, 0.0, 0.0),
                new NodeDto(null, "b", "B", "HTTP", null, null, null, 100.0, 0.0),
                new NodeDto(null, "c", "C", "HTTP", null, null, null, 200.0, 0.0)
        );
        List<EdgeDto> edges = List.of(
                new EdgeDto("a", "b"),
                new EdgeDto("b", "c"),
                new EdgeDto("c", "a")
        );

        ValidationResultDto result = service.validate(nodes, edges);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("cycle")));
    }

    @Test
    void shouldDetectDuplicateNodeKeys() {
        List<NodeDto> nodes = List.of(
                new NodeDto(null, "node1", "Node 1", "HTTP", null, null, null, 0.0, 0.0),
                new NodeDto(null, "node1", "Node 1 Dup", "HTTP", null, null, null, 100.0, 0.0)
        );

        ValidationResultDto result = service.validate(nodes, List.of());

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Duplicate")));
    }

    @Test
    void shouldDetectInvalidEdgeReference() {
        List<NodeDto> nodes = List.of(
                new NodeDto(null, "a", "A", "HTTP", null, null, null, 0.0, 0.0)
        );
        List<EdgeDto> edges = List.of(
                new EdgeDto("a", "nonexistent")
        );

        ValidationResultDto result = service.validate(nodes, edges);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("non-existent")));
    }

    @Test
    void shouldWarnDisconnectedNodes() {
        List<NodeDto> nodes = List.of(
                new NodeDto(null, "a", "A", "HTTP", null, null, null, 0.0, 0.0),
                new NodeDto(null, "b", "B", "HTTP", null, null, null, 100.0, 0.0),
                new NodeDto(null, "island", "Island", "HTTP", null, null, null, 200.0, 0.0)
        );
        List<EdgeDto> edges = List.of(
                new EdgeDto("a", "b")
        );

        ValidationResultDto result = service.validate(nodes, edges);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("island")));
    }

    @Test
    void shouldDetectSelfEdge() {
        List<NodeDto> nodes = List.of(
                new NodeDto(null, "a", "A", "HTTP", null, null, null, 0.0, 0.0)
        );
        List<EdgeDto> edges = List.of(
                new EdgeDto("a", "a")
        );

        ValidationResultDto result = service.validate(nodes, edges);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Self-referencing")));
    }

    @Test
    void shouldFailOnEmptyNodes() {
        ValidationResultDto result = service.validate(List.of(), List.of());

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("at least one node")));
    }
}
