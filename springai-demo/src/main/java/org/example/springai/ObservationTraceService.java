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
            if (!name.startsWith("spring.ai") && !name.startsWith("gen_ai")) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("type", meter.getClass().getSimpleName().replace("MeterRegistry$", ""));
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
        // 按五层指标顺序排序
        List<String> layerOrder = List.of(
            "spring.ai.chat.client",
            "spring.ai.advisor",
            "spring.ai.tool",
            "gen_ai.client.operation",
            "gen_ai.client.token.usage"
        );
        result.sort((a, b) -> {
            String nameA = (String) a.get("name");
            String nameB = (String) b.get("name");
            int idxA = indexOfLayer(nameA, layerOrder);
            int idxB = indexOfLayer(nameB, layerOrder);
            return Integer.compare(idxA, idxB);
        });
        return result;
    }

    private int indexOfLayer(String name, List<String> layers) {
        for (int i = 0; i < layers.size(); i++) {
            if (name.startsWith(layers.get(i))) return i;
        }
        return 99;
    }

    /**
     * 返回 5 层指标名称列表，对应关系供演示页面使用。
     */
    public List<Map<String, String>> getLayersDescription() {
        return List.of(
            Map.of("layer", "① spring.ai.chat.client", "metric", "spring.ai.chat.client", "description", "ChatClient 层，包含 advisor 调用"),
            Map.of("layer", "② spring.ai.advisor", "metric", "spring.ai.advisor", "description", "Advisor 拦截层"),
            Map.of("layer", "③ spring.ai.tool", "metric", "spring.ai.tool", "description", "Tool calling 层"),
            Map.of("layer", "④ gen_ai.client.operation", "metric", "gen_ai.client.operation", "description", "ChatModel 层（DeepSeek API）"),
            Map.of("layer", "⑤ gen_ai.client.token.usage", "metric", "gen_ai.client.token.usage", "description", "Token 用量统计")
        );
    }
}