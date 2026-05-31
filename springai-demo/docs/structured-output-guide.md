# Spring AI 结构化输出完整指南

## 概述

结构化输出是 Spring AI 的核心特性，让 AI 返回的内容直接转换为类型安全的 Java 对象，无需手动解析 JSON。

## 示例代码结构

### 1. 基础示例：`StructuredOutPutDemo.java`
**适合人群**：新手入门

**覆盖内容**：
- ✅ `entity(Class<T>)` - 返回单个对象
- ✅ `entity(ParameterizedTypeReference<T>)` - 返回泛型集合
- ✅ `entity(StructuredOutputConverter<T>)` - 显式传入转换器
- ✅ `BeanOutputConverter` 手动使用 - 理解内部工作流程
- ✅ `responseEntity(Class<T>)` - 同时获取原始响应

**运行方式**：
```bash
cd springai-demo
mvn compile exec:java -Dexec.mainClass="org.example.springai.StructuredOutPutDemo"
```

### 2. 高级示例：`StructuredOutputAdvancedDemo.java`
**适合人群**：需要处理复杂场景的开发者

**覆盖内容**：
- ✅ `ListOutputConverter` - 简单字符串列表
- ✅ `MapOutputConverter` - 灵活的 Map 结构
- ✅ 枚举类型 - 分类、状态、级别等场景
- ✅ 嵌套复杂对象 - 多层结构处理
- ✅ 错误处理 - 格式不正确时的降级方案
- ✅ `responseEntity(ParameterizedTypeReference<T>)` - 泛型集合 + 原始响应

**运行方式**：
```bash
cd springai-demo
mvn compile exec:java -Dexec.mainClass="org.example.springai.StructuredOutputAdvancedDemo"
```

## 核心转换器对比

| 转换器 | 适用场景 | 类型安全 | 灵活性 |
|--------|---------|---------|--------|
| `BeanOutputConverter` | 固定结构的业务对象 | ✅ 强类型 | ⭐⭐ |
| `ListOutputConverter` | 简单字符串列表 | ✅ List<String> | ⭐ |
| `MapOutputConverter` | 动态键值对 | ❌ 需手动转换 | ⭐⭐⭐ |
| 自定义 Converter | 特殊格式或复杂逻辑 | ✅ 可定制 | ⭐⭐⭐ |

## 最佳实践

### 1. 选择合适的转换器
```java
// ✅ 推荐：固定结构用 BeanOutputConverter
ActorsFilms result = chatClient.prompt()
    .user("列出周星驰的电影")
    .call()
    .entity(ActorsFilms.class);

// ✅ 推荐：简单列表用 ListOutputConverter
List<String> cities = chatClient.prompt()
    .user("列出5个城市")
    .call()
    .entity(new ListOutputConverter(new DefaultConversionService()));

// ⚠️ 谨慎：动态结构才用 MapOutputConverter（失去类型安全）
Map<String, Object> data = chatClient.prompt()
    .user("返回产品信息")
    .call()
    .entity(new MapOutputConverter());
```

### 2. 提高格式稳定性
```java
// ✅ 使用 temperature=0 提高格式一致性
DeepSeekChatOptions.builder()
    .temperature(0.0)  // 降低随机性
    .build()

// ✅ 在提示词中明确要求 JSON 格式
chatClient.prompt()
    .user("""
        请列出演员信息。
        只返回 JSON 格式，不要有其他文字说明。
        """)
```

### 3. 错误处理
```java
try {
    ActorsFilms result = chatClient.prompt()
        .user("查询演员信息")
        .call()
        .entity(ActorsFilms.class);
    
    // 业务逻辑
} catch (Exception e) {
    // 记录原始响应用于调试
    logger.error("结构化输出解析失败", e);
    
    // 降级方案：使用非结构化输出
    String fallback = chatClient.prompt()
        .user("查询演员信息")
        .call()
        .content();
}
```

### 4. 使用枚举约束输出
```java
// ✅ 定义枚举类型
enum BookGenre {
    SCIENCE_FICTION, FANTASY, MYSTERY, ROMANCE
}

record BookReview(String title, BookGenre genre, int rating) {}

// BeanOutputConverter 会在 JSON Schema 中列出所有枚举值
// 引导 AI 只返回预定义的值
```

### 5. 嵌套对象处理
```java
// ✅ 定义嵌套结构
record CompanyProfile(
    String name,
    Address headquarters,      // 嵌套对象
    List<Product> products     // 嵌套对象列表
) {}

record Address(String city, String country) {}
record Product(String name, String description) {}

// BeanOutputConverter 会递归生成完整的 JSON Schema
```

## 常见问题

### Q1: 什么时候用 `entity()` vs `responseEntity()`？
- **entity()**：只需要业务数据，代码更简洁
- **responseEntity()**：需要查看 token 用量、metadata、原始响应等信息

### Q2: 为什么 AI 有时返回格式不正确？
- 提示词不够明确（未要求 JSON 格式）
- temperature 太高导致输出不稳定
- 模型能力限制（建议使用 GPT-4、Claude 等高级模型）

### Q3: 如何处理可选字段？
```java
// ✅ 使用 Java 的可空类型
record UserProfile(
    String name,           // 必填
    String email,          // 必填
    String phone           // 可选（可能为 null）
) {}

// 或使用 Optional
record UserProfile(
    String name,
    String email,
    Optional<String> phone
) {}
```

### Q4: 能否自定义转换逻辑？
可以，实现 `StructuredOutputConverter` 接口：
```java
public class CustomConverter implements StructuredOutputConverter<MyType> {
    @Override
    public MyType convert(String source) {
        // 自定义解析逻辑
    }
    
    @Override
    public String getFormat() {
        // 返回格式提示
    }
}
```

## 学习路径

1. **第一步**：运行 `StructuredOutPutDemo`，理解 5 种基础用法
2. **第二步**：运行 `StructuredOutputAdvancedDemo`，学习高级特性
3. **第三步**：在自己的项目中应用，从简单场景开始
4. **第四步**：根据实际需求定制转换器和错误处理

## 参考资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- 项目示例代码：`springai-demo/src/main/java/org/example/springai/`
