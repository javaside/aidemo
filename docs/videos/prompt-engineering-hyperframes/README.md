# 提示词工程 HyperFrames 讲解视频

这个目录包含一套 HyperFrames 风格的视频源文件，用于制作 Spring AI 提示词工程讲解视频。

## 文件说明

- `DESIGN.md`：视频视觉规范。
- `script.md`：旁白脚本和场景结构。
- `index.html`：16:9 动画组合源文件。

## 当前环境说明

当前沙箱无法运行 `npx hyperframes`，原因是 npm registry 的 DNS 解析被阻断。因此，这里先提供一个自包含的 HTML 组合源文件：它带有 HyperFrames 风格的 `data-composition-id` 元数据，并内置了用于浏览器预览的时间轴适配逻辑。

当本地可以使用 HyperFrames CLI 后，可以运行：

```bash
cd docs/videos/prompt-engineering-hyperframes
npx hyperframes lint
npx hyperframes inspect
npx hyperframes render --output prompt-engineering-spring-ai.mp4 --quality high
```

## 浏览器预览

可以直接在本地浏览器打开 `index.html`，也可以用任意静态文件服务器启动该目录。页面会自动播放压缩后的预览时间轴，并支持通过进度条拖动查看不同场景。
