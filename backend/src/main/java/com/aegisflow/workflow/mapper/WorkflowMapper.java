package com.aegisflow.workflow.mapper;

import com.aegisflow.workflow.dto.EdgeDto;
import com.aegisflow.workflow.dto.NodeDto;
import com.aegisflow.workflow.dto.WorkflowResponse;
import com.aegisflow.workflow.entity.WorkflowDefinition;
import com.aegisflow.workflow.entity.WorkflowEdge;
import com.aegisflow.workflow.entity.WorkflowNode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    @Mapping(target = "nodes", source = "nodes")
    @Mapping(target = "edges", source = "edges")
    WorkflowResponse toResponse(WorkflowDefinition entity);

    List<WorkflowResponse> toResponseList(List<WorkflowDefinition> entities);

    NodeDto toNodeDto(WorkflowNode node);

    @Mapping(target = "sourceNodeKey", expression = "java(edge.getSourceNode().getNodeKey())")
    @Mapping(target = "targetNodeKey", expression = "java(edge.getTargetNode().getNodeKey())")
    EdgeDto toEdgeDto(WorkflowEdge edge);
}
