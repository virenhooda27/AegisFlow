package com.aegisflow.workflow.controller;

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
class WorkflowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateWorkflow() throws Exception {
        String uniqueName = "test-wf-" + UUID.randomUUID().toString().substring(0, 8);
        WorkflowCreateRequest request = new WorkflowCreateRequest(
                uniqueName,
                "Test workflow description",
                List.of(
                        new NodeDto(null, "start", "Start Node", "HTTP", null, 30, null, 0.0, 0.0),
                        new NodeDto(null, "end", "End Node", "HTTP", null, null, null, 200.0, 0.0)
                ),
                List.of(new EdgeDto("start", "end"))
        );

        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(uniqueName)))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.nodes", hasSize(2)))
                .andExpect(jsonPath("$.edges", hasSize(1)));
    }

    @Test
    void shouldGetAllWorkflows() throws Exception {
        mockMvc.perform(get("/api/workflows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)));
    }

    @Test
    void shouldReturn404ForNonExistentWorkflow() throws Exception {
        mockMvc.perform(get("/api/workflows/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateWorkflow() throws Exception {
        String uniqueName = "validate-" + UUID.randomUUID().toString().substring(0, 8);
        WorkflowCreateRequest request = new WorkflowCreateRequest(
                uniqueName,
                "Validation test",
                List.of(
                        new NodeDto(null, "a", "A", "HTTP", null, null, null, 0.0, 0.0),
                        new NodeDto(null, "b", "B", "JAVA", null, null, null, 100.0, 0.0)
                ),
                List.of(new EdgeDto("a", "b"))
        );

        String response = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/workflows/" + id + "/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)));
    }

    @Test
    void shouldCreateNewVersionOnUpdate() throws Exception {
        String uniqueName = "versioned-" + UUID.randomUUID().toString().substring(0, 8);
        WorkflowCreateRequest createReq = new WorkflowCreateRequest(
                uniqueName,
                "v1",
                List.of(new NodeDto(null, "n1", "Node 1", "HTTP", null, null, null, 0.0, 0.0)),
                List.of()
        );

        String createResponse = mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version", is(1)))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(createResponse).get("id").asText();
        int v1 = objectMapper.readTree(createResponse).get("version").asInt();

        WorkflowCreateRequest updateReq = new WorkflowCreateRequest(
                uniqueName,
                "v2",
                List.of(
                        new NodeDto(null, "n1", "Node 1", "HTTP", null, null, null, 0.0, 0.0),
                        new NodeDto(null, "n2", "Node 2", "JAVA", null, null, null, 100.0, 0.0)
                ),
                List.of(new EdgeDto("n1", "n2"))
        );

        mockMvc.perform(put("/api/workflows/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", is(v1 + 1)))
                .andExpect(jsonPath("$.nodes", hasSize(2)));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "nodes": []
                }
                """;

        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
