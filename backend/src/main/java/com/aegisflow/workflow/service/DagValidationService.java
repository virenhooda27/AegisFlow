package com.aegisflow.workflow.service;

import com.aegisflow.workflow.dto.EdgeDto;
import com.aegisflow.workflow.dto.NodeDto;
import com.aegisflow.workflow.dto.ValidationResultDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DagValidationService {

    public ValidationResultDto validate(List<NodeDto> nodes, List<EdgeDto> edges) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (nodes == null || nodes.isEmpty()) {
            errors.add("Workflow must have at least one node");
            return new ValidationResultDto(false, errors, warnings);
        }

        checkDuplicateNodeKeys(nodes, errors);
        Set<String> nodeKeys = collectNodeKeys(nodes);
        checkInvalidEdgeReferences(edges, nodeKeys, errors);
        checkSelfEdges(edges, errors);

        if (errors.isEmpty() && edges != null && !edges.isEmpty()) {
            checkCycles(nodeKeys, edges, errors);
            checkDisconnectedNodes(nodeKeys, edges, warnings);
        }

        boolean valid = errors.isEmpty();
        return new ValidationResultDto(valid, errors, warnings);
    }

    private void checkDuplicateNodeKeys(List<NodeDto> nodes, List<String> errors) {
        Set<String> seen = new HashSet<>();
        for (NodeDto node : nodes) {
            if (!seen.add(node.nodeKey())) {
                errors.add("Duplicate node key: " + node.nodeKey());
            }
        }
    }

    private Set<String> collectNodeKeys(List<NodeDto> nodes) {
        Set<String> keys = new HashSet<>();
        for (NodeDto node : nodes) {
            keys.add(node.nodeKey());
        }
        return keys;
    }

    private void checkInvalidEdgeReferences(List<EdgeDto> edges, Set<String> nodeKeys, List<String> errors) {
        if (edges == null) return;
        for (EdgeDto edge : edges) {
            if (!nodeKeys.contains(edge.sourceNodeKey())) {
                errors.add("Edge references non-existent source node: " + edge.sourceNodeKey());
            }
            if (!nodeKeys.contains(edge.targetNodeKey())) {
                errors.add("Edge references non-existent target node: " + edge.targetNodeKey());
            }
        }
    }

    private void checkSelfEdges(List<EdgeDto> edges, List<String> errors) {
        if (edges == null) return;
        for (EdgeDto edge : edges) {
            if (edge.sourceNodeKey().equals(edge.targetNodeKey())) {
                errors.add("Self-referencing edge on node: " + edge.sourceNodeKey());
            }
        }
    }

    private void checkCycles(Set<String> nodeKeys, List<EdgeDto> edges, List<String> errors) {
        Map<String, List<String>> adjacency = new HashMap<>();
        for (String key : nodeKeys) {
            adjacency.put(key, new ArrayList<>());
        }
        for (EdgeDto edge : edges) {
            adjacency.get(edge.sourceNodeKey()).add(edge.targetNodeKey());
        }

        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();

        for (String node : nodeKeys) {
            if (!visited.contains(node)) {
                if (hasCycleDfs(node, adjacency, visited, inStack)) {
                    errors.add("Workflow contains a cycle");
                    return;
                }
            }
        }
    }

    private boolean hasCycleDfs(String node, Map<String, List<String>> adjacency,
                                 Set<String> visited, Set<String> inStack) {
        visited.add(node);
        inStack.add(node);

        for (String neighbor : adjacency.getOrDefault(node, List.of())) {
            if (inStack.contains(neighbor)) {
                return true;
            }
            if (!visited.contains(neighbor) && hasCycleDfs(neighbor, adjacency, visited, inStack)) {
                return true;
            }
        }

        inStack.remove(node);
        return false;
    }

    private void checkDisconnectedNodes(Set<String> nodeKeys, List<EdgeDto> edges, List<String> warnings) {
        Set<String> connectedNodes = new HashSet<>();
        for (EdgeDto edge : edges) {
            connectedNodes.add(edge.sourceNodeKey());
            connectedNodes.add(edge.targetNodeKey());
        }

        for (String key : nodeKeys) {
            if (!connectedNodes.contains(key)) {
                warnings.add("Node is disconnected: " + key);
            }
        }
    }
}
