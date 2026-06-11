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
 * - temperature（温度）：通常越低越稳定，越高越多样；temperature=0 也不保证所有供应商都逐字可复现。
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
    // 2. One-Shot & Few-Shot Prompting（单样本 / 少样本提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 通过提供示例，让模型学习输入输出的对应关系和格式，而非依赖通用知识。
     * 给 1 个例子叫单样本(One-Shot)，给几个例子叫少样本(Few-Shot)，是同一招，只差例子数量。
     *
     * 【与Zero-Shot的区别】
     * - Zero-Shot: 无示例，模型"自由发挥"
     * - One-Shot/Few-Shot: 有示例，模型"照猫画虎"
     *
     * 【本示例演示】
     * 提供两组"输入→输出"示例（属 Few-Shot），展示如何用示例让模型照着输出 JSON 格式。
     */
    public String fewShot() {
        System.out.println("\n========== 2. One-Shot & Few-Shot Prompting (单样本/少样本提示) ==========");
        System.out.println("特点: 给1个(单样本)或几个(少样本)示例 → 模型学格式后照样子输出");
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
     * 系统提示用 .system() 设定"全局行为框架"——输出格式、语气、边界、高层目标。
     * 它像一份贯穿本次请求或当前对话上下文的"使命说明"，框定 user 提问该如何被回答。
     *
     * 【与角色提示的区别】
     * - 系统提示：偏"全局规则/格式/约束"（如：只返回 JSON、不超过40字、语气正式）
     * - 角色提示：偏"扮演一个具体身份"（如：入门老师、旅行向导）
     *
     * 【本示例演示】
     * 用系统提示设定一条全局回答规则（先结论、再一句话解释、不超过40字），
     * 再随便问一个问题，回答会乖乖遵守这条规则。
     */
    public String systemPrompting() {
        System.out.println("\n========== 3. System Prompting (系统提示) ==========");
        System.out.println("特点: .system()设全局规则(格式/语气/边界) → 框定本次请求或当前对话怎么答");
        System.out.println("配置: temperature=0.3 → 要稳稳遵守格式规则，用较低温");
        // 系统提示 = 本次请求或当前对话的"行为框架"：定下格式/语气/约束。
        return chatClient.prompt()
                .system("你是一个回答助手。规则：(1)先给结论 (2)再用一句话解释 (3)全文不超过40字。")
                .user("周末适合去爬山吗？")
                .options(ChatOptions.builder().temperature(0.3).build())
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
     * 让模型扮演"Spring AI 入门老师"角色，以教学视角解释 ChatClient。
     * 关键：输出应体现老师的解释方式和对初学者的照顾。
     */
    public String rolePrompting() {
        System.out.println("\n========== 4. Role Prompting (角色提示) ==========");
        System.out.println("特点: 扮演特定角色 → 以该角色视角和专业知识回答");
        System.out.println("配置: temperature=0.5 → 教学解释要清楚，也要保留自然表达");
        // 教学角色：温度适中——既要清楚稳定，也不要像机械说明书。
        return chatClient.prompt()
                .system("扮演一位耐心的 Spring AI 入门老师，擅长用 Java 初学者能懂的话解释概念。")
                .user("用两句话解释 ChatClient 是什么，以及新手应该先学哪个方法。")
                .options(ChatOptions.builder().temperature(0.5).build())
                .call().content();
    }

    // ========================================================================
    // 5. Contextual Prompting（上下文提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 上下文提示：在提示里额外给出一段"背景信息(Context)"，让模型的回答贴合你的具体场景，
     * 而不必把背景和主指令搅在一起。
     *
     * 【本示例演示】（对标官方示例）
     * 主指令是"推荐3个文章选题"，再通过 param 注入背景"为复古80年代街机游戏博客写作"，
     * 模型据此给出契合该博客的选题。换个背景，选题就跟着变。
     */
    public String contextualPrompting() {
        System.out.println("\n========== 5. Contextual Prompting (上下文提示) ==========");
        System.out.println("特点: 在提示里给出背景(Context:) → 回答据此贴合具体场景");
        System.out.println("配置: temperature=0.8 → 选题要发散，用较高温");
        // 上下文提示 = 给模型一段"背景信息"，让回答契合你的场景。
        return chatClient.prompt()
                .user(u -> u.text("""
                        推荐3个文章选题，每个用一句话说明写什么。
                        背景：{context}
                        """).param("context", "你在为一个复古80年代街机游戏博客写作"))
                .options(ChatOptions.builder().temperature(0.8).build())
                .call().content();
    }

    // ========================================================================
    // 6. Step-Back Prompting（回退提示）
    // ========================================================================
    /**
     * 【原理说明】
     * 回退提示先获取高层原则、背景知识或判断标准，再将它们应用于具体问题。
     * 先"退一步"获取广泛知识，再"进一步"解决具体问题。
     *
     * 【适用场景】
     * - 需要广泛背景知识的问题
     * - 直接回答过于具体或局限
     * - 需要高层原则引导具体创意
     *
     * 【本示例演示】
     * Step 1: 获取"优秀科幻故事开头的常见元素"（高层原则）
     * Step 2: 将抽象概念用于"写一个科幻故事开头"（具体）
     * 关键：展示从抽象到具体的转化过程，创意更丰富。
     */
    public String stepBackPrompting() {
        System.out.println("\n========== 6. Step-Back Prompting (回退提示) ==========");
        System.out.println("特点: 先获取高层概念 → 再用于具体问题，创意更丰富");
        System.out.println("配置: temperature=1.0 + topP=0.8 → 抽象与创作两步都偏创意，对标官方高温配置");
        // 官方此模式用 temperature=1.0, topK=40, topP=0.8；DeepSeek 无 top_k，这里改用 topP。
        ChatOptions creative = ChatOptions.builder().temperature(1.0).topP(0.8).build();

        // 第一步：获取高层原则（退一步）
        String concepts = chatClient.prompt("优秀科幻故事开头通常具备哪些元素？列举5个，各一句话")
                .options(creative)
                .call().content();
        System.out.println("Step1 - 获取高层原则: " + concepts);

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
     * 自洽性通过多次高温采样 + 多数投票，汇总结果更可靠。
     * 多次运行取最常见答案，比单次更稳定。
     *
     * 【核心流程】
     * 1. 同一问题运行多次（用高温得到不同推理路径）
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
        // Self-Consistency 常和“逐步思考”组合：故意用高温生成不同推理路径，
        // 再用多数投票收敛到更稳的结论。温度高低没有绝对好坏，取决于你的目标。
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
     * 思维树在 CoT「单线推理」之上更进一步：生成多个候选 → 评估比较选出最佳 →
     * 再从最佳处"向前推演"后续几步，像做学习计划一样先比较路线再细化行动。
     *
     * 【与CoT的区别】
     * - CoT: 一条思路走到底
     * - ToT: 先铺开几条思路、挑最好的，再往前推演它的后续
     *
     * 【本示例演示】（用新手学习 Spring AI 的路线选择，避免额外游戏规则）
     * 新手只有 2 小时时间：① 生成3条学习路线 ② 评估选出最适合的一条
     * ③ 基于选中的路线继续推演执行步骤和可能卡点。
     * 完整 ToT 可以反复展开、评估和回溯；这里为了入门，只演示最核心的三步。
     * 关键在第③步——"向前推演几步"，这正是 ToT 区别于思维链之处。
     */
    public String treeOfThoughts() {
        System.out.println("\n========== 9. Tree of Thoughts (思维树) ==========");
        System.out.println("特点: 生成候选→评估选最佳→向前推演后续几步（像做学习计划先选路线再拆行动）");
        System.out.println("配置: 候选阶段 temperature=0.7（要多样学习路线）");

        // ① 生成几条候选学习路线
        String plans = chatClient.prompt("""
                我是 Spring AI 新手，只有2小时学习时间。
                给出3条不同的学习路线：先读文档、先跑 Demo、先改 Prompt。
                每条说明适合什么情况，并按“上手快/收获大/难度低”打分(1-10)。
                """)
                .options(ChatOptions.builder().temperature(0.7).build())
                .call().content();
        System.out.println("候选学习路线: " + plans);

        // ② 评估并选出最适合新手的一条
        String best = chatClient.prompt()
                .user(u -> u.text("从这些学习路线里选最适合新手的一条，说明理由：\n{p}").param("p", plans))
                .call().content();
        System.out.println("评估选择: " + best);

        // ③ 向前推演：选定后，接下来怎么做、卡住时先查什么（要简短，避免长篇）
        return chatClient.prompt()
                .user(u -> u.text("基于选中的路线，用3-4句话推演具体执行步骤，并说明一个最可能卡住的点怎么处理：\n{b}")
                        .param("b", best))
                .call().content();
    }

    // ========================================================================
    // 10. Automatic Prompt Engineering（自动提示词工程）
    // ========================================================================
    /**
     * 【原理说明】
     * 自动提示词工程(APE)：让 AI 自己"生成多个候选提示词，再评估、挑出最优的一个"——
     * 把"找更好提示词"这件事也自动化。本示例用模型自评来简化，生产环境建议用验证集和明确指标评估。
     *
     * 【本示例演示】
     * ① 让 AI 为情感分类任务生成5个候选提示词（temp=1.0 求多样）
     * ② 让 AI 按格式约束、清晰度、减少歧义三个标准评估，挑出最适合放进示例代码的一条。
     */
    public String automaticPromptEngineering() {
        System.out.println("\n========== 10. Auto PE (自动提示词工程) ==========");
        System.out.println("特点: AI 自己生成多个候选提示词 → 再自己评估，挑出最好的一个");
        System.out.println("配置: 生成阶段 temperature=1.0（要尽量多样）");

        // ① 生成同一任务的多个候选提示词
        String prompts = chatClient.prompt("""
                我们要做情感分类任务，输入是一句中文评论，输出只能是 POSITIVE/NEGATIVE/NEUTRAL。
                请生成5个不同的候选提示词，每个都要强调只能输出这3个标签之一。
                """)
                .options(ChatOptions.builder().temperature(1.0).build())
                .call().content();
        System.out.println("候选提示词: " + prompts);

        // ② 让 AI 评估这些候选提示词，挑出最适合放进示例代码的一条
        return chatClient.prompt()
                .user(u -> u.text("按格式约束、清晰度、减少歧义三项评估，选出最好的一条并说明理由：\n{p}")
                        .param("p", prompts))
                .options(ChatOptions.builder().temperature(0.2).build())
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
     * 明确要求用Python实现指定功能，并处理空列表边界情况。
     * 关键：清晰的规格说明减少模型理解歧义。
     */
    public String codePrompting() {
        System.out.println("\n========== 11. Code Prompting (代码提示) ==========");
        System.out.println("特点: 明确指定语言和规格 → 减少歧义，输出更精准");
        System.out.println("配置: temperature=0.0 → 代码要正确可运行，用最低温保证精准、可复现");
        // 代码生成：和数学题一样追求正确性，低温避免模型“发挥创意”写出跑不通的代码。
        return chatClient.prompt("用Python写一个函数: 输入整数列表，返回平均值；如果列表为空，抛出 ValueError。只写函数。")
                .options(ChatOptions.builder().temperature(0.0).build())
                .call().content();
    }

    // ========================================================================
    // 附加演示: 温度对照（同一提示，不同温度，亲眼看差异）
    // ========================================================================
    /**
     * 【目的】温度是全文最重要的参数，但“读到”不如“看到”。
     * 这里用同一个创意提示，分别在 temperature=0.0 和 1.0 下各跑两次：
     * - 0.0：通常更稳定（但不把它理解成所有供应商都逐字可复现）
     * - 1.0：通常更明显不同（更随机、更有创意）
     * 跑一遍就能直观体会温度的作用，这正是其它模式选择高/低温的依据。
     */
    public String temperatureContrast() {
        System.out.println("\n========== 附加演示: 温度对照（同一提示，不同温度）==========");
        System.out.println("目的: 同一句创意提示，在 0.0 与 1.0 下各跑两次，对比“稳定 vs 多样”");
        String prompt = "用一句话描写“雨后的城市”，不超过30字";

        System.out.println("\n[temperature=0.0] 期望：通常更稳定");
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
        return "（温度对照见上方四行输出：0.0 通常更稳定，1.0 通常更多样）";
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
