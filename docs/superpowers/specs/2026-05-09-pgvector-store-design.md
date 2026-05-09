# PgVector Store 演示设计

## 目标

在 springai-demo 模块中创建 `PgVectorStoreDemo`，演示使用编程方式（非自动装配）配置 PgVectorStore 向量数据库。

## 技术选型

- **向量数据库**：PostgreSQL + PgVector
- **Embedding 模型**：Ollama（本地运行，无需 API key）
- **连接方式**：编程方式创建 JdbcTemplate + PgVectorStore

## 实现内容

### 1. 依赖配置
在 pom.xml 中添加：
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

### 2. 演示类 PgVectorStoreDemo.java

包含以下功能演示：
1. **编程方式创建 VectorStore**：使用 `PgVectorStore.builder(jdbcTemplate, embeddingModel).build()`
2. **文档操作**：添加文档、相似度搜索
3. **过滤查询**：使用 FilterExpression 过滤元数据
4. **删除操作**：按 ID 和过滤条件删除

### 3. 配置说明

使用 `application.properties` 配置数据库连接：
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 4. 运行要求

需要本地运行 PgVector：
```bash
docker run -it --rm --name postgres -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  pgvector/pgvector
```

## 文件清单

- `springai-demo/src/main/java/org/example/springai/PgVectorStoreDemo.java` - 演示类
- `springai-demo/pom.xml` - 添加依赖

## 验收标准

- [ ] 代码可运行，展示完整的向量存储功能
- [ ] 使用编程方式创建 PgVectorStore（不使用 @EnableAutoConfiguration）
- [ ] 包含添加、搜索、过滤、删除操作的完整演示