package org.example.commom;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 根据McpSchema.JsonSchema自动生成McpSchema.CallToolRequest的参数
 */
public class JsonSchemaArgumentGenerator {
    
    private static final Random random = new Random();

    /**
     * 根据JsonSchema生成参数Map
     * @param jsonSchema JsonSchema定义
     * @return 生成的参数Map
     */
    public static Map<String, Object> generateArgumentsFromSchema(McpSchema.JsonSchema jsonSchema) {
        Map<String, Object> arguments = new HashMap<>();
        
        if (jsonSchema == null || jsonSchema.properties() == null) {
            return arguments;
        }
        
        // 遍历所有属性并生成对应的值
        for (Map.Entry<String, Object> entry : jsonSchema.properties().entrySet()) {
            String propertyName = entry.getKey();
            Object propertySchema = entry.getValue();
            
            // 处理属性定义
            Object value = generateValueForProperty(propertySchema);
            if (value != null) {
                arguments.put(propertyName, value);
            }
        }
        
        // 如果有必需字段，确保它们被包含
        if (jsonSchema.required() != null) {
            for (String requiredField : jsonSchema.required()) {
                if (!arguments.containsKey(requiredField)) {
                    // 为必需字段生成默认值
                    Object defaultValue = generateDefaultValueForRequiredField(jsonSchema, requiredField);
                    if (defaultValue != null) {
                        arguments.put(requiredField, defaultValue);
                    }
                }
            }
        }
        
        return arguments;
    }
    
    /**
     * 为属性生成对应的值
     * @param propertySchema 属性Schema定义
     * @return 生成的值
     */
    private static Object generateValueForProperty(Object propertySchema) {
        // 如果是SchemaObj类型，根据类型信息生成值
        if (propertySchema instanceof SchemaObj) {
            SchemaObj schemaObj = (SchemaObj) propertySchema;
            return generateValueByType(schemaObj.type(), schemaObj.description());
        }
        
        // 如果是Map类型，尝试提取type字段
        if (propertySchema instanceof Map) {
            Map<String, Object> propertyMap = (Map<String, Object>) propertySchema;
            
            // 检查是否有type字段
            Object typeObj = propertyMap.get("type");
            if (typeObj instanceof String) {
                String type = (String) typeObj;
                String description = (String) propertyMap.get("description");
                return generateValueByType(type, description);
            }
            // 如果没有type字段，尝试从其他可能的字段获取类型信息
            else if (propertyMap.containsKey("typeHint")) {
                String type = (String) propertyMap.get("typeHint");
                String description = (String) propertyMap.get("description");
                return generateValueByType(type, description);
            }
        }
        
        return null;
    }
    
    /**
     * 为必需字段生成默认值
     * @param jsonSchema JsonSchema定义
     * @param fieldName 字段名
     * @return 生成的默认值
     */
    private static Object generateDefaultValueForRequiredField(McpSchema.JsonSchema jsonSchema, String fieldName) {
        Object propertySchema = jsonSchema.properties().get(fieldName);
        return generateValueForProperty(propertySchema);
    }
    
    /**
     * 根据类型生成值
     * @param type 类型
     * @param description 描述（可用于生成更合适的值）
     * @return 生成的值
     */
    private static Object generateValueByType(String type, String description) {
        if (type == null) {
            return null;
        }
        
        switch (type.toLowerCase()) {
            case "string":
            case "str":
                return generateStringValue(description);
            case "number":
            case "integer":
            case "int":
                return generateIntegerValue(description);
            case "boolean":
            case "bool":
                return generateBooleanValue();
            case "array":
                return generateArrayValue(description);
            case "object":
                // 对于对象类型，返回一个空Map或根据子属性生成值
                return new HashMap<>();
            default:
                // 如果类型不明确，尝试根据描述或默认生成字符串
                return generateStringValue(description);
        }
    }
    
    /**
     * 生成字符串值
     * @param description 描述
     * @return 生成的字符串值
     */
    private static String generateStringValue(String description) {
        if (description != null) {
            // 根据描述生成更合适的值
            if (description.toLowerCase().contains("name")) {
                return "testName";
            } else if (description.toLowerCase().contains("operation") || 
                      description.toLowerCase().contains("operator")) {
                return "+";
            } else if (description.toLowerCase().contains("city")) {
                return "Beijing";
            } else if (description.toLowerCase().contains("country")) {
                return "CN";
            }
        }
        return "testValue";
    }
    
    /**
     * 生成整数值
     * @param description 描述
     * @return 生成的整数值
     */
    private static Integer generateIntegerValue(String description) {
        if (description != null) {
            // 根据描述生成可能更合适的值
            if (description.toLowerCase().contains("a")) {
                return 10;
            } else if (description.toLowerCase().contains("b")) {
                return 5;
            }
        }
        return random.nextInt(100);
    }
    
    /**
     * 生成布尔值
     * @return 生成的布尔值
     */
    private static Boolean generateBooleanValue() {
        return random.nextBoolean();
    }
    
    /**
     * 生成数组值
     * @param description 描述
     * @return 生成的数组值
     */
    private static Object[] generateArrayValue(String description) {
        return new Object[0]; // 返回空数组作为默认值
    }
}