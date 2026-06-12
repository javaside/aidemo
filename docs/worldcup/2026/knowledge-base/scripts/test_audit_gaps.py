#!/usr/bin/env python3
"""Tests for the World Cup knowledge-base audit script."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from audit_gaps import SUPPLEMENTAL_FIELDS, audit_file


PLAYER_TABLE_HEADER = """| 号码 | 姓名 | 全名 | 出生日期/年龄 | 身高/惯用脚 | 俱乐部/联赛 | 国家队出场/进球 | 世界杯经历 | 当前状态 | 技术特点 | 大赛经验 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"""


def player_rows(count: int = 23) -> str:
    return "\n".join(
        f"| {idx} | Player {idx} | Player {idx} Full | 2000-01-01 / 26 | 180 cm / 右脚 | Club / League | 1/0 | 2026 注册名单 | 入选名单 | 技术特点 | 中 |"
        for idx in range(1, count + 1)
    )


def team_markdown(supplemental_rows: list[str]) -> str:
    return f"""---
team_id: TST
team_name_zh: 测试队
team_name_en: Test Team
---

# 测试队

## 1. 基础信息

| 字段 | 内容 |
| --- | --- |
| 球队全称 | Test Team |

## 2. 教练组

| 字段 | 内容 |
| --- | --- |
| 主教练 | Test Coach |

## 3. 球员

### 门将

{PLAYER_TABLE_HEADER}
{player_rows()}

## 4. 补充分析数据

| 维度 | 对应分析维度 | 内容 |
| --- | --- | --- |
{chr(10).join(supplemental_rows)}

## 5. 来源

- Test source
"""


class AuditGapsTest(unittest.TestCase):
    def write_team(self, text: str) -> Path:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = Path(directory.name) / "test-team.md"
        path.write_text(text, encoding="utf-8")
        return path

    def test_audit_tracks_analysis_readiness_fields(self) -> None:
        rows = []
        for field in SUPPLEMENTAL_FIELDS:
            content = "已取得"
            if field == "进攻创造特征":
                content = "FootyStats xG 场均 1.23"
            if field == "防守组织特征":
                content = "FootyStats xGA 场均 0.91，零封率 40%"
            if field == "伤病/停赛风险名单":
                content = "核心球员待赛前确认"
            rows.append(f"| {field} | 测试 | {content} |")

        audit = audit_file(self.write_team(team_markdown(rows)))

        self.assertEqual(audit.player_rows, 23)
        self.assertEqual(audit.supplemental_field_count, len(SUPPLEMENTAL_FIELDS))
        self.assertEqual(audit.missing_supplemental_fields, [])
        self.assertTrue(audit.xg_numeric)
        self.assertTrue(audit.xga_numeric)
        self.assertTrue(audit.clean_sheet_mentioned)
        self.assertEqual(audit.matchday_confirmation_count, 1)
        self.assertEqual(audit.status, "已补齐")

    def test_audit_reports_missing_supplemental_fields(self) -> None:
        audit = audit_file(
            self.write_team(
                team_markdown(["| 近 10 场正式比赛 | 4 近期状态 | 近况已取得 |"])
            )
        )

        self.assertEqual(audit.supplemental_field_count, 1)
        self.assertIn("定位球攻防数据", audit.missing_supplemental_fields)
        self.assertEqual(audit.status, "补充字段缺失")


if __name__ == "__main__":
    unittest.main()
