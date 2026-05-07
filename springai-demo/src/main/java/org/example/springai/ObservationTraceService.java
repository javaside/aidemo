package org.example.springai;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 从 Micrometer MeterRegistry 收集并返回 Spring AI 5 层指标摘要。
 */
@Service
public class ObservationTraceService {

    private final MeterRegistry meterRegistry;

    public ObservationTraceService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 返回所有 spring_ai 和 gen_ai 相关指标，供 /ai/metrics 端点展示。
     */
    public List<Map<String, Object>> getMetricsSummary() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var meter : meterRegistry.getMeters()) {
            String name = meter.getId().getName();
            if (!name.startsWith("spring_ai") && !name.startsWith("gen_ai")) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("type", meter.getType().name());
            entry.put("tags", meter.getId().getTags().toString());

            if (meter instanceof Timer t) {
                entry.put("count", t.count());
                entry.put("mean_ms", t.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                entry.put("max_ms", t.max(java.util.concurrent.TimeUnit.MILLISECONDS));
            } else if (meter instanceof Counter c) {
                entry.put("count", c.count());
            }
            result.add(entry);
        }
        return result;
    }

    /**
     * 返回 5 层指标名称列表，对应关系供演示页面使用。
     */
    public List<Map<String, String>> getLayersDescription() {
        return List.of(
            Map.of("layer", "① spring.ai.chat.client", "metric", "spring_ai_chat_client_operation_seconds", "description", "ChatClient 层，包含 advisor 调用"),
            Map.of("layer", "② spring.ai.advisor", "metric", "spring_ai_advisor_operation_seconds", "description", "Advisor 拦截层"),
            Map.of("layer", "③ spring.ai.tool", "metric", "spring_ai_tool_operation_seconds", "description", "Tool calling 层"),
            Map.of("layer", "④ gen_ai.client.operation", "metric", "gen_ai_client_operation_seconds", "description", "ChatModel 层（DeepSeek API）"),
            Map.of("layer", "⑤ gen_ai.client.token.usage", "metric", "gen_ai_client_token_usage_total", "description", "Token 用量统计")
        );
    }
}