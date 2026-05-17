package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring AI 提示词工程模式演示
 *
 * 本类演示了11种提示词工程方法，每种方法都基于Spring AI的ChatClient实现。
 *
 * 参考文档: https://spring.io/blog/2025/04/14/spring-ai-prompt-engineering-patterns
 */
public class PromptEngineeringPatterns {

    private final ChatClient chatClient;

    public PromptEngineeringPatterns(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    // ========================================================================
    // 1. Zero-Shot Prompting（零样本提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 零样本提示是最基础的提示词工程方法，直接给出任务指令，不提供任何示例。
     * 模型依靠其预训练知识来理解和执行任务。
     *
     * 【适用场景】
     * - 任务简单明确，模型能直接从指令理解需求
     * - 不需要特定输出格式的场景
     * - 快速原型验证
     *
     * 【本示例演示】
     * 直接要求模型将影评分类为POSITIVE/NEUTRAL/NEGATIVE三种之一。
     * 通过简洁的指令，让模型理解任务并输出标准化结果。
     */
    public String zeroShot() {
        System.out.println("\n========== 1. Zero-Shot Prompting ==========");
        // 原理: 直接给出任务指令，不提供示例
        // 特点: 简单直接，依赖模型预训练知识
        return chatClient.prompt("将电影评论分类为 POSITIVE、NEUTRAL 或 NEGATIVE。评论: \"这部电影的剧情太棒了，演员演技精湛！\"").call().content();
    }

    // ========================================================================
    // 2. Few-Shot Prompting（多样本提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 多样本提示通过提供1-N个示例，让模型学习输入输出的对应关系和格式要求。
     * 示例帮助模型理解任务的具体的模式和格式，而不是依赖其通用知识。
     *
     * 【与Zero-Shot的区别】
     * - Zero-Shot: 只给指令，模型"自由发挥"
     * - Few-Shot: 提供示例，模型"照猫画虎"
     *
     * 【本示例演示】
     * 提供两个披萨订单解析为JSON的示例，展示：
     * - 输入: 自然语言订单
     * - 输出: 结构化JSON格式
     * 模型学习到这种对应关系后，能正确解析新的订单。
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

    // ========================================================================
    // 3. System Prompting（系统提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 系统提示通过.system()方法设置全局上下文，定义模型的行为框架、身份角色或约束条件。
     * 系统提示在对话开始前就确定，影响后续所有交互。
     *
     * 【核心特点】
     * - 设置持久的上下文，不受单次交互影响
     * - 定义模型的身份、语气、知识范围等
     * - 可以包含格式要求、行为准则等
     *
     * 【本示例演示】
     * 通过system()设置"专业美食评论家，幽默风趣"的设定。
     * 模型以此身份和风格来评价宫保鸡丁，输出更具特色和一致性。
     */
    public String systemPrompting() {
        System.out.println("\n========== 3. System Prompting ==========");
        // 原理: 使用 .system() 设置全局上下文和行为框架
        // 特点: 影响整个对话，定义模型身份和说话风格
        return chatClient.prompt()
                .system("你是一位专业的美食评论家，说话风格幽默风趣。")
                .user("评价一下宫保鸡丁这道菜")
                .call().content();
    }

    // ========================================================================
    // 4. Role Prompting（角色提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 角色提示让模型扮演特定角色，以此角色视角来回答问题。
     * 与System Prompting类似，但更强调"扮演"某个具体角色。
     *
     * 【与System Prompting的区别】
     * - System Prompting: 定义通用行为框架
     * - Role Prompting: 强调扮演特定身份/职业
     *
     * 【本示例演示】
     * 让模型"扮演旅行导游"角色：
     * - 需要具备导游的专业知识（景点、文化）
     * - 以导游的口吻和视角提供建议
     * - 输出内容符合导游的表达方式
     */
    public String rolePrompting() {
        System.out.println("\n========== 4. Role Prompting ==========");
        // 原理: 让模型扮演特定角色，以该角色视角回答问题
        // 特点: 强调身份和专业知识，适合顾问、咨询类场景
        return chatClient.prompt()
                .system("我希望你能扮演一位旅行导游，熟悉世界各地的景点和文化。")
                .user("我在巴黎待3天，喜欢艺术和美食，帮我规划行程")
                .call().content();
    }

    // ========================================================================
    // 5. Contextual Prompting（上下文提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 上下文提示通过参数注入背景信息，让模型在特定上下文中理解和回答问题。
     * 上下文字符串作为占位符在运行时动态填充，实现提示词复用。
     *
     * 【核心优势】
     * - 将通用提示词与具体上下文分离
     * - 一套提示词模板，多种上下文复用
     * - 便于动态调整上下文内容
     *
     * 【本示例演示】
     * 使用 .params(Map.of("context", "...")) 注入博客主题背景：
     * - 模板: "推荐3个适合写博客的主题。背景: {context}"
     * - 填充: context = "复古80年代街机视频游戏"
     * 模型基于此背景推荐相关主题，而非泛泛推荐。
     */
    public String contextualPrompting() {
        System.out.println("\n========== 5. Contextual Prompting ==========");
        // 原理: 通过 .params() 注入背景信息到提示词模板
        // 特点: 提示词与上下文分离，实现复用和动态调整
        return chatClient.prompt()
                .user(u -> u.text("推荐3个适合写博客的主题。背景: {context}").params(Map.of("context", "复古80年代街机视频游戏")))
                .call().content();
    }

    // ========================================================================
    // 6. Step-Back Prompting（回退提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 回退提示先将问题分解为两步：
     * 1. 先询问高层概念/抽象问题
     * 2. 再将抽象答案应用于具体问题
     *
     * 【适用场景】
     * - 问题需要广泛背景知识
     * - 直接回答可能过于具体或局限
     * - 需要抽象概念来引导具体推理
     *
     * 【本示例演示】
     * 两步流程：
     * Step 1: "基于FPS游戏，列出5个虚构的标志性场景设定"
     *         → 获取高层概念（赛博朋克城市、太空站等）
     * Step 2: 将场景设定作为背景，写FPS游戏关卡故事情节
     *         → 抽象概念转化为具体创意
     */
    public String stepBackPrompting() {
        System.out.println("\n========== 6. Step-Back Prompting ==========");

        // 第一步：获取高层概念
        String stepBack = chatClient.prompt("""
            基于FPS游戏（第一人称射击游戏），列出5个虚构的标志性场景设定
            """).call().content();

        System.out.println("获取的场景设定: " + stepBack);

        // 第二步：在主任务中使用这些概念
        // 原理: 将抽象概念作为具体问题的背景信息
        return chatClient.prompt()
                .user(u -> u.text("为新的FPS游戏关卡写一个故事情节。背景: {context}").params(Map.of("context", stepBack)))
                .call().content();
    }

    // ========================================================================
    // 7. Chain of Thought (CoT)（思维链）
    // ========================================================================
    /**
     * 【原理说明】
     * 思维链提示要求模型"一步步思考"，展示推理过程而非直接给出答案。
     * 引导模型进行多步骤推理，提高复杂任务的准确性。
     *
     * 【核心要点】
     * - "让我们一步步思考" 触发模型逐步推理
     * - 展示中间步骤，便于检查逻辑
     * - 减少模型"跳步"导致的错误
     *
     * 【本示例演示】
     * 数学问题："当我3岁时，哥哥年龄是我的3倍。现在我20岁，哥哥几岁？"
     * 通过"让我们一步步思考"引导模型：
     * - 展示年龄计算过程
     * - 避免直接给出可能错误的答案
     * - 体现了推理的中间状态
     */
    public String chainOfThought() {
        System.out.println("\n========== 7. Chain of Thought ==========");
        // 原理: 添加"一步步思考"触发逐步推理
        // 特点: 展示中间过程，适合数学、逻辑推理任务
        return chatClient.prompt("""
            小明说: "当我3岁时，我哥哥的年龄是我的3倍。现在我20岁了，我哥哥几岁？"
            让我们一步步思考。
            """).call().content();
    }

    // ========================================================================
    // 8. Self-Consistency（自洽性/自洽性提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 自洽性提示通过多次运行同一提示（不同温度），然后汇总结果。
     * 多次采样的结果中，最常见的答案被认为是更可靠的。
     *
     * 【与CoT的关系】
     * CoT + Self-Consistency = 更可靠的推理
     * CoT要求展示推理过程
     * Self-Consistency通过投票汇总多次结果
     *
     * 【本示例演示】
     * 1. 同一问题运行3次（temperature=1.0，每次不同采样）
     * 2. 每次都要求"一步步思考后给出答案"
     * 3. 汇总三次结果：POSITIVE票数 vs NEUTRAL票数 vs NEGATIVE票数
     * 4. 多数票作为最终判定
     * 结果比单次调用更稳定可靠。
     */
    public String selfConsistency() {
        System.out.println("\n========== 8. Self-Consistency ==========");
        List<String> results = new ArrayList<>();

        // 多次运行，每次使用高温度产生不同采样
        for (int i = 0; i < 3; i++) {
            String result = chatClient.prompt()
                    .user("判断这句话的情感: \"今天天气真好，心情很愉快\"。请一步步思考后给出答案(只回答POSITIVE、NEUTRAL或NEGATIVE之一)。")
                    .options(ChatOptions.builder().temperature(1.0).build())
                    .call().content();
            results.add(result);
            System.out.println("第" + (i + 1) + "次运行结果: " + result);
        }

        // 汇总结果（多数投票）
        long positiveCount = results.stream().filter(r -> r.contains("POSITIVE")).count();
        long negativeCount = results.stream().filter(r -> r.contains("NEGATIVE")).count();
        long neutralCount = results.stream().filter(r -> r.contains("NEUTRAL")).count();

        return "最终判定(多数票): " + (positiveCount > negativeCount && positiveCount > neutralCount ? "POSITIVE" :
                negativeCount > neutralCount ? "NEGATIVE" : "NEUTRAL");
    }

    // ========================================================================
    // 9. Tree of Thoughts (ToT)（思维树）
    // ========================================================================
    /**
     * 【原理说明】
     * 思维树是CoT的扩展，不是一条链式推理，而是探索多条并行推理路径：
     * 1. 生成多个候选方案/路径
     * 2. 评估每个方案
     * 3. 选择最佳方案继续深入
     *
     * 【与CoT的区别】
     * - CoT: 单条路径逐步推理
     * - ToT: 多条路径并行探索，类似决策树
     *
     * 【本示例演示】
     * 三步流程：
     * Step 1: 生成3个西洋跳棋开局策略（分支探索）
     * Step 2: 分析并选择最有利的一个（评估选择）
     * Step 3: 预测后续3步可能发展（深入推理）
     *
     * 这种方法适合策略规划、方案评估等需要多角度思考的场景。
     */
    public String treeOfThoughts() {
        System.out.println("\n========== 9. Tree of Thoughts ==========");

        // 步骤1: 生成多个选项（树的分支）
        String initialMoves = chatClient.prompt("""
            为西洋跳棋游戏生成3个不同的开局策略选项，每个选项简述其特点
            """).call().content();
        System.out.println("候选策略:\n" + initialMoves);

        // 步骤2: 评估并选择最佳（剪枝）
        String evaluation = chatClient.prompt()
                .user(u -> u.text("分析以下开局策略，选择最有利的一个并说明理由。策略: {moves}").params(Map.of("moves", initialMoves)))
                .call().content();
        System.out.println("策略评估:\n" + evaluation);

        // 步骤3: 探索未来状态（深入）
        return chatClient.prompt()
                .user(u -> u.text("基于以下策略预测接下来3步的可能发展。策略: {best}").params(Map.of("best", evaluation)))
                .call().content();
    }

    // ========================================================================
    // 10. Automatic Prompt Engineering（自动提示词工程）
    // ========================================================================
    /**
     * 【原理说明】
     * 自动提示词工程使用AI来生成和优化提示词变体，而不是手动设计。
     * 可以批量生成多种表达方式，用于测试或构建更鲁棒的提示词。
     *
     * 【核心价值】
     * - 发现人工想不到的表达方式
     * - 自动化生成提示词变体
     * - 评估不同提示词的效果
     *
     * 【本示例演示】
     * 两步流程：
     * Step 1: 让AI生成5种不同的T恤订单表达方式
     *         "一件Metallica乐队T恤，尺码S" → 多种自然语言说法
     * Step 2: 用生成的变体测试模型的理解能力
     *         将不同说法都正确解析为JSON格式
     *
     * 这展示了如何用AI辅助构建更全面的测试用例。
     */
    public String automaticPromptEngineering() {
        System.out.println("\n========== 10. Automatic Prompt Engineering ==========");

        // 步骤1: 用AI生成提示词变体
        String variants = chatClient.prompt("""
            将顾客的T恤订单表达为自然语言，生成5种不同的说法。
            原始订单: "一件Metallica乐队T恤，尺码S"
            """).call().content();
        System.out.println("生成的说法:\n" + variants);

        // 步骤2: 用变体测试模型能力
        return chatClient.prompt()
                .user(u -> u.text("请将以下任何一种订单表达转换为结构化JSON格式。订单列表:\n{variants}").params(Map.of("variants", variants)))
                .call().content();
    }

    // ========================================================================
    // 11. Code Prompting（代码提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 代码提示专门用于代码生成、解释、调试等编程相关任务。
     * 提示词明确说明代码要求，如语言、框架、功能、测试等。
     *
     * 【最佳实践】
     * - 明确指定编程语言
     * - 说明输入输出要求
     * - 要求包含测试代码
     * - 指明代码风格或约束
     *
     * 【本示例演示】
     * 要求模型：
     * - 写一个Python函数
     * - 功能: 接收整数列表，返回偶数之和
     * - 包含完整函数定义和测试代码
     *
     * 这种明确的任务描述帮助模型生成符合要求的代码。
     */
    public String codePrompting() {
        System.out.println("\n========== 11. Code Prompting ==========");
        // 原理: 明确指定编程语言、输入输出、约束条件
        // 特点: 适合代码生成、调试、解释等编程任务
        return chatClient.prompt("""
            写一个Python函数，接收一个整数列表，返回其中的偶数之和。
            要求: 包含完整的函数定义和测试代码。
            """).call().content();
    }

    // ========================================================================
    // Main 方法
    // ========================================================================
    public static void main(String[] args) {
        // 创建 DeepSeek API 配置
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey("sk-f429667b2e4a4581bc1a3bb873ffa69f")
                .build();

        DeepSeekChatModel model = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        ChatClient chatClient = ChatClient.builder(model).build();

        PromptEngineeringPatterns patterns = new PromptEngineeringPatterns(chatClient);

        System.out.println("================================================================================");
        System.out.println("           Spring AI 提示词工程模式演示 (共11种)");
        System.out.println("================================================================================");

        System.out.println("\n--- 1. Zero-Shot ---");
        System.out.println(patterns.zeroShot());

        System.out.println("\n--- 2. Few-Shot ---");
        System.out.println(patterns.fewShot());

        System.out.println("\n--- 3. System Prompting ---");
        System.out.println(patterns.systemPrompting());

        System.out.println("\n--- 4. Role Prompting ---");
        System.out.println(patterns.rolePrompting());

        System.out.println("\n--- 5. Contextual ---");
        System.out.println(patterns.contextualPrompting());

        System.out.println("\n--- 6. Step-Back ---");
        System.out.println(patterns.stepBackPrompting());

        System.out.println("\n--- 7. Chain of Thought ---");
        System.out.println(patterns.chainOfThought());

        System.out.println("\n--- 8. Self-Consistency ---");
        System.out.println(patterns.selfConsistency());

        System.out.println("\n--- 9. Tree of Thoughts ---");
        System.out.println(patterns.treeOfThoughts());

        System.out.println("\n--- 10. Auto PE ---");
        System.out.println(patterns.automaticPromptEngineering());

        System.out.println("\n--- 11. Code Prompting ---");
        System.out.println(patterns.codePrompting());

        System.out.println("\n================================================================================");
        System.out.println("                         演示结束");
        System.out.println("================================================================================");
    }
}