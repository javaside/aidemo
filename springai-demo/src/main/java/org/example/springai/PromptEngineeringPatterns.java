package org.example.springai;

import org.example.springai.config.ApiKeyConfig;

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
 * 每种方法都设计为：清晰展示原理 + 简单易执行 + 不易超时
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
     * 【核心特点】
     * - 无示例，直接指令
     * - 模型"自由发挥"依赖预训练知识
     * - 适合简单明确的任务
     *
     * 【本示例演示】
     * 直接指令让模型将影评分类为POSITIVE/NEUTRAL/NEGATIVE。
     * 对比Few-Shot：没有示例，模型依靠自身知识完成分类。
     */
    public String zeroShot() {
        System.out.println("\n========== 1. Zero-Shot Prompting (零样本提示) ==========");
        System.out.println("特点: 无示例，直接指令 → 模型依靠预训练知识自由发挥");
        return chatClient.prompt("评论: \"这部电影太棒了！\" 分类: POSITIVE/NEGATIVE/NEUTRAL?").call().content();
    }

    // ========================================================================
    // 2. Few-Shot Prompting（多样本提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 多样本提示通过提供1-3个示例，让模型学习输入输出的对应关系和格式。
     * 示例帮助模型理解任务的具体模式，而非依赖通用知识。
     *
     * 【与Zero-Shot的区别】
     * - Zero-Shot: 无示例，模型"自由发挥"
     * - Few-Shot: 有示例，模型"照猫画虎"
     *
     * 【本示例演示】
     * 提供"输入→输出"的示例对，展示如何通过示例让模型理解JSON格式要求。
     */
    public String fewShot() {
        System.out.println("\n========== 2. Few-Shot Prompting (多样本提示) ==========");
        System.out.println("特点: 提供1-3个示例 → 模型学习格式后照样子输出");
        String prompt = """
            将订单转为JSON:
            订单: 小披萨加芝士 → {"size":"small","toppings":["芝士"]}
            订单: 大号海鲜披萨 → {"size":"large","toppings":["海鲜"]}
            订单: 中号榴莲披萨
            """;
        return chatClient.prompt(prompt).call().content();
    }

    // ========================================================================
    // 3. System Prompting（系统提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 系统提示通过.system()设置全局上下文，定义模型的身份、语气和行为框架。
     * 系统提示在对话前就确定，影响后续所有交互。
     *
     * 【核心特点】
     * - 设置持久的全局上下文
     * - 定义模型的身份和说话风格
     * - 影响整个对话的行为一致性
     *
     * 【本示例演示】
     * 设置"武侠小说家"身份，让模型以该角色风格描述场景。
     * 关键：身份设定影响输出风格，展示System Prompting的行为控制能力。
     */
    public String systemPrompting() {
        System.out.println("\n========== 3. System Prompting (系统提示) ==========");
        System.out.println("特点: .system()设置全局身份 → 影响整个对话的输出风格");
        return chatClient.prompt()
                .system("你是一位武侠小说家，说话风格古风诗意。")
                .user("描述: 主角走进酒馆")
                .call().content();
    }

    // ========================================================================
    // 4. Role Prompting（角色提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 角色提示让模型扮演特定角色，以该角色的视角和专业知识来回答。
     * 与System Prompting类似，但更强调"扮演"某个具体身份。
     *
     * 【与System Prompting的区别】
     * - System Prompting: 定义通用行为框架
     * - Role Prompting: 强调扮演特定身份/职业
     *
     * 【本示例演示】
     * 让模型扮演"老中医"角色，以其专业视角提供养生建议。
     * 关键：输出应体现老中医的专业知识、口吻和思维模式。
     */
    public String rolePrompting() {
        System.out.println("\n========== 4. Role Prompting (角色提示) ==========");
        System.out.println("特点: 扮演特定角色 → 以该角色视角和专业知识回答");
        return chatClient.prompt()
                .system("扮演一位资深老中医，有30年临床经验，说话专业且温和。")
                .user("养生建议: 经常熬夜加班的上班族")
                .call().content();
    }

    // ========================================================================
    // 5. Contextual Prompting（上下文提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 上下文提示通过参数注入背景信息，实现提示词模板复用。
     * 同一个模板，不同context参数，产生不同结果。
     *
     * 【核心优势】
     * - 提示词模板与上下文分离
     * - 一套模板，多种上下文复用
     * - 便于动态调整上下文
     *
     * 【本示例演示】
     * 使用相同的模板"推荐3个{context}主题"，通过params注入不同context。
     * 关键：展示模板复用的便利性，同一句式不同背景产生不同推荐。
     */
    public String contextualPrompting() {
        System.out.println("\n========== 5. Contextual Prompting (上下文提示) ==========");
        System.out.println("特点: 通过params注入上下文 → 模板复用，背景动态切换");
        // 复用提示词模板，注入不同上下文
        String result = chatClient.prompt()
                .user(u -> u.text("模板: 推荐3个{topic}主题的短视频创意\n要求: 一句话概括每个").params(Map.of("topic", "复古80年代街机游戏")))
                .call().content();
        System.out.println("注入的context=复古80年代街机游戏 → 结果: " + result);
        return result;
    }

    // ========================================================================
    // 6. Step-Back Prompting（回退提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 回退提示先获取高层抽象概念，再将抽象概念应用于具体问题。
     * 先"退一步"获取广泛知识，再"进一步"解决具体问题。
     *
     * 【适用场景】
     * - 需要广泛背景知识的问题
     * - 直接回答过于具体或局限
     * - 需要抽象概念引导具体创意
     *
     * 【本示例演示】
     * Step 1: 获取"所有科幻经典元素"（抽象）
     * Step 2: 将抽象概念用于"写一个科幻故事开头"（具体）
     * 关键：展示从抽象到具体的转化过程，创意更丰富。
     */
    public String stepBackPrompting() {
        System.out.println("\n========== 6. Step-Back Prompting (回退提示) ==========");
        System.out.println("特点: 先获取高层概念 → 再用于具体问题，创意更丰富");

        // 第一步：获取高层概念（退一步）
        String concepts = chatClient.prompt("列举5个经典科幻电影的核心元素，一句话概括")
                .call().content();
        System.out.println("Step1 - 获取抽象概念: " + concepts);

        // 第二步：将抽象概念用于具体问题（进一步）
        return chatClient.prompt()
                .user(u -> u.text("用以下元素写一个科幻故事开头:\n{elements}")
                        .params(Map.of("elements", concepts)))
                .call().content();
    }

    // ========================================================================
    // 7. Chain of Thought (CoT)（思维链）
    // ========================================================================
    /**
     * 【原理说明】
     * 思维链要求模型展示逐步推理过程，而非直接给出答案。
     * "一步步思考"触发多步骤推理，减少"跳步"错误。
     *
     * 【核心要点】
     * - "一步步思考"触发逐步推理
     * - 展示中间步骤和逻辑
     * - 适合数学、逻辑推理任务
     *
     * 【本示例演示】
     * 数学问题添加"一步步思考"，展示推理过程。
     * 关键：输出应包含中间步骤，让用户看到推理链条。
     */
    public String chainOfThought() {
        System.out.println("\n========== 7. Chain of Thought (思维链) ==========");
        System.out.println("特点: 要求展示推理过程 → 减少跳步错误，提高准确性");
        return chatClient.prompt("问题: 停车场有50辆车，上午卖出20辆，下午买进15辆，现在多少辆？一步步思考后回答").call().content();
    }

    // ========================================================================
    // 8. Self-Consistency（自洽性提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 自洽性通过多次采样（不同温度）+ 多数投票，汇总结果更可靠。
     * 多次运行取最常见答案，比单次更稳定。
     *
     * 【核心流程】
     * 1. 同一问题运行多次（每次不同采样）
     * 2. 汇总结果，取最多票的答案
     *
     * 【本示例演示】
     * 情感分类运行3次，展示不同采样产生不同结果，最终通过投票统一。
     * 关键：展示多次采样的差异性和投票汇总过程。
     */
    public String selfConsistency() {
        System.out.println("\n========== 8. Self-Consistency (自洽性提示) ==========");
        System.out.println("特点: 多次采样+多数投票 → 结果比单次更稳定可靠");

        List<String> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String result = chatClient.prompt()
                    .user("情感分类: \"今天考试没考好，心情低落\" 回答: POSITIVE或NEGATIVE?一步步思考后回答")
                    .options(ChatOptions.builder().temperature(1.0).build())
                    .call().content();
            results.add(result);
            System.out.println("采样" + (i + 1) + ": " + result);
        }

        // 统计投票
        long pos = results.stream().filter(r -> r.contains("POSITIVE")).count();
        long neg = results.stream().filter(r -> r.contains("NEGATIVE")).count();

        return "投票结果: POSITIVE=" + pos + ", NEGATIVE=" + neg + " → 最终判定: " + (neg > pos ? "NEGATIVE" : "POSITIVE");
    }

    // ========================================================================
    // 9. Tree of Thoughts (ToT)（思维树）
    // ========================================================================
    /**
     * 【原理说明】
     * 思维树探索多条并行推理路径，模拟决策树：
     * 1. 生成多个候选方案（分支）
     * 2. 评估每个方案（剪枝）
     * 3. 选择最佳深入（延伸）
     *
     * 【与CoT的区别】
     * - CoT: 单条链式推理
     * - ToT: 多条路径并行探索，类似决策树
     *
     * 【本示例演示】
     * 解决"午餐吃什么"问题，展示分支探索和评估选择过程。
     * 关键：三步清晰展示树状决策过程。
     */
    public String treeOfThoughts() {
        System.out.println("\n========== 9. Tree of Thoughts (思维树) ==========");
        System.out.println("特点: 分支探索→评估选择→深入延伸 → 模拟决策树");

        // Step 1: 生成多个选项（分支）
        String options = chatClient.prompt("午餐选择: 列出3个不同风格的选项(中式/西式/日式)，每个一句话").call().content();
        System.out.println("分支探索: " + options);

        // Step 2: 评估选择（剪枝）
        String choice = chatClient.prompt()
                .user(u -> u.text("从以下选项中选择一个，说明理由:\n{opt}").params(Map.of("opt", options)))
                .call().content();
        System.out.println("评估选择: " + choice);

        // Step 3: 深入延伸
        return chatClient.prompt()
                .user(u -> u.text("基于选择: {c}，详细说明这顿午餐的内容").params(Map.of("c", choice)))
                .call().content();
    }

    // ========================================================================
    // 10. Automatic Prompt Engineering（自动提示词工程）
    // ========================================================================
    /**
     * 【原理说明】
     * 自动提示词工程用AI生成和优化提示词变体。
     * 可批量生成多种表达方式，发现人工想不到的变体。
     *
     * 【核心价值】
     * - 发现多样化表达方式
     * - 自动化生成测试用例
     * - 评估提示词效果
     *
     * 【本示例演示】
     * Step 1: 将"T恤订单"生成3种不同表达方式
     * Step 2: 用生成的变体测试模型理解能力
     * 关键：展示AI辅助生成提示词变体的过程。
     */
    public String automaticPromptEngineering() {
        System.out.println("\n========== 10. Auto PE (自动提示词工程) ==========");
        System.out.println("特点: 用AI生成提示词变体 → 发现多样化表达方式");

        // Step 1: AI生成变体
        String variants = chatClient.prompt("将\"买一件蓝色T恤 M码\"用3种不同方式表达").call().content();
        System.out.println("AI生成变体: " + variants);

        // Step 2: 用变体测试
        return chatClient.prompt()
                .user(u -> u.text("将以下任一表达转为JSON格式:\n{v}").params(Map.of("v", variants)))
                .call().content();
    }

    // ========================================================================
    // 11. Code Prompting（代码提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 代码提示用于代码生成、解释、调试等编程任务。
     * 明确指定语言、输入输出、约束条件，减少歧义。
     *
     * 【最佳实践】
     * - 明确指定编程语言
     * - 说明输入输出要求
     * - 包含边界情况处理
     *
     * 【本示例演示】
     * 明确要求用Python实现指定功能，并包含测试。
     * 关键：清晰的规格说明减少模型理解歧义。
     */
    public String codePrompting() {
        System.out.println("\n========== 11. Code Prompting (代码提示) ==========");
        System.out.println("特点: 明确指定语言和规格 → 减少歧义，输出更精准");
        return chatClient.prompt("用Python写一个函数: 输入整数列表，返回平均值。只写核心函数，不要注释。").call().content();
    }

    // ========================================================================
    // Main 方法
    // ========================================================================
    public static void main(String[] args) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(ApiKeyConfig.getDeepSeekApiKey())
                .build();

        DeepSeekChatModel model = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        ChatClient chatClient = ChatClient.builder(model).build();
        PromptEngineeringPatterns patterns = new PromptEngineeringPatterns(chatClient);

        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║         Spring AI 提示词工程模式演示 (共11种)                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

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

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                          演示结束                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }
}