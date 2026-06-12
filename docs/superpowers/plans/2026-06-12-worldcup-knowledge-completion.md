# World Cup Knowledge Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the World Cup knowledge base easier to complete safely, then reduce missing fields for the four 2026-06-12 matchday teams.

**Architecture:** Add a local completion policy beside the knowledge base, add a dependency-free audit script that reads team Markdown files, then use source-backed edits for team data. Keep unknown values explicit instead of inventing data.

**Tech Stack:** Markdown knowledge files, Python 3 standard library, existing Git workflow.

---

### Task 1: Completion Rules

**Files:**
- Create: `docs/worldcup/2026/knowledge-base/completion-rules.md`
- Modify: `docs/worldcup/2026/knowledge-base/README.md`

- [ ] **Step 1: Create the rules document**

Add a document defining field priorities, source hierarchy, allowed inference rules, and the correct wording for unavailable public data.

- [ ] **Step 2: Link the rules from the README**

Add `completion-rules.md` to the file tree and update the maintenance rules section so future updates follow the same evidence standard.

- [ ] **Step 3: Verify the link**

Run: `rg -n "completion-rules|补全规则" docs/worldcup/2026/knowledge-base/README.md docs/worldcup/2026/knowledge-base/completion-rules.md`

Expected: both files contain matching references.

### Task 2: Gap Audit Script

**Files:**
- Create: `docs/worldcup/2026/knowledge-base/scripts/audit_gaps.py`

- [ ] **Step 1: Implement the script**

Create a Python script that counts `待补充`, `公开源未取得`, player rows, and section presence for each team Markdown file.

- [ ] **Step 2: Run the script for all teams**

Run: `python3 docs/worldcup/2026/knowledge-base/scripts/audit_gaps.py`

Expected: table output showing one row per team file plus a total row.

- [ ] **Step 3: Run the script for matchday teams**

Run: `python3 docs/worldcup/2026/knowledge-base/scripts/audit_gaps.py --teams canada bosnia-and-herzegovina united-states paraguay`

Expected: table output showing the four matchday teams only.

### Task 3: Matchday P0/P1 Completion

**Files:**
- Modify: `docs/worldcup/2026/knowledge-base/teams/canada.md`
- Modify: `docs/worldcup/2026/knowledge-base/teams/bosnia-and-herzegovina.md`
- Modify: `docs/worldcup/2026/knowledge-base/teams/united-states.md`
- Modify: `docs/worldcup/2026/knowledge-base/teams/paraguay.md`
- Modify: `docs/worldcup/2026/knowledge-base/sources.md`
- Modify: `docs/worldcup/2026/knowledge-base/expansion-tracker.md`

- [ ] **Step 1: Gather source-backed facts**

Use FIFA, national federation pages, FourFourTwo, Guardian, U.S. Soccer, RotoWire, and Sporttery. Prefer official sources for squad numbers, injuries, suspensions, and match status.

- [ ] **Step 2: Fill only verified fields**

Replace `待补充` with confirmed values only. If a field was attempted but unavailable in public sources, write `公开源未取得` with the source date when useful.

- [ ] **Step 3: Update source registry**

Register every newly used source in `sources.md`.

- [ ] **Step 4: Re-run audits**

Run:

```bash
python3 docs/worldcup/2026/knowledge-base/scripts/audit_gaps.py --teams canada bosnia-and-herzegovina united-states paraguay
git diff --check -- docs/worldcup/2026/knowledge-base
```

Expected: fewer `待补充` entries for the four teams and no whitespace errors.

### Task 4: Commit

**Files:**
- Commit all changed knowledge-base files and the plan.

- [ ] **Step 1: Review staged files**

Run: `git diff --stat && git status --short`

Expected: only World Cup knowledge base files, the audit script, and this plan are modified; unrelated `.playwright-mcp/` remains unstaged.

- [ ] **Step 2: Commit**

Run:

```bash
git add docs/superpowers/plans/2026-06-12-worldcup-knowledge-completion.md docs/worldcup/2026/knowledge-base
git commit -m "docs(worldcup): add knowledge completion workflow"
```

Expected: commit succeeds locally; do not push.
