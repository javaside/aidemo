# Spring AI Prompt Engineering Video Script

**Format:** 16:9 teaching video
**Target length:** about 6 minutes
**Style:** technical, dark canvas, Spring green accents
**Source material:** `springai-demo/docs/prompt-engineering-patterns-guide.md` and `PromptEngineeringPatterns.java`

## Scene 1: Hook

Prompt engineering is not magic. It is how we tell the model what we want, how we want it formatted, and how much freedom it has while generating.

In this video, we use Spring AI's `ChatClient` to walk through 11 practical prompt engineering patterns.

## Scene 2: The Mental Model

Remember two layers.

First: prompt text, or how you say it. This includes instructions, examples, roles, context, and output constraints.

Second: generation parameters, or how the model generates. In Spring AI, we use `ChatOptions` for settings like `temperature`, `topP`, and `maxTokens`.

## Scene 3: Basic Patterns

Start simple.

Zero-shot means no examples, just a clear instruction. It works well for simple classification, translation, and summaries.

Few-shot means giving examples first, so the model learns the format.

System prompting sets the rules for the current request or conversation.

Role prompting gives the model a useful perspective, like a patient Spring AI teacher.

Contextual prompting adds background so the answer fits the situation.

## Scene 4: Reasoning Patterns

For harder tasks, we guide the model's reasoning.

Step-back prompting first asks for higher-level principles, then returns to the concrete task.

Chain of Thought asks the model to reason step by step.

Self-consistency samples multiple reasoning paths and votes on the answer.

Tree of Thoughts goes further: branch, score, prune, then branch again.

## Scene 5: Tree of Thoughts Visual

Tree of Thoughts is not just "choose one of three options."

First, we open several branches. For example: read docs, run a demo, or edit a prompt.

Then we score those branches and keep the strongest one.

But the tree does not stop there. Under the selected branch, we create the next layer: run the full main method, run a minimal `ChatClient` call, or run the web demo.

Then we choose the next step. That is the tree: expand, evaluate, prune, continue.

## Scene 6: Automatic Prompt Engineering And Code Prompting

Automatic Prompt Engineering asks the model to generate candidate prompts, then evaluate which one is clearer.

For code prompting, the most important thing is a clear specification: language, input, output, and edge cases.

The example asks Python to average a list and explicitly handle an empty list with `ValueError`.

## Scene 7: How To Choose

Use zero-shot for simple direct tasks.

Use few-shot when format matters.

Use system and role prompts when behavior or voice matters.

Use context when the situation matters.

Use reasoning patterns when the model needs to compare, calculate, or plan.

Use code prompting when you need implementation help.

## Scene 8: Close

The real skill is not memorizing 11 names.

It is seeing the task clearly enough to choose the right pattern: direct instruction, examples, rules, context, reasoning, or code specification.

That is prompt engineering in practice.
