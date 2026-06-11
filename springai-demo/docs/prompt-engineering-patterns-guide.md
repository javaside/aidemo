# Spring AI 提示词工程模式详解

和 AI 打交道，其实跟与人沟通很像：同一件事，你说得清不清楚、有没有交代背景，得到的回答可能天差地别。
**所谓提示词工程，就是通过设计和打磨"提示词"，引导 AI 给出更准确、更高质量的回答**——用官方的话说，
它"既是一门艺术，也是一门科学"，需要反复打磨。

具体怎么做？提示词工程包含两个层面：

- **提示词文本（说什么）**：怎么组织语言、给不给例子、要不要设定角色。下面 11 个模式，主要练这一层。
- **输出参数（怎么生成）**：用 `ChatOptions` 调节模型的生成方式，其中最关键的是 `temperature`（温度）。

> 温度可以先记住一句：**通常越低越稳定，越高越多样。** 但不同模型供应商实现不同，`temperature=0`
> 也不等于绝对逐字可复现。

接下来的 11 种"问法"，每种叫一个**模式**。每个都讲清楚 **是什么、什么时候用、怎么写**，配好温度并附
示例输出。高温和创意类任务每次输出可能不同，重点看结构和方法。看完，你就有了一套随取随用的工具箱。

> 代码里的 `chatClient` 是 Spring AI 的入口对象 `ChatClient`，由模型构建而来；构建细节见源码 `main` 方法，
> 本文只聚焦每个模式的提示词写法。

---

## 1. 零样本提示（Zero-Shot）

最简单的一种：**不给任何例子，直接下指令**，让 AI 凭它学过的知识作答。

**什么时候用**：任务简单、AI 本来就会——分类、翻译、摘要。

```java
String result = chatClient.prompt("评论:\"这部电影太棒了！\" 请分类:POSITIVE/NEGATIVE/NEUTRAL?")
        .options(ChatOptions.builder().temperature(0.0).build())  // 分类要确定结果，用最低温
        .call()
        .content();
```

示例输出：`POSITIVE`

💡 **能直接说清的任务别绕弯子，零样本最省事。**

---

## 2. 单样本 / 少样本提示（One-Shot & Few-Shot）

给 AI 看一两个"输入 → 输出"的例子，它就**照着样子做**——像教小孩，示范两遍他就会了。
给 **1 个**例子叫**单样本（One-Shot）**，给**几个**叫**少样本（Few-Shot）**，是同一招、只差例子数量。

**什么时候用**：想要固定的输出格式，或任务稍复杂、光靠指令说不清。

```java
String prompt = """
    把订单转成 JSON：
    小披萨加芝士 → {"size":"small","toppings":["芝士"]}
    大号海鲜披萨 → {"size":"large","toppings":["海鲜"]}
    中号榴莲披萨 →
    """;

chatClient.prompt(prompt)
        .options(ChatOptions.builder().temperature(0.1).build())  // 照着例子仿，低温保格式
        .call()
        .content();
```

示例输出：`{"size":"medium","toppings":["榴莲"]}`

💡 **一两个例子，往往胜过一大段解释。**

---

## 3. 系统提示（System）

用 `.system()` 设一条**全局规则**——格式、语气、边界等，它会**框住本次请求或当前对话上下文该怎么答**。

**什么时候用**：想统一输出格式 / 语气 / 约束（比如"只返回 JSON""不超过 N 字""语气正式"）。

```java
chatClient.prompt()
        .system("你是一个回答助手。规则：(1)先给结论 (2)再用一句话解释 (3)全文不超过40字。")
        .user("周末适合去爬山吗？")
        .options(ChatOptions.builder().temperature(0.3).build())  // 要守规则，用较低温
        .call()
        .content();
```

示例输出：

```
适合。周末天气适宜，爬山能放松身心。
```

💡 **系统提示设的是"全局规矩"——在同一次请求或同一段对话里，回答都尽量照这套格式来。这就是它和"角色提示"的分工。**

---

## 4. 角色提示（Role）

