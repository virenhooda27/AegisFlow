package com.aegisflow.task.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SqlTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(SqlTaskExecutor.class);

    private final JdbcTemplate jdbcTemplate;

    public SqlTaskExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getType() {
        return "SQL";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        Map<String, Object> config = context.config();
        String query = (String) config.getOrDefault("query", "");
        String type = (String) config.getOrDefault("queryType", "SELECT");

        if (query.isBlank()) {
            return TaskResult.failure("SQL task requires 'query' in config");
        }

        log.info("Executing SQL {} (attempt {})", type, context.attempt());

        try {
            if ("SELECT".equalsIgnoreCase(type)) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
                return TaskResult.success(Map.of(
                        "rowCount", rows.size(),
                        "rows", rows
                ));
            } else {
                int affected = jdbcTemplate.update(query);
                return TaskResult.success(Map.of(
                        "affectedRows", affected
                ));
            }
        } catch (Exception e) {
            log.error("SQL task execution failed: {}", e.getMessage(), e);
            return TaskResult.failure("SQL execution failed: " + e.getMessage());
        }
    }
}
