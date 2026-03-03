package org.example.springai.alibaba;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.MergeStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class StateGraphDemo {
    public static void main(String[] args) throws GraphStateException {
        // 定义状态策略，使用 ReplaceStrategy
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("init", new AppendStrategy());  // 使用替换策略
            return keyStrategyMap;
        };

        StateGraph stateGraph  = new StateGraph(keyStrategyFactory);
        stateGraph.addNode("node1", AsyncNodeAction.node_async(state -> Map.of("n1","v1","init","node1")));
        stateGraph.addNode("node2", AsyncNodeAction.node_async(state -> Map.of("n2","v2")));
        stateGraph.addNode("node3", AsyncNodeAction.node_async(state -> Map.of("n3","v3")));
        stateGraph.addNode("node4", AsyncNodeAction.node_async(state -> Map.of("n4","v4")));
        stateGraph.addNode("node5", AsyncNodeAction.node_async(state -> Map.of("n5","v5")));
        stateGraph.addNode("node6", AsyncNodeAction.node_async(state -> Map.of("n6","v6")));
        stateGraph.addConditionalEdges(StateGraph.START, AsyncEdgeAction.edge_async(state -> {
            Random random = new Random();
            boolean nexted = random.nextBoolean();
            System.out.println("nexted: " + nexted);
            return  nexted ? "node1" : "node6";
        }), Map.of("node1","node1","node6","node6"));
//        stateGraph.addEdge(StateGraph.START, "node1");
//        stateGraph.addEdge("node1", "node2");
//        stateGraph.addEdge("node1", "node3");
//        stateGraph.addEdge("node2", "node6");
//        stateGraph.addEdge("node3", "node6");
//        stateGraph.addEdge("node4", "node6");
//        stateGraph.addEdge("node5", "node6");
        //stateGraph.addEdge("node2", StateGraph.END);
        //stateGraph.addEdge("node3", StateGraph.END);
        stateGraph.addEdge("node6", StateGraph.END);


        CompiledGraph compile = stateGraph.compile();
        Optional<OverAllState> invoke = compile.invoke(Map.of("init", "init0"));

        invoke.ifPresent(overAllState -> {
            System.out.println(overAllState.data());
        });
    }
}