让 AI **扮演一个具体身份**（老师、向导、客服……），用那个身份的视角和表达方式答话。和系统提示一样用
`.system()`，但内容是"你是谁"，而不是"全局规则"。

**什么时候用**：需要某个稳定视角或专业口吻——老师、客服、产品经理……

```java
chatClient.prompt()
        .system("扮演一位耐心的 Spring AI 入门老师，擅长用 Java 初学者能懂的话解释概念。")
        .user("用两句话解释 ChatClient 是什么，以及新手应该先学哪个方法。")
        .options(ChatOptions.builder().temperature(0.5).build())  // 教学解释要清楚，也要自然
        .call()
        .content();
```

示例输出（节选）：

```
ChatClient 就是 Spring AI 里和模型对话的入口，负责把你的提示词发给模型并拿回回答。
新手先学 `prompt().user(...).call().content()` 这条最短调用链，跑通后再看 system、参数和上下文。
```

💡 **"你是谁"决定了"你怎么答"。**

---

## 5. 上下文提示（Contextual）

在提示里额外给一段**背景信息（Context）**，让 AI 的回答**贴合你的具体场景**，而不是泛泛而答。

**什么时候用**：同一个任务，因背景不同要给出不同的、更贴切的回答。

```java
chatClient.prompt()
        .user(u -> u.text("""
                推荐3个文章选题，每个用一句话说明写什么。
                背景：{context}
                """).param("context", "你在为一个复古80年代街机游戏博客写作"))
        .options(ChatOptions.builder().temperature(0.8).build())
        .call()
        .content();
```

示例输出（节选）：

```
1.《投币心理学：80年代街机厅如何掏空我们的零花钱》——解析"续命"机制与即时奖励循环。
2.《街机厅社会学：被遗忘的玩家社群暗语与高分榜争夺》——它作为青少年社交空间的集体记忆。
3.《为什么我们还在玩"老古董"？》——论像素美学、芯片音乐对现代独立游戏的影响。
```

💡 **关键是给出"背景上下文"——同样问"推荐选题"，背景换成美妆博客，选题就全变了。**

---

## 6. 回退提示（Step-Back）

假设你的目标是**"写一个科幻故事开头"**。直接让 AI 写，往往俗套、平淡。

回退提示的思路是：**先别急着写，把问题往上"退一层"**——先问"优秀科幻故事开头通常具备哪些元素？"，
拿到更高层的原则和背景后，**再带着这些原则回到原目标**去写。绕这一下，成品明显更丰富。

**什么时候用**：直接做容易写窄、写空，需要先补充高层原则、背景知识或判断标准时。

```java
ChatOptions creative = ChatOptions.builder().temperature(1.0).build();

// 第一步｜退：先问一个更宽泛的问题，拿"高层原则"
String concepts = chatClient.prompt("优秀科幻故事开头通常具备哪些元素？列举5个，各一句话")
        .options(creative)
        .call().content();

// 第二步｜进：带着素材，回到原目标——写故事
chatClient.prompt()
        .user(u -> u.text("用这些元素写一个科幻故事开头：\n{c}").param("c", concepts))
        .options(creative)
        .call().content();
```

示例输出（节选）：

```
退一步拿到原则：强钩子 / 未知悬念 / 世界规则 / 人物处境 / 冲突信号
回到目标写出来：2147年，超级AI"织女"向2045年发出一段加密信息，收信人正是主角失踪的母亲……
```

💡 **"退"是手段，"进"才是目的：先借更宽的问题找原则，再回头把具体任务做得更好。**

---

## 7. 思维链（Chain of Thought）

加一句"一步步思考"，让 AI **把推理过程写出来**，而不是直接蹦答案。

**什么时候用**：数学、逻辑、多步推理——展示步骤能大幅减少算错。

```java
chatClient.prompt("停车场有50辆车，上午卖出20辆，下午买进15辆，现在多少辆？一步步思考后回答")
        .options(ChatOptions.builder().temperature(0.0).build())  // 唯一答案，低温求准
        .call()
        .content();
```

示例输出：

```
50 − 20 = 30，30 + 15 = 45。答案：45 辆。
```

