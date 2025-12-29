package org.example;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class LoopWorkFlowTest {
    public static void main(String[] args) {
        OpenAiChatModel BASE_MODEL = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();

        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(BASE_MODEL)
                .outputKey("story")
                .build();

        StyleScorer styleScorer = AgenticServices
                .agentBuilder(StyleScorer.class)
                .chatModel(BASE_MODEL)
                .outputKey("score")
                .build();

        UntypedAgent styleReviewLoop = AgenticServices
                .loopBuilder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(5)
                .exitCondition( agenticScope -> agenticScope.readState("score", 0.0) >= 0.8)
                .build();

        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(BASE_MODEL)
                .outputKey("story")
                .build();

        StyledWriter styledWriter = AgenticServices
                .sequenceBuilder(StyledWriter.class)
                .subAgents(creativeWriter, styleReviewLoop)
                .outputKey("story")
                .build();

        String story = styledWriter.writeStoryWithStyle("dragons and wizards", "comedy");

        System.out.println(story);

    }
}
