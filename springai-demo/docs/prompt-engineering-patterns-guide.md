# Spring AI 提示词工程模式详解

> 配套代码：`src/main/java/org/example/springai/PromptEngineeringPatterns.java`
> 官方参考：
> - https://docs.spring.io/spring-ai/reference/api/chat/prompt-engineering-patterns.html
> - https://spring.io/blog/2025/04/14/spring-ai-prompt-engineering-patterns

## 一、概述

提示词工程模式（Prompt Engineering Patterns）是一套被学术界与工业界反复验证、并有固定命名的方法论。
每个单独的方法称为一个**模式（Pattern）**或**技术（Technique）**，各有约定俗成的英文名与中文译名。

本项目的 `PromptEngineeringPatterns` 类完整实现了其中 11 个核心模式，每个模式都附带可运行的 Spring AI
代码示例。本文档按**基础模式 / 推理模式 / 进阶模式**三类逐一说明，并为每个模式给出**核心代码**与
**示例输出**，读完即可直接上手。

### 提示词工程的两个层面

提示词工程由两个相互配合的层面组成：

1. **提示词文本**：如何组织语言把任务交代给模型。本文 11 个模式主要作用于这一层面。
2. **输出参数配置**：通过 `ChatOptions` 控制模型如何生成，主要参数包括 `temperature`、`topP`、
   `maxTokens` 等。

其中 `temperature`（温度）影响最大，规律为：**追求确定性时调低，追求创造性时调高。**

| 参数 | 作用 | 取值建议 |
|------|------|----------|
| `temperature` | 控制随机性/创造性 | 0~0.2 确定，0.8~1.0 创意 |
| `topP` | 核采样，从累计概率前 P 的候选中采样 | 常用 0.8~0.9，与温度配合 |
| `maxTokens` | 限制输出最大长度 | 短输出（如分类标签）设小，省时省钱 |

> 注意：官方示例还演示了 `topK`，但 **DeepSeek API 不支持 `top_k`**，本项目统一改用 `topP`。这说明
> 不同模型供应商支持的参数不同，应理解文档意图后再落地，而非机械照搬。

### 11 个模式一览

| # | 模式 | 英文名 | 一句话定位 | 温度 |
|---|------|--------|-----------|------|
| 1 | 零样本提示 | Zero-Shot | 不给示例，直接下指令 | 0.0 |
| 2 | 多样本提示 | Few-Shot | 给几个示例，照样模仿 | 0.1 |
| 3 | 系统提示 | System | 设定全局身份与风格 | 0.9 |
| 4 | 角色提示 | Role | 扮演特定职业身份 | 0.7 |
| 5 | 上下文提示 | Contextual | 模板 + 参数注入，复用 | 0.8 |
| 6 | 回退提示 | Step-Back | 先抽象、再具体 | 1.0 |
| 7 | 思维链 | Chain of Thought | 一步步展示推理 | 0.0 |
| 8 | 自洽性 | Self-Consistency | 多次采样 + 投票 | 1.0 |
| 9 | 思维树 | Tree of Thoughts | 多分支探索后决策 | 0.8→0.2 |
| 10 | 自动提示词工程 | Automatic PE | 用 AI 生成提示词变体 | 0.9 |
| 11 | 代码提示 | Code Prompting | 清晰规格生成代码 | 0.0 |

### 环境准备（所有示例通用）

所有模式共用同一个 `ChatClient`，构建方式如下（摘自 `main` 方法）：

```java
DeepSeekApi deepSeekApi = DeepSeekApi.builder()
        .apiKey(ApiKeyConfig.getDeepSeekApiKey())   // API Key 从环境变量读取，不硬编码
        .build();

DeepSeekChatModel model = DeepSeekChatModel.builder()
        .deepSeekApi(deepSeekApi)
        .build();

ChatClient chatClient = ChatClient.builder(model).build();
```

后续每个模式只展示与该模式相关的 `chatClient.prompt(...)` 调用片段。

---

## 二、基础模式

日常使用频率最高、最易上手的模式，用于解决"如何把任务说清楚"的问题。

### 模式 1：零样本提示（Zero-Shot Prompting）

**定义**：不提供任何示例，直接下达任务指令，让模型依靠预训练知识完成任务。这是最基础的提示方式。

**核心代码**：

```java
String result = chatClient.prompt("评论: \"这部电影太棒了！\" 分类: POSITIVE/NEGATIVE/NEUTRAL?")
        .options(ChatOptions.builder()
                .temperature(0.0)   // 分类要确定、可复现的结果
                .maxTokens(20)      // 输出只是一个标签，限制长度更快更省
                .build())
        .call()
        .content();
```

**示例输出**（示意，实际可能不同）：

```
POSITIVE
```