💡 **让 AI "想出声"，它就不容易跳步出错。**

---

## 8. 自洽性（Self-Consistency）

让 AI 对**同一个问题多次采样**，再取**出现最多的答案**——像遇事多问几个人，听大多数。

**什么时候用**：推理、判断类任务，想让结果更稳、更可靠。

```java
List<String> results = new ArrayList<>();
for (int i = 0; i < 3; i++) {                    // 同一问题跑 3 次
    results.add(chatClient.prompt("情感分类:\"考试没考好，心情低落\" → POSITIVE 还是 NEGATIVE? 一步步思考后回答")
            .options(ChatOptions.builder().temperature(1.0).build())  // 故意高温，让每次有差异
            .call().content());
}
// 再数票，取多数
```

示例输出：

```
采样1 → NEGATIVE   采样2 → NEGATIVE   采样3 → NEGATIVE   最终：NEGATIVE（3 票）
```

💡 **它不是思维链的反面，而是常和思维链组合：多跑几条推理路径，再对结论投票。温度没有绝对好坏，看你要什么。**

---

## 9. 思维树（Tree of Thoughts）

**像画一棵决策树一样思考**：不急着认定一种学法，而是先列出几条可选路线 → 比较哪条最有潜力 →
在选中的路线下面继续展开几个子方案 → 再评估下一层该走哪条。

和上一个思维链对比着记：**思维链是"沿着一条路线一直往下想"；思维树是"每一层都先分叉，再评估、剪枝、继续展开"。**

**什么时候用**：需要在多个方案之间做选择，并且选完后还要继续规划下一步时。

完整 ToT 可以反复展开、评估和回溯；这里为了入门，演示两层分支：先选大方向，再在大方向下面选下一步。

```java
// ① 第一层：生成几条候选学习路线（要多样 → 高温）
String plans = chatClient.prompt("""
        我是 Spring AI 新手，只有2小时学习时间。
        给出3条不同的学习路线：先读文档、先跑 Demo、先改 Prompt。
        每条说明适合什么情况，并按"上手快/收获大/难度低"打分(1-10)。
        """)
        .options(ChatOptions.builder().temperature(0.7).build())
        .call().content();

// ② 剪枝：评估第一层分支，选出最适合继续展开的一条
String best = chatClient.prompt()
        .user(u -> u.text("从这些学习路线里选最适合继续展开的一条，说明理由：\n{p}").param("p", plans))
        .options(ChatOptions.builder().temperature(0.2).build())  // 评估要稳定，低温
        .call().content();

// ③ 第二层：在选中的路线下面继续生成子方案
String branchDetails = chatClient.prompt()
        .user(u -> u.text("基于选中的路线，继续展开3个下一步子方案，每个说明优缺点并打分：\n{b}")
                .param("b", best))
        .options(ChatOptions.builder().temperature(0.7).build())  // 继续展开，要多样
        .call().content();

// ④ 再次评估：从子方案里选最终执行路径
String finalChoice = chatClient.prompt()
        .user(u -> u.text("从这些子方案里选最适合新手马上执行的一条，并说明最终路径：\n{s}")
                .param("s", branchDetails))
        .options(ChatOptions.builder().temperature(0.2).build())  // 最终选择要稳定
        .call().content();
```

示例输出（节选）：

```
第一层：
├─ A. 先读文档
├─ B. 先跑 Demo  ← 评分最高，继续展开
└─ C. 先改 Prompt

第二层（展开 B）：
├─ B1. 跑完整 main 方法
├─ B2. 只跑最小 ChatClient 调用  ← 最适合新手马上执行
└─ B3. 跑 Web 示例

最终路径：B → B2。先确认 API Key 和模块路径，再运行最小 ChatClient 示例；跑通后只改一句 Prompt 对比输出。
```

💡 **价值在"继续展开"：不是三条路里选一条就结束，而是在选中的分支下面继续长出下一层分支，再评估、剪枝、往下走——这才是"树"。**

---

## 10. 自动提示词工程（Automatic Prompt Engineering）

