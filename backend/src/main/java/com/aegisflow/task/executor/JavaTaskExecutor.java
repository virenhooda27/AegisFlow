package com.aegisflow.task.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Component
public class JavaTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(JavaTaskExecutor.class);

    @Override
    public String getType() {
        return "JAVA";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        Map<String, Object> config = context.config();
        String className = (String) config.getOrDefault("className", "");
        String methodName = (String) config.getOrDefault("methodName", "execute");

        if (className.isBlank()) {
            return TaskResult.failure("Java task requires 'className' in config");
        }

        log.info("Executing Java {}.{} (attempt {})", className, methodName, context.attempt());

        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getMethod(methodName, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) method.invoke(instance, config);

            return TaskResult.success(result != null ? result : Map.of());
        } catch (ClassNotFoundException e) {
            return TaskResult.failure("Class not found: " + className);
        } catch (NoSuchMethodException e) {
            return TaskResult.failure("Method not found: " + methodName + "(Map) in " + className);
        } catch (Exception e) {
            log.error("Java task execution failed: {}", e.getMessage(), e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return TaskResult.failure("Java execution failed: " + message);
        }
    }
}
