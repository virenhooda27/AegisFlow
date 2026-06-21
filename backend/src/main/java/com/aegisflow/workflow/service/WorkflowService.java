package com.aegisflow.workflow.service;

import com.aegisflow.common.exception.ResourceNotFoundException;
import com.aegisflow.workflow.dto.EdgeDto;
import com.aegisflow.workflow.dto.NodeDto;
import com.aegisflow.workflow.dto.ValidationResultDto;
import com.aegisflow.workflow.dto.WorkflowCreateRequest;
import com.aegisflow.workflow.dto.WorkflowResponse;
import com.aegisflow.workflow.dto.WorkflowUpdateRequest;
import com.aegisflow.workflow.entity.WorkflowDefinition;
import com.aegisflow.workflow.entity.WorkflowEdge;
import com.aegisflow.workflow.entity.WorkflowNode;
import com.aegisflow.workflow.mapper.WorkflowMapper;
import com.aegisflow.workflow.repository.WorkflowDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowDefinitionRepository workflowRepository;
    private final WorkflowMapper workflowMapper;
    private final DagValidationService dagValidationService;

    public WorkflowService(WorkflowDefinitionRepository workflowRepository,
                           WorkflowMapper workflowMapper,
                           DagValidationService dagValidationService) {
        this.workflowRepository = workflowRepository;
        this.workflowMapper = workflowMapper;
        this.dagValidationService = dagValidationService;
    }

    @Transactional
    public WorkflowResponse createWorkflow(WorkflowCreateRequest request) {
        log.info("Creating workflow: {}", request.name());

        int nextVersion = workflowRepository.findMaxVersionByName(request.name()) + 1;

        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .name(request.name())
                .description(request.description())
                .version(nextVersion)
                .build();

        addNodesAndEdges(workflow, request.nodes(), request.edges());

        WorkflowDefinition saved = workflowRepository.save(workflow);
        log.info("Created workflow '{}' version {}", saved.getName(), saved.getVersion());
        return workflowMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> getAllWorkflows() {
        List<WorkflowDefinition> workflows = workflowRepository.findAllLatestVersions();
        return workflowMapper.toResponseList(workflows);
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(UUID id) {
        WorkflowDefinition workflow = findWorkflowOrThrow(id);
        return workflowMapper.toResponse(workflow);
    }

    @Transactional
    public WorkflowResponse updateWorkflow(UUID id, WorkflowUpdateRequest request) {
        WorkflowDefinition existing = findWorkflowOrThrow(id);
        log.info("Updating workflow '{}', creating new version", existing.getName());

        int nextVersion = workflowRepository.findMaxVersionByName(existing.getName()) + 1;

        WorkflowDefinition newVersion = WorkflowDefinition.builder()
                .name(request.name())
                .description(request.description())
                .version(nextVersion)
                .build();

        addNodesAndEdges(newVersion, request.nodes(), request.edges());

        WorkflowDefinition saved = workflowRepository.save(newVersion);
        log.info("Created new version {} for workflow '{}'", saved.getVersion(), saved.getName());
        return workflowMapper.toResponse(saved);
    }

    @Transactional
    public void deleteWorkflow(UUID id) {
        WorkflowDefinition workflow = findWorkflowOrThrow(id);
        log.info("Deleting workflow '{}' version {}", workflow.getName(), workflow.getVersion());
        workflowRepository.delete(workflow);
    }

    public ValidationResultDto validateWorkflow(UUID id) {
        WorkflowDefinition workflow = findWorkflowOrThrow(id);
        List<NodeDto> nodes = workflow.getNodes().stream()
                .map(workflowMapper::toNodeDto)
                .toList();
        List<EdgeDto> edges = workflow.getEdges().stream()
                .map(workflowMapper::toEdgeDto)
                .toList();
        return dagValidationService.validate(nodes, edges);
    }

    private WorkflowDefinition findWorkflowOrThrow(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", id));
    }

    private void addNodesAndEdges(WorkflowDefinition workflow,
                                   List<NodeDto> nodeDtos,
                                   List<EdgeDto> edgeDtos) {
        if (nodeDtos == null) return;

        Map<String, WorkflowNode> nodeMap = new HashMap<>();

        for (NodeDto dto : nodeDtos) {
            WorkflowNode node = WorkflowNode.builder()
                    .nodeKey(dto.nodeKey())
                    .name(dto.name())
                    .type(dto.type())
                    .config(dto.config())
                    .timeoutSeconds(dto.timeoutSeconds())
                    .retryPolicy(dto.retryPolicy())
                    .positionX(dto.positionX())
                    .positionY(dto.positionY())
                    .build();
            workflow.addNode(node);
            nodeMap.put(dto.nodeKey(), node);
        }

        if (edgeDtos == null) return;

        for (EdgeDto dto : edgeDtos) {
            WorkflowNode source = nodeMap.get(dto.sourceNodeKey());
            WorkflowNode target = nodeMap.get(dto.targetNodeKey());

            if (source == null || target == null) {
                throw new IllegalArgumentException(
                        "Edge references non-existent node: source=%s, target=%s"
                                .formatted(dto.sourceNodeKey(), dto.targetNodeKey()));
            }

            WorkflowEdge edge = WorkflowEdge.builder()
                    .sourceNode(source)
                    .targetNode(target)
                    .build();
            workflow.addEdge(edge);
        }
    }
}
