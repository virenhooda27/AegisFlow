package com.aegisflow.execution.controller;

import com.aegisflow.execution.service.ReplayEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/replay")
public class ReplayController {

    private final ReplayEngine replayEngine;

    public ReplayController(ReplayEngine replayEngine) {
        this.replayEngine = replayEngine;
    }

    @GetMapping("/{runId}")
    public ResponseEntity<ReplayEngine.ReplayState> replay(@PathVariable UUID runId) {
        return ResponseEntity.ok(replayEngine.replay(runId));
    }
}
