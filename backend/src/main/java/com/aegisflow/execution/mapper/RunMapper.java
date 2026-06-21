package com.aegisflow.execution.mapper;

import com.aegisflow.execution.dto.TaskRunResponse;
import com.aegisflow.execution.dto.WorkerNodeResponse;
import com.aegisflow.execution.dto.WorkflowRunResponse;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkerNode;
import com.aegisflow.execution.entity.WorkflowRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RunMapper {

    @Mapping(source = "workflow.id", target = "workflowId")
    @Mapping(source = "workflow.name", target = "workflowName")
    @Mapping(source = "workflow.version", target = "workflowVersion")
    @Mapping(source = "status", target = "status")
    WorkflowRunResponse toRunResponse(WorkflowRun run);

    @Mapping(source = "workflowNode.name", target = "nodeName")
    @Mapping(source = "workflowNode.type", target = "nodeType")
    @Mapping(source = "status", target = "status")
    TaskRunResponse toTaskRunResponse(TaskRun taskRun);

    List<TaskRunResponse> toTaskRunResponses(List<TaskRun> taskRuns);

    WorkerNodeResponse toWorkerResponse(WorkerNode worker);

    List<WorkerNodeResponse> toWorkerResponses(List<WorkerNode> workers);

    default String mapRunStatus(com.aegisflow.execution.entity.RunStatus status) {
        return status != null ? status.name() : null;
    }
}
