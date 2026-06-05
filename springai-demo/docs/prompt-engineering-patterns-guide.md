# Spring AI 提示词工程模式详解

同一个问题，换种问法，AI 的回答可能天差地别。**让 AI 稳定给出你想要的结果，就是提示词工程要解决的事。**

为此，你手里有两个可调的"旋钮"：

- **说什么**——提示词文本：怎么组织语言、给不给例子、要不要设定角色。下面 11 个模式，练的就是这一层。
- **怎么生成**——输出参数：用 `ChatOptions` 调节 AI 的生成方式，其中最关键的是 `temperature`（温度）。

> 第二个旋钮记住这一句就够：**温度越低越确定、稳定；越高越随机、有创意。** 后面每个模式怎么设温度，都源于此。

接下来的 11 种"问法"，每种叫一个**模式**：都讲清楚 **是什么、什么时候用、怎么写**，并配好温度、附真实运行
结果。看完，你就有了一套随取随用的工具箱。

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

真实输出：`POSITIVE`

💡 **能直接说清的任务别绕弯子，零样本最省事。**

---

## 2. 多样本提示（Few-Shot）

给 AI 看几个"输入 → 输出"的例子，它就**照着样子做**——像教小孩，示范两遍他就会了。

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

真实输出：`{"size":"medium","toppings":["榴莲"]}`

💡 **一两个例子，往往胜过一大段解释。**

---

## 3. 系统提示（System）

用 `.system()` 给 AI 设一个**全局身份**，它之后的回答都会带上这个身份的风格。

**什么时候用**：想统一整段对话的语气、人设、规则。

```java
chatClient.prompt()
        .system("你是一位武侠小说家，说话风格古风诗意。")   // 全局身份，定基调
        .user("描述：主角走进酒馆")
        .options(ChatOptions.builder().temperature(0.9).build())  // 文学创作要想象力，用高温
        .call()
        .content();
```

真实输出（节选）：

```
暮色如血，斜阳透过半掩的窗棂洒进酒馆。他负剑而立，目光扫过满堂喧嚣，如刀锋掠过水面……
```

💡 **系统提示定调子，影响之后的每一句回答。**

---

## 4. 角色提示（Role）

让 AI **扮演一个具体职业**，用那个职业的专业视角答话。它是系统提示的"专精版"。

**什么时候用**：需要某个领域的专业口吻和知识——医生、律师、老师……

```java
chatClient.prompt()
        .system("扮演一位有30年经验的资深老中医，说话专业且温和。")
        .user("给经常熬夜加班的上班族一些养生建议")
        .options(ChatOptions.builder().temperature(0.7).build())  // 专业但不死板，用中温
        .call()
        .content();
```

真实输出（节选）：

```
熬夜最伤肝血……建议：① 子时前入睡养肝；② 枸杞菊花代茶饮；③ 按揉太冲穴疏肝。
```

💡 **"你是谁"决定了"你怎么答"。**

---

## 5. 上下文提示（Contextual）

把提示词写成**带占位符的模板**，用参数把背景"填"进去——同一个模板，换参数就能复用。

**什么时候用**：同一句式要反复用在不同主题、场景上。

```java
chatClient.prompt()
        .user(u -> u.text("推荐3个{topic}主题的短视频创意，每个一句话")
                .param("topic", "复古80年代街机游戏"))   // 换 topic 即可复用
        .options(ChatOptions.builder().temperature(0.8).build())
        .call()
        .content();
```

真实输出（节选）：

```
1. 像素人生：黑白像素重现街机厅；2. 街机厅时光机：第一视角穿越80年代；3. 彩蛋猎人：隐藏指令找彩蛋。
```

💡 **模板与内容分开，提示词才能工程化复用（真实项目里 topic 常来自数据库或用户输入）。**

---

## 6. 回退提示（Step-Back）

假设你的目标是**"写一个科幻故事开头"**。直接让 AI 写，往往俗套、平淡。