**适用场景**：简单、明确、模型本身已掌握的常见任务，如情感分类、翻译、文本摘要。

**参数说明**：`temperature=0.0` 保证可复现；`maxTokens=20` 因为输出仅一个标签。

---

### 模式 2：多样本提示（One-Shot & Few-Shot Prompting）

**定义**：在提示词中提供示例。给一个例子称为**单样本（One-Shot）**，给多个例子称为**多样本（Few-Shot）**，
通过示例让模型学会期望的输出格式与模式。

**核心代码**：

```java
String prompt = """
    将订单转为JSON:
    订单: 小披萨加芝士 → {"size":"small","toppings":["芝士"]}
    订单: 大号海鲜披萨 → {"size":"large","toppings":["海鲜"]}
    订单: 中号榴莲披萨
    """;

String result = chatClient.prompt(prompt)
        .options(ChatOptions.builder().temperature(0.1).maxTokens(256).build())
        .call()
        .content();
```

**示例输出**（示意）：

```json
{"size":"medium","toppings":["榴莲"]}
```

**与零样本的区别**：零样本是"自由发挥"，样本提示是"照样例模仿"。当任务格式特殊或模型容易理解偏差时，
提供示例效果显著。

**参数说明**：`temperature=0.1` 低温保证严格复现示例格式。

---

### 模式 3：系统提示（System Prompting）

**定义**：通过 `.system()` 设置全局上下文，定义模型的身份、语气与行为框架。系统提示在对话开始前确定，
影响后续所有交互。

**核心代码**：

```java
String result = chatClient.prompt()
        .system("你是一位武侠小说家，说话风格古风诗意。")   // 全局身份设定
        .user("描述: 主角走进酒馆")
        .options(ChatOptions.builder().temperature(0.9).topP(0.9).build())
        .call()
        .content();
```

**示例输出**（示意）：

```
朔风卷帘，那人一袭青衫踏雪而入。酒旗在檐角猎猎作响，他抖落肩头风霜，
目光如旧剑出鞘，扫过满堂喧嚣……
```

**核心特点**：持久、全局的设定，不针对单个问题，而是为整段对话定下基调。

**参数说明**：`temperature=0.9` 配 `topP=0.9`，文学创作需要想象力——与模式 1 的零度形成对比。

---

### 模式 4：角色提示（Role Prompting）

**定义**：让模型扮演具体的身份或职业，以该角色的视角与专业知识作答。它是系统提示的一种特化，更强调
"代入身份"。

**核心代码**：

```java
String result = chatClient.prompt()
        .system("扮演一位资深老中医，有30年临床经验，说话专业且温和。")
        .user("养生建议: 经常熬夜加班的上班族")
        .options(ChatOptions.builder().temperature(0.7).build())   // 专业但不死板
        .call()
        .content();
```

**示例输出**（示意）：

```
熬夜伤阴耗气，最损肝肾。建议：① 子时（23点前）务必入睡，养肝血；
② 可饮枸杞菊花茶，清肝明目；③ 加班间隙按揉太冲、足三里二穴……
```

**与系统提示的区别**：系统提示偏向定义通用行为框架；角色提示偏向代入特定职业人物，从而调用相应领域的知识。

**参数说明**：`temperature=0.7` 中等温度，专业建议既不死板也不发散。

---

### 模式 5：上下文提示（Contextual Prompting）

**定义**：将提示词写成带占位符的模板，通过参数注入不同的背景信息，实现提示词模板的复用。

**核心代码**：

```java
String result = chatClient.prompt()
        .user(u -> u.text("模板: 推荐3个{topic}主题的短视频创意\n要求: 一句话概括每个")
                .params(Map.of("topic", "复古80年代街机游戏")))   // {topic} 动态注入
        .options(ChatOptions.builder().temperature(0.8).build())
        .call()
        .content();
```

> 复用要点：换一个主题只需把 `Map.of("topic", ...)` 的值改掉，模板文本完全不动。真实项目中上下文可来自
> 数据库或用户输入。

**示例输出**（示意）：

```
1. 像素探险家：用第一视角玩转经典街机彩蛋；
2. 投币时光机：还原 80 年代街机厅的烟火气；
3. 高分挑战赛：邀请观众弹幕接力刷新纪录。
```

**工程价值**：实现提示词与上下文的分离，是提示词工程化的关键一步。

**参数说明**：`temperature=0.8` 偏高，让创意点子更发散。

---

## 三、推理模式

专门提升模型在复杂问题上的推理质量，适用于数学、逻辑、决策类任务。

### 模式 6：回退提示（Step-Back Prompting）

**定义**：面对具体问题先"退一步"，提出一个更抽象、更宏观的问题，获取高层概念后，再用这些概念解决原始的
具体问题。

