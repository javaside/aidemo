# Prompt Engineering Tree Of Thoughts Example Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Tree of Thoughts demo with a beginner-friendly Spring AI learning-route example.

**Architecture:** Keep the existing single-class demo and Markdown guide structure. Only update the Tree of Thoughts section in the guide and the `treeOfThoughts()` method in the Java example.

**Tech Stack:** Java 21 style text blocks, Spring AI `ChatClient`, Maven, Markdown.

---

### Task 1: RED Check For Existing Example

**Files:**
- Read: `springai-demo/docs/prompt-engineering-patterns-guide.md`
- Read: `springai-demo/src/main/java/org/example/springai/PromptEngineeringPatterns.java`

- [ ] **Step 1: Run the failing target-state check**

```bash
if rg -n "井字棋|中心/角/边|对手最可能下哪|双线威胁" springai-demo/docs/prompt-engineering-patterns-guide.md springai-demo/src/main/java/org/example/springai/PromptEngineeringPatterns.java >/tmp/tot-old.txt; then
  echo "FAIL: old Tree of Thoughts tic-tac-toe example is still present"
  cat /tmp/tot-old.txt
  exit 1
fi
```

Expected: FAIL, because the old tic-tac-toe example is still present before implementation.

### Task 2: Update Markdown Guide

**Files:**
- Modify: `springai-demo/docs/prompt-engineering-patterns-guide.md`

- [ ] **Step 1: Replace section 9 with the learning-route example**

Use this section structure:

```markdown
## 9. 思维树（Tree of Thoughts）

**像做学习计划一样思考**：不急着认定一种学法，而是先列出几条可选路线 → 比较哪条最适合新手 → 再把选中的路线继续拆成下一步行动。

和思维链对比着记：**思维链是"沿着一条路线往下想"；思维树是"先展开几条路线，选一条，再继续细化它"。**

**什么时候用**：需要在多个方案之间做选择，并且选完后还要继续规划下一步时。
```

Then show Java code with three variables: `plans`, `best`, and `nextSteps`.

- [ ] **Step 2: Include a short realistic output**

The output should show:

```text
① 三条路线：读文档 / 跑 Demo / 改 Prompt
② 推荐：先跑 Demo，再改 Prompt
③ 推演：先运行一个最小示例，看到结果后只改一处提示词；如果失败，优先检查 API Key 和启动命令。
```

### Task 3: Update Java Demo

**Files:**
- Modify: `springai-demo/src/main/java/org/example/springai/PromptEngineeringPatterns.java`

- [ ] **Step 1: Update the method comment**

Describe the new demo as:

```java
 * 【本示例演示】（用新手学习 Spring AI 的路线选择，避免额外游戏规则）
 * 新手只有 2 小时时间：① 生成3条学习路线 ② 评估选出最适合的一条
 * ③ 基于选中的路线继续推演执行步骤和可能卡点。
```

- [ ] **Step 2: Update `treeOfThoughts()` prompts**

Use `plans`, `best`, and `nextSteps` naming. The first prompt should ask for 3 routes for a beginner learning Spring AI in 2 hours. The second prompt should evaluate which route has the best beginner feedback loop. The final prompt should return 3-4 short sentences with concrete steps and one likely blocker.

### Task 4: Verify And Commit

**Files:**
- Verify: `springai-demo/docs/prompt-engineering-patterns-guide.md`
- Verify: `springai-demo/src/main/java/org/example/springai/PromptEngineeringPatterns.java`

- [ ] **Step 1: Run target-state text checks**

```bash
rg -n "2小时|Spring AI|学习路线|先跑 Demo|改 Prompt" springai-demo/docs/prompt-engineering-patterns-guide.md springai-demo/src/main/java/org/example/springai/PromptEngineeringPatterns.java
```

Expected: matches in both files.

```bash
if rg -n "井字棋|中心/角/边|对手最可能下哪|双线威胁" springai-demo/docs/prompt-engineering-patterns-guide.md springai-demo/src/main/java/org/example/springai/PromptEngineeringPatterns.java; then
  echo "FAIL: old tic-tac-toe wording remains"
  exit 1
fi
```

Expected: PASS with no output from `rg`.

- [ ] **Step 2: Compile the module**

```bash
mvn -pl springai-demo -am compile
```

Expected: build success, unless local dependency/network configuration prevents Maven from resolving artifacts.

- [ ] **Step 3: Commit implementation**

```bash
git add springai-demo/docs/prompt-engineering-patterns-guide.md springai-demo/src/main/java/org/example/springai/PromptEngineeringPatterns.java
git commit -m "docs(springai): 简化思维树为学习路线示例"
```