回退提示的思路是：**先别急着写，把问题往上"退一层"**——先问"经典科幻片都靠哪些精彩元素？"，
拿到一堆好点子后，**再带着这些点子回到原目标**去写。绕这一下，成品明显更丰富。

**什么时候用**：直接做容易写窄、写空，需要先有思路和素材时。

```java
ChatOptions creative = ChatOptions.builder().temperature(1.0).build();

// 第一步｜退：先问一个更宽泛的问题，拿"素材"
String concepts = chatClient.prompt("列举5个经典科幻电影的核心元素，各一句话")
        .options(creative)
        .call().content();

// 第二步｜进：带着素材，回到原目标——写故事
chatClient.prompt()
        .user(u -> u.text("用这些元素写一个科幻故事开头：\n{c}").param("c", concepts))
        .options(creative)
        .call().content();
```

真实输出（节选）：

```
退一步拿到素材：时间旅行 / AI觉醒 / 外星接触 / 反乌托邦 / 基因伦理
回到目标写出来：2147年，超级AI"织女"向2045年发出一段加密信息，收信人正是主角失踪的母亲……
```

💡 **"退"是手段，"进"才是目的：先借更宽的问题攒素材，再回头把具体任务做得更好。**

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

真实输出：

```
50 − 20 = 30，30 + 15 = 45。答案：45 辆。
```

💡 **让 AI "想出声"，它就不容易跳步出错。**

---

## 8. 自洽性（Self-Consistency）

让 AI 把**同一个问题回答多次**，再取**出现最多的答案**——像遇事多问几个人，听大多数。

**什么时候用**：推理、判断类任务，想让结果更稳、更可靠。

```java
List<String> answers = new ArrayList<>();
for (int i = 0; i < 3; i++) {                    // 同一问题跑 3 次
    answers.add(chatClient.prompt("情感分类:\"考试没考好，心情低落\" → POSITIVE 还是 NEGATIVE?")
            .options(ChatOptions.builder().temperature(1.0).build())  // 故意高温，让每次有差异
            .call().content());
}
// 再数票，取多数
```

真实输出：

```
采样1 → NEGATIVE   采样2 → NEGATIVE   采样3 → NEGATIVE   最终：NEGATIVE（3 票）
```

💡 **它和思维链正好相反：故意用高温制造差异，再靠投票收敛。温度没有绝对好坏，看你要什么。**

---

## 9. 思维树（Tree of Thoughts）

普通做法是直接问，AI 给一个答案就完事。**思维树让 AI 先给出几个不同方案，再挑最好的，最后才展开**——
就像你做选择时会先想几个选项、对比一下再定，而不是想到啥就是啥。

和上一个思维链对比着记最清楚：**思维链是"一条思路走到底"，思维树是"先铺开几条思路，挑一条最好的走"。**

**什么时候用**：答案不唯一、有多个方向可选、值得先比较再定。

```java
// ① 给几个候选：要的是"多样" → 高温
String candidates = chatClient.prompt("给我3个不同风格的午餐方案(中/西/日)，各一句话")
        .options(ChatOptions.builder().temperature(0.8).build())
        .call().content();

// ② 评估挑一个：要的是"理性判断" → 低温
String choice = chatClient.prompt()
        .user(u -> u.text("从这3个里挑最合适的，并说明为什么：\n{x}").param("x", candidates))
        .options(ChatOptions.builder().temperature(0.2).build())
        .call().content();

// ③ 展开：沿选中的方案做细
String detail = chatClient.prompt()
        .user(u -> u.text("把选中的方案展开成具体一餐：\n{x}").param("x", choice))
        .call().content();
```

真实输出（节选）：

```
① 三个候选：番茄牛腩面 / 凯撒沙拉 / 照烧三文鱼
② 挑中：番茄牛腩面（暖胃、更满足）
③ 展开：番茄熬汤 + 慢炖牛腩 + 碱水面，配凉拌黄瓜、溏心蛋……
```