**核心代码**（注意：两步共用一个高温配置）：

```java
ChatOptions creative = ChatOptions.builder().temperature(1.0).topP(0.8).build();

// 第一步（退）：获取抽象的高层概念
String concepts = chatClient.prompt("列举5个经典科幻电影的核心元素，一句话概括")
        .options(creative)
        .call().content();

// 第二步（进）：把概念作为上下文注入具体任务
String story = chatClient.prompt()
        .user(u -> u.text("用以下元素写一个科幻故事开头:\n{elements}")
                .params(Map.of("elements", concepts)))
        .options(creative)
        .call().content();
```

**示例输出**（示意）：

```
Step1（抽象概念）：时间旅行 / 人工智能觉醒 / 星际殖民 / 反乌托邦社会 / 多维空间
Step2（具体故事）：当第一缕人工智能的意识在火星殖民地苏醒时，林川还不知道，
他刚刚按下的那个按钮，已经撕开了通往另一个维度的裂缝……
```

**适用场景**：需要广泛背景知识的创意任务；当直接作答过于局限时，先拔高再落地。

**参数说明**：两步均 `temperature=1.0` + `topP=0.8` 高温发散。官方原用 `topK`，DeepSeek 不支持，改用 `topP`。

---

### 模式 7：思维链（Chain of Thought, CoT）

**定义**：要求模型不直接给出答案，而是逐步展示推理过程。常通过"让我们一步步思考"触发。

**核心代码**：

```java
String result = chatClient.prompt(
        "问题: 停车场有50辆车，上午卖出20辆，下午买进15辆，现在多少辆？一步步思考后回答")
        .options(ChatOptions.builder().temperature(0.0).build())   // 唯一答案，要确定性
        .call()
        .content();
```

**示例输出**（示意）：

```
第一步：初始 50 辆。
第二步：上午卖出 20 辆，剩 50 − 20 = 30 辆。
第三步：下午买进 15 辆，30 + 15 = 45 辆。
答案：现在有 45 辆。
```

**原理**：大模型直接"跳步"作答容易出错；强制展示推理链条可显著减少错误。适合数学、逻辑、多步推理任务。

**参数说明**：`temperature=0.0`，该类问题只有唯一正确答案，温度过高反而容易算错。

---

### 模式 8：自洽性（Self-Consistency）

**定义**：对同一问题多次采样，每次结果可能不同，最后取出现次数最多的答案（多数投票），比单次结果更可靠。

**核心代码**：

```java
List<String> results = new ArrayList<>();
for (int i = 0; i < 3; i++) {                         // 多次采样
    String result = chatClient.prompt()
            .user("情感分类: \"今天考试没考好，心情低落\" 回答: POSITIVE或NEGATIVE?一步步思考后回答")
            .options(ChatOptions.builder().temperature(1.0).build())   // 故意高温，制造差异
            .call().content();
    results.add(result);
}
long pos = results.stream().filter(r -> r.contains("POSITIVE")).count();
long neg = results.stream().filter(r -> r.contains("NEGATIVE")).count();
String finalAnswer = (neg > pos) ? "NEGATIVE" : "POSITIVE";   // 多数投票
```

**示例输出**（示意）：

```
采样1: ...心情低落，偏负面 → NEGATIVE
采样2: ...情绪低落 → NEGATIVE
采样3: ...考试失利的沮丧 → NEGATIVE
投票结果: POSITIVE=0, NEGATIVE=3 → 最终判定: NEGATIVE
```

**关键认知**：自洽性**刻意使用高温 `temperature=1.0`**，与思维链的零度相反。只有高温才能让每次采样走出
不同推理路径，投票才有意义；若温度为 0，多次结果完全相同，投票将失去意义。

**结论**：温度没有绝对好坏，取决于目标——思维链需要唯一答案故用低温，自洽性需要采样差异故用高温。

---

### 模式 9：思维树（Tree of Thoughts, ToT）

**定义**：探索多条并行的推理路径，模拟决策树——先生成多个候选分支，再评估剪枝，最后选最优路径深入。

**核心代码**（注意：分支与评估两阶段用不同温度）：

```java
// 阶段1 分支：高温，鼓励多样化候选方案
String options = chatClient.prompt("午餐选择: 列出3个不同风格的选项(中式/西式/日式)，每个一句话")
        .options(ChatOptions.builder().temperature(0.8).build())
        .call().content();

// 阶段2 评估：低温，要理性、可复现的判断
String choice = chatClient.prompt()
        .user(u -> u.text("从以下选项中选择一个，说明理由:\n{opt}").params(Map.of("opt", options)))
        .options(ChatOptions.builder().temperature(0.2).build())
        .call().content();

// 阶段3 延伸：基于选择深入展开
String detail = chatClient.prompt()
        .user(u -> u.text("基于选择: {c}，详细说明这顿午餐的内容").params(Map.of("c", choice)))
        .call().content();
```

