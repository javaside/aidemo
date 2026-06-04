package org.example.springai;

import org.example.springai.config.ApiKeyConfig;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Spring AI 提示词工程模式演示
 *
 * 本类演示了11种提示词工程方法，每种方法都基于Spring AI的ChatClient实现。
 * 每种方法都设计为：清晰展示原理 + 简单易执行 + 不易超时
 *
 * 【核心洞见】提示词工程 = 提示词文本 + LLM输出参数配置。
 * 官方文档强调：好的提示不仅是“怎么说”，还包括用 ChatOptions 控制“怎么生成”。
 * 因此本示例为每个模式都搭配了合适的输出参数，并打印选择理由：
 * - temperature（温度）：越低越确定/稳定，越高越随机/有创意。
 *   分类、数学、代码 → 低温（0.0~0.2）；文案、故事、头脑风暴 → 高温（0.8~1.0）。
 * - topP（核采样）：从累计概率前 P 的候选里采样，常与 temperature 配合控制多样性。
 * - maxTokens（最大输出长度）：分类等极短输出可设很小，既快又省 token。
 * 注意：官方示例还演示了 topK，但 DeepSeek API 不支持 top_k，故本示例改用 topP（这本身也是
 * 一个教学点——不同模型供应商支持的参数不同，不可照抄）。
 *
 * 参考文档:
 * - 官方参考: https://docs.spring.io/spring-ai/reference/api/chat/prompt-engineering-patterns.html
 * - 官方博客: https://spring.io/blog/2025/04/14/spring-ai-prompt-engineering-patterns
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
        System.out.println("配置: temperature=0.0 + maxTokens=20 → 分类要确定性结果，输出极短，省 token 且更快");
        // 分类任务：要可复现的确定答案，所以用最低温；只需要一个标签，maxTokens 设很小即可。
        return chatClient.prompt("评论: \"这部电影太棒了！\" 分类: POSITIVE/NEGATIVE/NEUTRAL?")
                .options(ChatOptions.builder().temperature(0.0).maxTokens(20).build())
                .call().content();
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
        System.out.println("配置: temperature=0.1 → 要严格照着示例的 JSON 格式输出，低温保证格式保真");
        String prompt = """
            将订单转为JSON:
            订单: 小披萨加芝士 → {"size":"small","toppings":["芝士"]}
            订单: 大号海鲜披萨 → {"size":"large","toppings":["海鲜"]}
            订单: 中号榴莲披萨
            """;
        // 跟着示例“照猫画虎”输出固定格式：低温让模型老实模仿，不要自由发挥。
        return chatClient.prompt(prompt)
                .options(ChatOptions.builder().temperature(0.1).maxTokens(256).build())
                .call().content();
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
        System.out.println("配置: temperature=0.9 + topP=0.9 → 文学创作要有想象力，高温让文风更生动");
        // 创意写作：高温 + 核采样，给模型更大的发挥空间，文字更有古风韵味。
        return chatClient.prompt()
                .system("你是一位武侠小说家，说话风格古风诗意。")
                .user("描述: 主角走进酒馆")
                .options(ChatOptions.builder().temperature(0.9).topP(0.9).build())
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
        System.out.println("配置: temperature=0.7 → 专业建议要靠谱，但保留自然的表达语气，用中等温度");
        // 专业角色：温度适中——既不能太死板（像念说明书），也不能太发散（编造偏方）。
        return chatClient.prompt()
                .system("扮演一位资深老中医，有30年临床经验，说话专业且温和。")
                .user("养生建议: 经常熬夜加班的上班族")
                .options(ChatOptions.builder().temperature(0.7).build())
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
        System.out.println("配置: temperature=0.8 → 创意点子要发散多样，用较高温度");
        // 复用提示词模板，注入不同上下文
        String result = chatClient.prompt()
                .user(u -> u.text("模板: 推荐3个{topic}主题的短视频创意\n要求: 一句话概括每个").params(Map.of("topic", "复古80年代街机游戏")))
                .options(ChatOptions.builder().temperature(0.8).build())
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
        System.out.println("配置: temperature=1.0 + topP=0.8 → 抽象与创作两步都偏创意，对标官方高温配置");
        // 官方此模式用 temperature=1.0, topK=40, topP=0.8；DeepSeek 无 top_k，这里改用 topP。
        ChatOptions creative = ChatOptions.builder().temperature(1.0).topP(0.8).build();

        // 第一步：获取高层概念（退一步）
        String concepts = chatClient.prompt("列举5个经典科幻电影的核心元素，一句话概括")
                .options(creative)
                .call().content();
        System.out.println("Step1 - 获取抽象概念: " + concepts);

        // 第二步：将抽象概念用于具体问题（进一步）
        return chatClient.prompt()
                .user(u -> u.text("用以下元素写一个科幻故事开头:\n{elements}")
                        .params(Map.of("elements", concepts)))
                .options(creative)
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
        System.out.println("配置: temperature=0.0 → 数学题只有唯一正确答案，必须用最低温避免随机算错");
        // CoT 推理题：答案唯一，要确定性。高温会让模型“想太多”反而算错。
        return chatClient.prompt("问题: 停车场有50辆车，上午卖出20辆，下午买进15辆，现在多少辆？一步步思考后回答")
                .options(ChatOptions.builder().temperature(0.0).build())
                .call().content();
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
        System.out.println("配置: temperature=1.0（故意高温）→ 这里反而需要高温制造采样差异，再靠投票收敛");
        // 注意：和 CoT 的低温相反！Self-Consistency 故意用高温，让多次采样产生不同推理路径，
        // 再用多数投票收敛到最可靠的答案。温度高低没有绝对好坏，取决于你的目标。
        List<String> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String result = chatClient.prompt()
                    .user("情感分类: \"今天考试没考好，心情低落\" 回答: POSITIVE或NEGATIVE?一步步思考后回答")
                    .options(ChatOptions.builder().temperature(1.0).build())
                    .call().content();
            results.add(result);
            System.out.println("采样" + (i + 1) + ": " + result);
        }

        // 统计投票：对每条回复判定其结论标签，再数票。
        // 朴素写法 contains("POSITIVE") 有两个坑：
        //   ① 一句话里同时出现两个词（如“不是 POSITIVE 而是 NEGATIVE”）会被双重计票；
        //   ② 一个标签词都没有时，会静默落入默认分支。
        // 这里改用 voteOf：取“更靠后出现的标签”（结论通常在末尾），并把无标签的算作弃权。
        long pos = results.stream().filter(r -> voteOf(r).equals("POSITIVE")).count();
        long neg = results.stream().filter(r -> voteOf(r).equals("NEGATIVE")).count();
        long invalid = results.stream().filter(r -> voteOf(r).equals("INVALID")).count();

        String verdict = (neg > pos) ? "NEGATIVE" : (pos > neg ? "POSITIVE" : "平票/无法判定");
        return "投票结果: POSITIVE=" + pos + ", NEGATIVE=" + neg + ", 弃权=" + invalid + " → 最终判定: " + verdict;
    }

    /**
     * 判定单条回复的情感标签：取“更靠后出现”的标签（结论一般在末尾），都没有则弃权。
     * 这是教学用的简化判定；生产环境建议改用结构化输出（见 docs/structured-output-guide.md）强制返回枚举。
     */
    private static String voteOf(String text) {
        int pos = text.lastIndexOf("POSITIVE");
        int neg = text.lastIndexOf("NEGATIVE");
        if (pos < 0 && neg < 0) {
            return "INVALID";
        }
        return (neg > pos) ? "NEGATIVE" : "POSITIVE";
    }

    // ========================================================================
    // 9. Tree of Thoughts (ToT)（思维树）
    // ========================================================================
    /**
     * 【原理说明】
     * 思维树在 CoT「单线推理」基础上更进一步：先生成多个候选方案（分支），
     * 评估比较后选出最优的一个，再沿它深入展开，整个过程形如一棵决策树。
     *
     * 【与CoT的区别】
     * - CoT: 单条链式推理
     * - ToT: 多方案对比后再决策，更适合开放性、需要权衡的问题
     *
     * 【本示例是简化演示，请注意与完整算法的差别】
     * 完整 ToT 会在「多层树」上反复分支、打分、剪枝乃至回溯，复杂度高得多。
     * 为便于入门，本示例简化为「生成候选 → 评估选择 → 深入展开」三步：
     * 1. 分支：用一次调用让模型列出 3 个候选（而非并行运行 3 条独立推理链）；
     * 2. 评估：选中 1 个，即相当于把其余分支「剪枝」掉；
     * 3. 延伸：沿选中的方案深入。没有多层树，也没有回溯。
     * 目的是让新手用三步看懂「分支→评估→深入」的核心思想，先建立直觉。
     */
    public String treeOfThoughts() {
        System.out.println("\n========== 9. Tree of Thoughts (思维树) ==========");
        System.out.println("特点: 分支探索→评估选择→深入延伸（简化版 ToT，完整算法含多层树与回溯）");
        System.out.println("配置: 分支阶段 temperature=0.8（要多样选项），评估阶段 temperature=0.2（要理性判断）");

        // Step 1: 生成多个选项（分支）——高温，鼓励多样化的候选方案
        String options = chatClient.prompt("午餐选择: 列出3个不同风格的选项(中式/西式/日式)，每个一句话")
                .options(ChatOptions.builder().temperature(0.8).build())
                .call().content();
        System.out.println("分支探索: " + options);

        // Step 2: 评估选择（剪枝）——低温，要理性、可复现的判断
        String choice = chatClient.prompt()
                .user(u -> u.text("从以下选项中选择一个，说明理由:\n{opt}").params(Map.of("opt", options)))
                .options(ChatOptions.builder().temperature(0.2).build())
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
     * 【本示例是简化演示，请注意与完整算法的差别】
     * Step 1: 将"T恤订单"生成3种不同表达方式
     * Step 2: 用生成的变体测试模型理解能力
     * 注意：完整的自动提示词工程还会对每个变体「逐一打分、择优、迭代优化」，
     * 形成"生成 → 评估 → 改进"的闭环。本示例省略了打分与择优，
     * 重在展示"用 AI 生产提示词"这一核心思路。
     */
    public String automaticPromptEngineering() {
        System.out.println("\n========== 10. Auto PE (自动提示词工程) ==========");
        System.out.println("特点: 用AI生成提示词变体 → 发现多样化表达方式");
        System.out.println("配置: temperature=0.9 → 生成变体阶段要尽量多样，用高温拉开差异");

        // Step 1: AI生成变体——高温，鼓励多样化表达
        String variants = chatClient.prompt("将\"买一件蓝色T恤 M码\"用3种不同方式表达")
                .options(ChatOptions.builder().temperature(0.9).build())
                .call().content();
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
        System.out.println("配置: temperature=0.0 → 代码要正确可运行，用最低温保证精准、可复现");
        // 代码生成：和数学题一样追求正确性，低温避免模型“发挥创意”写出跑不通的代码。
        return chatClient.prompt("用Python写一个函数: 输入整数列表，返回平均值。只写核心函数，不要注释。")
                .options(ChatOptions.builder().temperature(0.0).build())
                .call().content();
    }

    // ========================================================================
    // 附加演示: 温度对照（同一提示，不同温度，亲眼看差异）
    // ========================================================================
    /**
     * 【目的】温度是全文最重要的参数，但“读到”不如“看到”。
     * 这里用同一个创意提示，分别在 temperature=0.0 和 1.0 下各跑两次：
     * - 0.0：两次结果几乎一致（确定、可复现）
     * - 1.0：两次结果明显不同（随机、有创意）
     * 跑一遍就能直观体会温度的作用，这正是其它模式选择高/低温的依据。
     */
    public String temperatureContrast() {
        System.out.println("\n========== 附加演示: 温度对照（同一提示，不同温度）==========");
        System.out.println("目的: 同一句创意提示，在 0.0 与 1.0 下各跑两次，对比“确定 vs 多样”");
        String prompt = "用一句话描写“雨后的城市”，不超过30字";

        System.out.println("\n[temperature=0.0] 期望：两次几乎相同");
        for (int i = 1; i <= 2; i++) {
            String r = chatClient.prompt(prompt)
                    .options(ChatOptions.builder().temperature(0.0).build())
                    .call().content();
            System.out.println("  第" + i + "次: " + r);
        }

        System.out.println("\n[temperature=1.0] 期望：两次明显不同");
        for (int i = 1; i <= 2; i++) {
            String r = chatClient.prompt(prompt)
                    .options(ChatOptions.builder().temperature(1.0).build())
                    .call().content();
            System.out.println("  第" + i + "次: " + r);
        }
        return "（温度对照见上方四行输出：0.0 几乎相同，1.0 明显不同）";
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

        // 用 runSafely 包装每个模式：单个模式失败（如网络读超时）不会中断整个演示，且自动重试。
        runSafely("1. Zero-Shot", patterns::zeroShot);
        runSafely("2. Few-Shot", patterns::fewShot);
        runSafely("3. System Prompting", patterns::systemPrompting);
        runSafely("4. Role Prompting", patterns::rolePrompting);
        runSafely("5. Contextual", patterns::contextualPrompting);
        runSafely("6. Step-Back", patterns::stepBackPrompting);
        runSafely("7. Chain of Thought", patterns::chainOfThought);
        runSafely("8. Self-Consistency", patterns::selfConsistency);
        runSafely("9. Tree of Thoughts", patterns::treeOfThoughts);
        runSafely("10. Auto PE", patterns::automaticPromptEngineering);
        runSafely("11. Code Prompting", patterns::codePrompting);
        runSafely("附加. 温度对照", patterns::temperatureContrast);

        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                          演示结束                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }

    /**
     * 安全执行一个模式：打印标题、捕获异常、失败自动重试（最多 3 次）。
     * 长文本生成（如回退提示）偶发底层 HTTP 读超时；重试 + 容错让演示不被单点失败中断、能完整跑到最后
     * （个别模式重试仍失败则跳过并提示），各模式互不影响。
     */
    private static void runSafely(String label, Supplier<String> demo) {
        System.out.println("\n--- " + label + " ---");
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                System.out.println(demo.get());
                return;
            }
            catch (Exception e) {
                System.err.println("[" + label + "] 第" + attempt + "次执行失败: " + e.getClass().getSimpleName()
                        + (attempt < 3 ? "，重试中…" : "，已跳过（不影响后续模式）"));
            }
        }
    }
}