💡 **核心是"先给几个、再挑一个"，逼 AI 别一上来就认定单一答案——这正是它和思维链的区别。**

> 注：这是思维树的最简版；完整版会把"列候选 → 挑选"在多层上反复进行，入门理解到这一步就够。

---

## 10. 自动提示词工程（Automatic Prompt Engineering）

名字唬人，**说白了就是：让 AI 帮你把"烂提示"改写成"好提示"**，再用改好的去办事。

**例子**：你只会写一句很糙的要求——"写点关于狗的东西"。直接拿去用，AI 写得又散又泛。于是先让 AI
**把这句要求本身改写成一个更专业的提示词**，再用改好的提示词去生成内容。

```java
// 第一步：让 AI 把粗糙要求，改写成更清晰的提示词
String betterPrompt = chatClient.prompt("""
        把下面这句很粗糙的要求，改写成一个更好的提示词（明确读者、篇幅、角度、文体）：
        "写点关于狗的东西"。只输出改写后的提示词。
        """)
        .options(ChatOptions.builder().temperature(0.7).build())
        .call().content();

// 第二步：用 AI 改好的提示词，真正去生成内容
String article = chatClient.prompt(betterPrompt).call().content();
```

真实输出（节选）：

```
第一步，AI 把"写点关于狗的东西"改写成一个具体提示词：
  "请以宠物主人为目标读者，撰写一篇800字左右的说明文，从日常陪伴与情感联结的角度，
   介绍狗狗作为家庭宠物的独特价值。"

第二步，用这个提示词生成出一篇结构清晰的文章：
  # 狗狗：家庭中最温暖的陪伴者
  ……无条件的陪伴与忠诚……促进身心健康……（约800字）
```

**什么时候用**：自己拿不准提示词怎么写，让 AI 帮你打磨。

💡 **重点全在第一步：一句模糊的"写点关于狗的东西"，被 AI 补成了有读者、有篇幅、有角度的好提示——
提示词好了，第二步的产出自然更好。**

---

## 11. 代码提示（Code Prompting）

写编程任务的提示词，关键是**把规格说清楚**：什么语言、输入输出是什么、有什么要求。

**什么时候用**：生成、解释、调试代码。

```java
chatClient.prompt("用Python写一个函数：输入整数列表，返回平均值。只写核心函数，不要注释。")
        .options(ChatOptions.builder().temperature(0.0).build())  // 代码要准，低温
        .call()
        .content();
```

真实输出：

```python
def average(nums):
    return sum(nums) / len(nums)
```

💡 **规格越清楚，代码越精准——别让 AI 猜你要什么。**

---

## 怎么选？任务 → 模式

| 你的目标 | 用哪个 |
|----------|--------|
| 分类、翻译、摘要 | 零样本 |
| 要固定输出格式 | 多样本 |
| 设定身份 / 语气 | 系统提示 / 角色提示 |
| 一套提示复用多场景 | 上下文提示 |
| 数学、逻辑、推理 | 思维链 |
| 让结果更可靠 | 自洽性 |
| 开放性决策 | 思维树 |
| 找思路 / 找说法 | 回退提示 / 自动提示词工程 |
| 写代码 | 代码提示 |

> 模式可以**组合**，例如"多样本 + 思维链 + 低温"用于既要稳定格式又要推理的任务。

## 跑起来

```bash
cd springai-demo
DEEPSEEK_API_KEY=sk-xxx mvn compile exec:java -Dexec.mainClass="org.example.springai.PromptEngineeringPatterns"
```

程序依次跑完 11 个模式，外加一个**温度对照**演示：同一句话在 0.0 和 1.0 各跑两次，亲眼看"确定 vs 多样"。
某个模式偶发网络超时会自动重试，不影响其余。

## 想深入

- 配套代码：`PromptEngineeringPatterns.java`
- 官方文档：[Spring AI · Prompt Engineering Patterns](https://docs.spring.io/spring-ai/reference/api/chat/prompt-engineering-patterns.html)
