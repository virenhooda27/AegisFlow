package com.aegisflow.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LLMProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(LLMProviderRegistry.class);

    private final Map<String, LLMProvider> providers;
    private final LLMProvider defaultProvider;

    public LLMProviderRegistry(List<LLMProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(LLMProvider::providerName, Function.identity()));
        this.defaultProvider = providers.getOrDefault("mock", providerList.getFirst());
        log.info("Registered {} LLM providers: {}. Default: {}",
                providers.size(), providers.keySet(), defaultProvider.providerName());
    }

    public LLMProvider getProvider(String name) {
        LLMProvider provider = providers.get(name);
        if (provider == null) {
            log.warn("LLM provider '{}' not found, falling back to default '{}'", name, defaultProvider.providerName());
            return defaultProvider;
        }
        return provider;
    }

    public LLMProvider getDefault() {
        return defaultProvider;
    }
}
