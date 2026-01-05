package org.example.langchain4j;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelWorkFlowTest {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        OpenAiChatModel BASE_MODEL = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();

        FoodExpert foodExpert = AgenticServices
                .agentBuilder(FoodExpert.class)
                .chatModel(BASE_MODEL)
                .outputKey("meals")
                .build();

        MovieExpert movieExpert = AgenticServices
                .agentBuilder(MovieExpert.class)
                .chatModel(BASE_MODEL)
                .outputKey("movies")
                .build();

        EveningPlannerAgent eveningPlannerAgent = AgenticServices
                .parallelBuilder(EveningPlannerAgent.class)
                .subAgents(movieExpert,foodExpert)
                .executor(executorService)
                .outputKey("plans")
                .output(agenticScope -> {
                    List<String> movies = agenticScope.readState("movies", List.of());
                    List<String> meals = agenticScope.readState("meals", List.of());

                    List<EveningPlan> moviesAndMeals = EveningPlannerAgent.createPlans(movies,meals);
                    return moviesAndMeals;
                })
                .build();

        List<EveningPlan> plans = eveningPlannerAgent.plan("romantic");
        plans.forEach(System.out::println);
        executorService.shutdown();
    }
}
