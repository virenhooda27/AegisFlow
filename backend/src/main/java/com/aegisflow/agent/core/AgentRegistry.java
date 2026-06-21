package com.aegisflow.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

    private final Map<String, Agent> agents;

    public AgentRegistry(List<Agent> agentList) {
        this.agents = agentList.stream()
                .collect(Collectors.toMap(Agent::agentType, Function.identity()));
        log.info("Registered {} agents: {}", agents.size(), agents.keySet());
    }

    public Agent getAgent(String type) {
        Agent agent = agents.get(type);
        if (agent == null) {
            throw new IllegalArgumentException("No agent registered for type: " + type);
        }
        return agent;
    }

    public Collection<Agent> getAllAgents() {
        return agents.values();
    }
}
