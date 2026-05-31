# API Key 配置说明

## 概述

所有 springai-demo 模块的示例代码已统一改为从环境变量获取 API Key，避免硬编码，方便统一管理。

## 配置方式

### 方式 1：设置环境变量（推荐）

**macOS / Linux:**
```bash
export DEEPSEEK_API_KEY=sk-your-actual-key-here
```

**Windows (CMD):**
```cmd
set DEEPSEEK_API_KEY=sk-your-actual-key-here
```

**Windows (PowerShell):**
```powershell
$env:DEEPSEEK_API_KEY="sk-your-actual-key-here"
```

### 方式 2：在 IDE 中配置

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. 选择要运行的类
3. Environment Variables 中添加：`DEEPSEEK_API_KEY=sk-your-key-here`

**VS Code:**
在 `.vscode/launch.json` 中添加：
```json
{
  "configurations": [
    {
      "env": {
        "DEEPSEEK_API_KEY": "sk-your-key-here"
      }
    }
  ]
}
```

### 方式 3：命令行运行时指定

```bash
DEEPSEEK_API_KEY=sk-your-key-here mvn compile exec:java -Dexec.mainClass="org.example.springai.StructuredOutPutDemo"
```

## 永久配置（推荐）

将环境变量添加到 shell 配置文件中：

**Bash (~/.bashrc 或 ~/.bash_profile):**
```bash
export DEEPSEEK_API_KEY=sk-your-actual-key-here
```

**Zsh (~/.zshrc):**
```bash
export DEEPSEEK_API_KEY=sk-your-actual-key-here
```

配置后执行 `source ~/.zshrc` 或重启终端生效。

## 验证配置

```bash
# 检查环境变量是否设置
echo $DEEPSEEK_API_KEY

# 运行任意示例验证
cd springai-demo
mvn compile exec:java -Dexec.mainClass="org.example.springai.StructuredOutPutDemo"
```

## 错误提示

如果未设置环境变量，程序会输出警告信息：

```
⚠️  警告：未设置环境变量 DEEPSEEK_API_KEY
   请设置环境变量：export DEEPSEEK_API_KEY=sk-your-key-here
   当前使用默认值（可能无法正常工作）
```

## 技术实现

所有示例代码通过 `ApiKeyConfig.getDeepSeekApiKey()` 统一获取 API Key：

```java
import org.example.springai.config.ApiKeyConfig;

DeepSeekApi deepSeekApi = DeepSeekApi.builder()
    .baseUrl("https://api.deepseek.com")
    .apiKey(ApiKeyConfig.getDeepSeekApiKey())  // 从环境变量获取
    .build();
```

## 安全建议

1. ✅ **不要**将 API Key 提交到 Git 仓库
2. ✅ **不要**在代码中硬编码 API Key
3. ✅ **使用**环境变量或密钥管理服务
4. ✅ **定期**轮换 API Key
5. ✅ **限制** API Key 的权限范围

## 已修改的文件列表

以下文件已全部改为从环境变量获取 API Key：

- StructuredOutPutDemo.java
- StructuredOutputAdvancedDemo.java
- SpringAiRagDemo.java
- ToolsDemo.java
- PromptEngineeringPatterns.java
- RetrievalAugmentationAdvisorDemo.java
- AdvisorDemo.java
- ChatClientUserDemo.java
- DeepSeekChatModelDemo.java
- DeepSeekApiDemo.java
- DeepSeekMemoryDemo.java
- ChatClientObservabilityDemo.java
- ToolCallbackDemo.java
- ChatMemoryDemo.java
- DeepSeekAiDemo.java
