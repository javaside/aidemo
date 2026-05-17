package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring AI 提示词工程模式演示
 * 参考: https://spring.io/blog/2025/04/14/spring-ai-prompt-engineering-patterns
 */
public class PromptEngineeringPatterns {

    private final ChatClient chatClient;

    public PromptEngineeringPatterns(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 1. Zero-Shot Prompting（零样本提示）
     * 直接要求模型执行任务，无示例
     */
    public String zeroShot() {
        System.out.println("\n========== 1. Zero-Shot Prompting ==========");
        return chatClient.prompt("将电影评论分类为 POSITIVE、NEUTRAL 或 NEGATIVE。评论: \"这部电影的剧情太棒了，演员演技精湛！\"").call().content();
    }

    /**
     * 2. One-Shot & Few-Shot Prompting（单样本/多样本提示）
     * 提供示例引导输出格式
     */
    public String fewShot() {
        System.out.println("\n========== 2. Few-Shot Prompting ==========");
        String prompt = """
            将客户订单解析为JSON格式。

            示例1:
            输入: 我想要一个小的披萨，加双份芝士
            JSON响应: {"size": "small", "toppings": ["extra cheese"]}

            示例2:
            输入: 一个大号海鲜披萨，不要蘑菇
            JSON响应: {"size": "large", "toppings": ["seafood", "-mushroom"]}

            现在请解析:
            输入: 中号榴莲披萨，多加芝士
            """;
        return chatClient.prompt(prompt).call().content();
    }

    /**
     * 3. System Prompting（系统提示）
     * 设置全局上下文和行为框架
     */
    public String systemPrompting() {
        System.out.println("\n========== 3. System Prompting ==========");
        return chatClient.prompt()
                .system("你是一位专业的美食评论家，说话风格幽默风趣。")
                .user("评价一下宫保鸡丁这道菜")
                .call().content();
    }

    /**
     * 4. Role Prompting（角色提示）
     * 让模型扮演特定角色
     */
    public String rolePrompting() {
        System.out.println("\n========== 4. Role Prompting ==========");
        return chatClient.prompt()
                .system("我希望你能扮演一位旅行导游，熟悉世界各地的景点和文化。")
                .user("我在巴黎待3天，喜欢艺术和美食，帮我规划行程")
                .call().content();
    }

    /**
     * 5. Contextual Prompting（上下文提示）
     * 通过参数注入背景信息
     */
    public String contextualPrompting() {
        System.out.println("\n========== 5. Contextual Prompting ==========");
        return chatClient.prompt()
                .user(u -> u.text("推荐3个适合写博客的主题。背景: {context}")
                        .param("context", "复古80年代街机视频游戏"))
                .call().content();
    }

    /**
     * 6. Step-Back Prompting（回退提示）
     * 先获取背景知识，再解决具体问题
     */
    public String stepBackPrompting() {
        System.out.println("\n========== 6. Step-Back Prompting ==========");

        // 第一步：获取高层概念
        String stepBack = chatClient.prompt("""
            基于FPS游戏（第一人称射击游戏），列出5个虚构的标志性场景设定
            """).call().content();

        System.out.println("获取的场景设定: " + stepBack);

        // 第二步：在主任务中使用这些概念
        return chatClient.prompt()
                .user("为新的FPS游戏关卡写一个故事情节。背景: {context}")
                .param("context", stepBack)
                .call().content();
    }

    /**
     * 7. Chain of Thought (CoT)（思维链）
     * 要求模型逐步推理
     */
    public String chainOfThought() {
        System.out.println("\n========== 7. Chain of Thought ==========");
        return chatClient.prompt("""
            小明说: "当我3岁时，我哥哥的年龄是我的3倍。现在我20岁了，我哥哥几岁？"
            让我们一步步思考。
            """).call().content();
    }

    /**
     * 8. Self-Consistency（自洽性）
     * 多次运行并汇总结果
     */
    public String selfConsistency() {
        System.out.println("\n========== 8. Self-Consistency ==========");
        List<String> results = new ArrayList<>();

        // 运行多次，每次使用不同温度
        for (int i = 0; i < 3; i++) {
            String result = chatClient.prompt()
                    .user("判断这句话的情感: \"今天天气真好，心情很愉快\"。请一步步思考后给出答案(只回答POSITIVE、NEUTRAL或NEGATIVE之一)。")
                    .options(ChatOptions.builder().temperature(1.0).build())
                    .call().content();
            results.add(result);
            System.out.println("第" + (i + 1) + "次运行结果: " + result);
        }

        // 汇总结果（简单多数投票）
        long positiveCount = results.stream().filter(r -> r.contains("POSITIVE")).count();
        long negativeCount = results.stream().filter(r -> r.contains("NEGATIVE")).count();
        long neutralCount = results.stream().filter(r -> r.contains("NEUTRAL")).count();

        return "最终判定(多数票): " + (positiveCount > negativeCount && positiveCount > neutralCount ? "POSITIVE" :
                negativeCount > neutralCount ? "NEGATIVE" : "NEUTRAL");
    }

    /**
     * 9. Tree of Thoughts (ToT)（思维树）
     * 探索多条推理路径
     */
    public String treeOfThoughts() {
        System.out.println("\n========== 9. Tree of Thoughts ==========");

        // 步骤1: 生成多个选项
        String initialMoves = chatClient.prompt("""
            为西洋跳棋游戏生成3个不同的开局策略选项，每个选项简述其特点
            """).call().content();
        System.out.println("候选策略:\n" + initialMoves);

        // 步骤2: 评估并选择最佳
        String evaluation = chatClient.prompt()
                .user("分析以下开局策略，选择最有利的一个并说明理由。策略: {moves}")
                .param("moves", initialMoves)
                .call().content();
        System.out.println("策略评估:\n" + evaluation);

        // 步骤3: 探索未来状态
        return chatClient.prompt()
                .user("基于以下策略预测接下来3步的可能发展。策略: {best}")
                .param("best", evaluation)
                .call().content();
    }

    /**
     * 10. Automatic Prompt Engineering（自动提示词工程）
     * 用AI生成和评估提示词变体
     */
    public String automaticPromptEngineering() {
        System.out.println("\n========== 10. Automatic Prompt Engineering ==========");

        // 用AI生成多种表达方式
        String variants = chatClient.prompt("""
            将顾客的T恤订单表达为自然语言，生成5种不同的说法。
            原始订单: "一件Metallica乐队T恤，尺码S"
            """).call().content();
        System.out.println("生成的说法:\n" + variants);

        // 用生成的变体来构造更丰富的提示
        return chatClient.prompt("""
            请将以下任何一种订单表达转换为结构化JSON格式。
            订单列表:
            {variants}
            """)
                .param("variants", variants)
                .call().content();
    }

    /**
     * 11. Code Prompting（代码提示）
     * 用于代码相关任务
     */
    public String codePrompting() {
        System.out.println("\n========== 11. Code Prompting ==========");
        return chatClient.prompt("""
            写一个Python函数，接收一个整数列表，返回其中的偶数之和。
            要求: 包含完整的函数定义和测试代码。
            """).call().content();
    }

    public static void main(String[] args) {
        // 创建ChatClient实例（需要先配置好Bean）
        // ChatClient chatClient = ...

        System.out.println("================================================================================");
        System.out.println("           Spring AI 提示词工程模式演示 (共11种)");
        System.out.println("================================================================================");
        System.out.println("提示: 需要在Spring上下文中注入ChatClient才能运行此演示");
        System.out.println("参考: https://spring.io/blog/2025/04/14/spring-ai-prompt-engineering-patterns");
        System.out.println("================================================================================");

        // 下面展示每种模式的核心代码结构
        demonstratePatternStructures();
    }

    /**
     * 演示每种模式的代码结构（不需要实际运行AI）
     */
    private static void demonstratePatternStructures() {
        System.out.println("\n\n========================================");
        System.out.println("       各模式核心代码结构展示");
        System.out.println("========================================\n");

        // 1. Zero-Shot
        System.out.println("【1. Zero-Shot Prompting】");
        System.out.println("""
            String result = chatClient.prompt("将评论分类为 POSITIVE、NEUTRAL 或 NEGATIVE。评论: \"太棒了！\"")
                .call().content();
            """);

        // 2. Few-Shot
        System.out.println("\n【2. Few-Shot Prompting】");
        System.out.println("""
            String result = chatClient.prompt("\"\"
                将订单解析为JSON。
                示例: 输入: 小披萨 加芝士 → {"size": "small", "toppings": ["cheese"]}
                现在请解析: 输入: 大号海鲜披萨
            \"\"\").call().content();
            """);

        // 3. System Prompting
        System.out.println("\n【3. System Prompting】");
        System.out.println("""
            String result = chatClient.prompt()
                .system("你是一位专业美食评论家。")
                .user("评价一下宫保鸡丁")
                .call().content();
            """);

        // 4. Role Prompting
        System.out.println("\n【4. Role Prompting】");
        System.out.println("""
            String result = chatClient.prompt()
                .system("我希望你能扮演一位旅行导游。")
                .user("我在巴黎3天，推荐行程")
                .call().content();
            """);

        // 5. Contextual
        System.out.println("\n【5. Contextual Prompting】");
        System.out.println("""
            String result = chatClient.prompt()
                .user(u -> u.text("推荐3个主题。背景: {context}").param("context", "复古游戏"))
                .call().content();
            """);

        // 6. Step-Back
        System.out.println("\n【6. Step-Back Prompting】");
        System.out.println("""
            // 第一步：获取高层概念
            String concepts = chatClient.prompt("列出5个FPS游戏场景").call().content();
            // 第二步：使用概念
            String story = chatClient.prompt()
                .user("写一个FPS关卡故事。背景: {c}").param("c", concepts)
                .call().content();
            """);

        // 7. CoT
        System.out.println("\n【7. Chain of Thought】");
        System.out.println("""
            String result = chatClient.prompt("甲3岁，乙是甲3倍年龄。甲现在20岁，乙几岁？让我们一步步思考。")
                .call().content();
            """);

        // 8. Self-Consistency
        System.out.println("\n【8. Self-Consistency】");
        System.out.println("""
            List<String> results = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                results.add(chatClient.prompt().user("分类情感。一步步思考。")
                    .options(ChatOptions.builder().temperature(1.0).build())
                    .call().content());
            }
            // 多数投票决定最终结果
            """);

        // 9. ToT
        System.out.println("\n【9. Tree of Thoughts】");
        System.out.println("""
            // 1. 生成多个选项
            String options = chatClient.prompt("生成3个策略").call().content();
            // 2. 评估选择
            String best = chatClient.prompt().user("分析并选择: {o}").param("o", options).call().content();
            // 3. 探索后续
            String future = chatClient.prompt().user("预测后续发展: {b}").param("b", best).call().content();
            """);

        // 10. Auto PE
        System.out.println("\n【10. Automatic Prompt Engineering】");
        System.out.println("""
            String variants = chatClient.prompt("生成10种不同说法: \"一件T恤尺码S\"").call().content();
            String result = chatClient.prompt().user("解析订单: {v}").param("v", variants).call().content();
            """);

        // 11. Code Prompting
        System.out.println("\n【11. Code Prompting】");
        System.out.println("""
            String code = chatClient.prompt("写一个Python函数返回列表中的偶数之和").call().content();
            """);

        System.out.println("\n================================================================================");
        System.out.println("                         完整示例结束");
        System.out.println("================================================================================");
    }
}