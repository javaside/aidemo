package org.example.langchain4j.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JSON {
    private static ObjectMapper objectMapper = new ObjectMapper();
//    static {
//        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
//    }

    public static String toJSON(Object object) {
        return objectMapper.valueToTree(object).toString();
    }

    public static <T> T fromJSON(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