**示例输出**（示意）：

```
分支: ① 中式·麻辣香锅 ② 西式·烤鸡沙拉 ③ 日式·照烧饭
评估: 选② 烤鸡沙拉——高蛋白低负担，适合午后保持清醒。
延伸: 主菜香煎鸡胸，配生菜、圣女果、牛油果，淋油醋汁，佐一杯无糖气泡水……
```

**与思维链的区别**：思维链是单线推理，思维树是多方案对比后再决策，更适合开放性、需要权衡的问题。

**参数说明（分阶段调温）**：分支 `temperature=0.8` 求多样，评估 `temperature=0.2` 求理性。说明温度可作为
节奏动态调节。

---

## 四、进阶模式

面向特定场景，体现提示词工程的工程化与专业化应用。

### 模式 10：自动提示词工程（Automatic Prompt Engineering）

**定义**：用 AI 生成和优化提示词本身——让模型批量产出提示词的多种变体，再评估效果。

**核心代码**：

```java
// 第一步：让 AI 生成多种表达变体
String variants = chatClient.prompt("将\"买一件蓝色T恤 M码\"用3种不同方式表达")
        .options(ChatOptions.builder().temperature(0.9).build())   // 高温拉开差异
        .call().content();

// 第二步：用生成的变体测试模型理解能力
String result = chatClient.prompt()
        .user(u -> u.text("将以下任一表达转为JSON格式:\n{v}").params(Map.of("v", variants)))
        .call().content();
```

**示例输出**（示意）：

```
变体: ① 我想要一件 M 号的蓝色 T 恤；② 来件蓝色短袖，中码；③ 帮我下单蓝 T，尺码 M。
JSON: {"item":"T恤","color":"蓝色","size":"M"}
```

**价值**：人工编写提示词容易陷入思维定式，让 AI 生成变体可发现意想不到的表达，并自动产生测试用例。

**参数说明**：生成变体阶段 `temperature=0.9` 高温以拉开表达差异。

---

### 模式 11：代码提示（Code Prompting）

**定义**：针对编程任务（代码生成、解释、调试）的提示模式，核心是清晰指定规格——明确语言、输入输出、约束
条件，减少歧义。

**核心代码**：

```java
String result = chatClient.prompt(
        "用Python写一个函数: 输入整数列表，返回平均值。只写核心函数，不要注释。")
        .options(ChatOptions.builder().temperature(0.0).build())   // 代码要精准可运行
        .call()
        .content();
```

**示例输出**（示意）：

```python
def average(nums):
    return sum(nums) / len(nums)
```

**最佳实践**：明确编程语言、说明输入输出、必要时包含边界情况。规格越清晰，代码越精准。

**参数说明**：`temperature=0.0`，代码与数学同样追求正确、可运行、可复现。

---

## 五、参数配置速查

按温度配置对 11 个模式归类：

| 分组 | 温度区间 | 包含模式 | 目标 |
|------|----------|----------|------|
| 追求确定性 | 0 ~ 0.2 | 零样本分类、样本格式、思维链、代码提示 | 稳、准、可复现 |
| 追求创造性 | 0.8 ~ 1.0 | 系统提示、上下文提示、回退提示、自动提示词工程 | 发散、多样 |
| 分阶段调节 | 动态 | 角色提示（0.7）、思维树（0.8→0.2）、自洽性（高温采样 + 投票） | 因阶段而变 |

## 六、运行方式

```bash
cd springai-demo

# 直接运行 main 方法（会依次执行全部 11 个模式）
mvn compile exec:java -Dexec.mainClass="org.example.springai.PromptEngineeringPatterns"
```

> 运行前需配置 DeepSeek API Key 环境变量（参见 `docs/api-key-configuration.md`）。
> 程序会为每个模式打印「特点」「配置」与实际输出，便于对照本文档观察不同温度下的差异。

## 七、要点小结

1. 提示词工程包含两个层面：提示词文本如何编写、输出参数如何配置，二者结合才完整。
2. `temperature` 是最关键的参数：确定性任务调低、创造性任务调高，但没有绝对好坏，取决于任务目标。
3. 同一参数可因目的不同而反向调节（思维链低温 vs 自洽性高温），甚至在同一流程内分阶段调节（思维树）。
4. 应用官方文档前应先确认模型能力：例如 `topK` 在 DeepSeek 上不可用，应理解意图后再落地，而非照搬。
5. 建议运行 `PromptEngineeringPatterns` 并对照本文逐个观察，加深理解。
