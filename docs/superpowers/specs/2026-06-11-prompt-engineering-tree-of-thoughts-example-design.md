# Prompt Engineering Tree of Thoughts Example Design

## Goal

将 `springai-demo` 中第 9 个提示工程模式“思维树（Tree of Thoughts）”的示例，从井字棋策略替换为更贴合项目读者的“新手 2 小时学习 Spring AI 的路线选择”。

## Background

当前示例使用井字棋说明“生成候选 -> 评估最佳 -> 向前推演”。虽然比象棋更简单，但仍要求读者理解中心、角、边、对手应招、双线威胁等游戏规则。这个额外背景会分散新手对思维树本身的理解。

项目定位是 AI 框架入门演示，读者更容易代入“我该怎么学习 Spring AI”这类场景。因此新示例应避免额外领域规则，让读者只关注思维树的核心流程。

## Scope

修改以下两个文件中的第 9 节内容：

- `springai-demo/docs/prompt-engineering-patterns-guide.md`
- `springai-demo/src/main/java/org/example/springai/PromptEngineeringPatterns.java`

不修改其它提示工程模式，不新增运行时依赖，不改 API Key、模型配置或 Maven 结构。

## User-Facing Behavior

新示例使用“新手只有 2 小时学习 Spring AI”作为任务：

1. 生成 3 条候选学习路线，例如“先读文档”“先跑 Demo”“先改 Prompt”。
2. 评估每条路线对新手的收益、难度和反馈速度，并选出最推荐的一条。
3. 基于选中的路线，继续推演接下来的执行步骤和可能卡点。

文档解释应强调：

- 思维链是一条路线走到底。
- 思维树是先展开多条路线，再评估选择一条，继续向下细化。
- “树”的价值在于先比较分支，再推演后续，而不是一次性拍脑袋选方案。

## Code Design

保留 `treeOfThoughts()` 方法和三段式 ChatClient 调用结构：

1. `plans`：高温生成 3 个学习方案，保证候选多样。
2. `best`：低到中温评估候选，选出最适合新手的一条。
3. 返回最终推演：围绕选中方案输出一个简短学习安排、下一步动作和卡点处理。

控制输出长度，避免示例变成长篇学习规划。代码注释继续保持新手友好，说明每一步对应 ToT 的哪个环节。

## Verification

先运行一个文本检查，确认旧示例仍在且新示例不存在，作为 RED。

修改后运行文本检查，确认：

- 文档和 Java 示例中第 9 节不再使用井字棋。
- 文档和 Java 示例包含“2小时”“Spring AI”“学习路线”等新示例关键词。

再运行 Maven 编译检查：

- `mvn -pl springai-demo -am compile`

如果编译因本地环境或网络受限失败，需要记录具体原因，不编造成功结果。
