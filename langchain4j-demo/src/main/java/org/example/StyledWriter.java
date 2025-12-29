package org.example;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface StyledWriter {

    @Agent
    String writeStoryWithStyle(@V("topic") String topic, @V("style") String style);
}
