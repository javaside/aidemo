package org.example.springai.config;

/**
 * API Key 配置工具类。
 *
 * <p>统一从环境变量中获取 API Key，避免硬编码。</p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * // 方式1：设置环境变量（推荐）
 * export DEEPSEEK_API_KEY=sk-your-key-here
 *
 * // 方式2：在 IDE 中配置运行参数
 * Run -> Edit Configurations -> Environment Variables
 * DEEPSEEK_API_KEY=sk-your-key-here
 *
 * // 方式3：在命令行运行时指定
 * DEEPSEEK_API_KEY=sk-xxx mvn exec:java -Dexec.mainClass="..."
 * </pre>
 */
public class ApiKeyConfig {

    /**
     * DeepSeek API Key 环境变量名
     */
    private static final String DEEPSEEK_API_KEY_ENV = "DEEPSEEK_API_KEY";

    /**
     * 默认 API Key（仅用于快速演示，生产环境必须使用环境变量）
     */
    private static final String DEFAULT_DEEPSEEK_API_KEY = "sk-please-set-DEEPSEEK_API_KEY-env-variable";

    /**
     * 获取 DeepSeek API Key。
     *
     * <p>优先从环境变量 DEEPSEEK_API_KEY 读取，如果未设置则返回默认值并打印警告。</p>
     *
     * @return DeepSeek API Key
     */
    public static String getDeepSeekApiKey() {
        String apiKey = System.getenv(DEEPSEEK_API_KEY_ENV);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("⚠️  警告：未设置环境变量 " + DEEPSEEK_API_KEY_ENV);
            System.err.println("   请设置环境变量：export " + DEEPSEEK_API_KEY_ENV + "=sk-your-key-here");
            System.err.println("   当前使用默认值（可能无法正常工作）");
            return DEFAULT_DEEPSEEK_API_KEY;
        }

        return apiKey;
    }

    /**
     * 检查 API Key 是否已正确配置。
     *
     * @return 如果已配置返回 true，否则返回 false
     */
    public static boolean isApiKeyConfigured() {
        String apiKey = System.getenv(DEEPSEEK_API_KEY_ENV);
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals(DEFAULT_DEEPSEEK_API_KEY);
    }

    /**
     * 验证 API Key 配置，如果未配置则抛出异常。
     *
     * @throws IllegalStateException 如果 API Key 未配置
     */
    public static void validateApiKey() {
        if (!isApiKeyConfigured()) {
            throw new IllegalStateException(
                    "未设置环境变量 " + DEEPSEEK_API_KEY_ENV + "。" +
                    "请执行：export " + DEEPSEEK_API_KEY_ENV + "=sk-your-key-here"
            );
        }
    }
}