**让 AI 自己生成多个候选提示词，再评估、挑出最好的一个**——把"找更好提示词"这件事也自动化。

**例子**：你要做一个情感分类任务，希望输出只能是 `POSITIVE/NEGATIVE/NEUTRAL`。于是先让 AI **生成
5 个候选提示词**，再让 AI **按格式约束、清晰度、减少歧义这几个标准评估并选出一个**。

```java
// ① 生成多个候选提示词（要多样 → 高温 1.0）
String prompts = chatClient.prompt("""
        我们要做情感分类任务，输入是一句中文评论，输出只能是 POSITIVE/NEGATIVE/NEUTRAL。
        请生成5个不同的候选提示词，每个都要强调只能输出这3个标签之一。
        """)
        .options(ChatOptions.builder().temperature(1.0).build())
        .call().content();

// ② 让 AI 评估这些候选提示词，挑出最适合放进示例代码的一条
String best = chatClient.prompt()
        .user(u -> u.text("按格式约束、清晰度、减少歧义三项评估，选出最好的一条并说明理由：\n{p}").param("p", prompts))
        .options(ChatOptions.builder().temperature(0.2).build())  // 评估要稳定，低温
        .call().content();
```

示例输出（节选）：

```
① AI 生成5个候选提示词：
   1. 请判断评论情感，只返回 POSITIVE/NEGATIVE/NEUTRAL
   2. 你是情感分类器，输出必须且只能是三个标签之一
   ...
② AI 评估后选出第2个——角色清楚、输出约束强，不容易额外解释
```

**什么时候用**：想批量生成候选提示词，或让 AI 帮你初步筛选更清晰的提示写法。

💡 **核心是"生成一批候选提示词 + 自动评估挑最优"。这里用模型自评做教学简化，生产环境更建议用验证集和明确指标评估。**

---

## 11. 代码提示（Code Prompting）

写编程任务的提示词，关键是**把规格说清楚**：什么语言、输入输出是什么、有什么要求。

**什么时候用**：生成、解释、调试代码。

```java
chatClient.prompt("用Python写一个函数：输入整数列表，返回平均值；如果列表为空，抛出 ValueError。只写函数。")
        .options(ChatOptions.builder().temperature(0.0).build())  // 代码要准，低温
        .call()
        .content();
```

示例输出：

```python
def average(nums):
    if not nums:
        raise ValueError("nums不能为空")
    return sum(nums) / len(nums)
```

💡 **规格越清楚，代码越精准——别让 AI 猜你要什么。**

---

## 怎么选？任务 → 模式

| 你的目标 | 用哪个 |
|----------|--------|
| 分类、翻译、摘要 | 零样本 |
| 要固定输出格式 | 少样本 |
| 定全局规则 / 格式 / 语气 | 系统提示 |
| 让 AI 扮演某个身份 | 角色提示 |
| 给背景，让回答贴合场景 | 上下文提示 |
| 先抽象、再回到具体任务 | 回退提示 |
| 数学、逻辑、多步推理 | 思维链 |
| 让结果更可靠 | 自洽性 |
| 多方案决策 / 复杂规划 / 推演后续步骤 | 思维树 |
| 自动生成并评估候选提示词 | 自动提示词工程 |
| 写代码 | 代码提示 |

> 模式可以**组合**，例如"少样本 + 思维链 + 低温"用于既要稳定格式又要推理的任务。

## 跑起来

```bash
cd springai-demo
DEEPSEEK_API_KEY=sk-xxx mvn compile exec:java -Dexec.mainClass="org.example.springai.PromptEngineeringPatterns"
```

程序依次跑完 11 个模式，外加一个**温度对照**演示：同一句话在 0.0 和 1.0 各跑两次，亲眼看"稳定 vs 多样"。
某个模式偶发网络超时会自动重试，不影响其余。

## 想深入

- 配套代码：`PromptEngineeringPatterns.java`
- 官方文档：[Spring AI · Prompt Engineering Patterns](https://docs.spring.io/spring-ai/reference/api/chat/prompt-engineering-patterns.html)
