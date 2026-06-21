package com.aegisflow.execution.controller;

import com.aegisflow.workflow.dto.EdgeDto;
import com.aegisflow.workflow.dto.NodeDto;
import com.aegisflow.workflow.dto.WorkflowCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RunControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createWorkflow() throws Exception {
        String name = "run-test-" + UUID.randomUUID().toString().substring(0, 8);
        WorkflowCreateRequest request = new WorkflowCreateRequest(
                name,
                "test",
                List.of(
                        new NodeDto(null, "start", "Start", "SHELL", null, 30, null, 0.0, 0.0),
                        new NodeDto(null, "end", "End", "SHELL", null, 30, null, 100.0, 0.0)
                ),
                List.of(new EdgeDto("start", "end"))
        );

        String response = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void shouldStartRun() throws Exception {
        String workflowId = createWorkflow();

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflowId\":\"" + workflowId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workflowId", is(workflowId)))
                .andExpect(jsonPath("$.status", is(notNullValue())))
                .andExpect(jsonPath("$.taskRuns", hasSize(2)));
    }

    @Test
    void shouldGetAllRuns() throws Exception {
        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)));
    }

    @Test
    void shouldGetRunById() throws Exception {
        String workflowId = createWorkflow();

        String runResponse = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflowId\":\"" + workflowId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String runId = objectMapper.readTree(runResponse).get("id").asText();

        mockMvc.perform(get("/api/runs/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(runId)))
                .andExpect(jsonPath("$.taskRuns", hasSize(2)));
    }

    @Test
    void shouldCancelRun() throws Exception {
        String workflowId = createWorkflow();

        String runResponse = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflowId\":\"" + workflowId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String runId = objectMapper.readTree(runResponse).get("id").asText();

        mockMvc.perform(post("/api/runs/" + runId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void shouldGetWorkers() throws Exception {
        mockMvc.perform(get("/api/runs/workers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("local-worker-1")));
    }
}
