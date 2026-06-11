# Prompt Engineering HyperFrames Video

This folder contains a HyperFrames-style video source for a Spring AI prompt engineering explainer.

## Files

- `DESIGN.md` defines the visual identity.
- `script.md` contains the narration and scene structure.
- `index.html` contains the animated 16:9 composition source.

## Current Environment Note

`npx hyperframes` could not run in this sandbox because npm registry DNS resolution is blocked. The source is therefore authored as a self-contained HTML composition with HyperFrames-style `data-composition-id` metadata and a timeline shim for browser preview.

When HyperFrames CLI is available locally, run:

```bash
cd docs/videos/prompt-engineering-hyperframes
npx hyperframes lint
npx hyperframes inspect
npx hyperframes render --output prompt-engineering-spring-ai.mp4 --quality high
```

## Browser Preview

Open `index.html` in a local browser or serve the folder with any static server. The composition autoplays a compressed preview timeline and supports seeking with the range control.
