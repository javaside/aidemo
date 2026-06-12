---
title: 2026 World Cup National Team Knowledge Base
snapshot_date: 2026-06-12
format: markdown
scope: national-teams
---

# 世界杯国家队知识库

本目录用于沉淀国家队维度的世界杯分析资料，支持后续对任意两支球队做结构化对比。

## 重要口径

- 用户原始目标提到“全部 32 支参赛队”，但 2026 年男足世界杯为 48 支参赛队。本文档按 48 队知识库设计，扩展追踪表中保留“32 队目标”备注，便于兼容旧赛制分析。
- 本知识库当前是 2026-06-12 的赛前快照。球员俱乐部、球衣号码、伤病、停赛、预计首发、FIFA 排名会变化。
- 无法从当前公开资料确认的字段统一写“待补充”，不使用猜测值。
- 球员年龄统一以 2026-06-11，世界杯开幕日为基准。

## 文件结构

```text
docs/worldcup/2026/knowledge-base/
├── README.md
├── analysis-dimensions.md
├── completion-rules.md
├── team-template.md
├── expansion-tracker.md
├── sources.md
├── scripts/
│   └── audit_gaps.py
└── teams/
    ├── argentina.md
    ├── bosnia-and-herzegovina.md
    ├── brazil.md
    ├── canada.md
    ├── czechia.md
    ├── england.md
    ├── france.md
    ├── germany.md
    ├── mexico.md
    ├── netherlands.md
    ├── portugal.md
    ├── paraguay.md
    ├── south-africa.md
    ├── south-korea.md
    ├── spain.md
    └── united-states.md
```

## 已建球队

优先顺序：卫冕冠军 + 当前热门球队 + 开幕日/比赛日球队。

| 顺序 | 国家队 | 文件 | 当前状态 |
| --- | --- | --- | --- |
| 1 | 阿根廷 | `teams/argentina.md` | 已建骨架，已录入 26 人名单 |
| 2 | 法国 | `teams/france.md` | 已建骨架，已录入 26 人名单 |
| 3 | 西班牙 | `teams/spain.md` | 已建骨架，已录入 26 人名单 |
| 4 | 英格兰 | `teams/england.md` | 已建骨架，已录入 26 人名单 |
| 5 | 巴西 | `teams/brazil.md` | 已建骨架，已录入 26 人名单 |
| 6 | 葡萄牙 | `teams/portugal.md` | 已建骨架，已录入 27 人名单，需核对最终注册人数 |
| 7 | 德国 | `teams/germany.md` | 已建骨架，已录入 26 人名单 |
| 8 | 荷兰 | `teams/netherlands.md` | 已建骨架，已录入 26 人名单 |
| 9 | 墨西哥 | `teams/mexico.md` | 开幕日球队，已录入 26 人名单 |
| 10 | 南非 | `teams/south-africa.md` | 开幕日球队，已录入 26 人名单 |
| 11 | 韩国 | `teams/south-korea.md` | 开幕日球队，已录入 26 人名单 |
| 12 | 捷克 | `teams/czechia.md` | 开幕日球队，已录入 26 人名单 |
| 13 | 加拿大 | `teams/canada.md` | 2026-06-12 比赛日球队，已录入 26 人名单 |
| 14 | 波黑 | `teams/bosnia-and-herzegovina.md` | 2026-06-12 比赛日球队，已录入 26 人名单 |
| 15 | 美国 | `teams/united-states.md` | 2026-06-12 比赛日球队，已录入 26 人名单 |
| 16 | 巴拉圭 | `teams/paraguay.md` | 2026-06-12 比赛日球队，已录入 26 人名单 |

## 今日比赛日补充

赛程来源：竞彩网足球竞猜赛程，页面更新时间 2026-06-12 12:11:01。

| 赛事编号 | 比赛 | 开赛时间（北京时间） | 知识库文件 |
| --- | --- | --- | --- |
| 周五003 | 加拿大 vs 波黑 | 2026-06-13 03:00 | `teams/canada.md`、`teams/bosnia-and-herzegovina.md` |
| 周五004 | 美国 vs 巴拉圭 | 2026-06-13 09:00 | `teams/united-states.md`、`teams/paraguay.md` |

## 对比分析用法

任意两队赛前对比时，先读取 `analysis-dimensions.md`，固定使用其第 1 节定义的五块输出格式：“数据速览表 + 关键维度柱状图 + 赛果倾向指数柱状图 + 关键对位卡 + 读图结论”（以 `analysis-dimensions.md` 为准，本处不重复细节）。球队资料读取顺序如下：

1. `基础信息`：比较历史成绩、FIFA 排名、常用阵型、整体强弱项。
2. `教练组`：比较主教练经验、战术偏好、临场保守/激进程度。
3. `球员`：按门将、后卫、中场、前锋逐线比较深度。
4. `补充分析数据`：比较近况、定位球、节奏、心理韧性、伤停风险、强强对话记录。

## 建库策略：滚动建库，不一次性堆 48 队

本届 48 队，但**不提前一次性建满**。原因：球员名单是 P0 死数据，但伤病/停赛/预计首发是 P1，会随时间失效——过早建好的队到开赛前 P1 已过期，反而误导。

因此采用**按赛程滚动建库**：

1. 已建 16 队为「卫冕冠军+热门+开幕日/比赛日」核心，长期维护。
2. 其余队**在其比赛前 1-2 天再建**，确保 P1（伤停/首发）新鲜。
3. 临场分析任意两队时，若对手尚未建库，先用 `team-template.md` 现场补建该队 P0+P1，再做对比。
4. 覆盖率不是目标，**P1 新鲜度才是**。不要为凑「48 队全建」而提前堆简介。

## 更新原则

1. 官方来源优先：FIFA、各国足协、赛事官方名单。
2. 媒体来源只作为补充：FourFourTwo、BBC、Guardian、Sky Sports、Al Jazeera、DAZN 等。
3. 伤病、停赛、预计首发必须标注时间点。
4. 每次更新国家队文件时，更新 `last_updated` 与 `data_status`。
5. 新增球队时从 `team-template.md` 复制结构，保留所有字段位。
6. 补全“待补充”字段时遵循 `completion-rules.md`，先补 P0/P1，无法确认的信息保持缺口说明。
7. 每批补全前后运行 `scripts/audit_gaps.py` 记录缺口变化。
8. 数据成熟度边界见 `analysis-dimensions.md` 第 5.1 节：维度 7/9/10 因免费源限制为永久代理，不强行填充。
