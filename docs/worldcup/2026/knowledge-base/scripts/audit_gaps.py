#!/usr/bin/env python3
"""Audit completion gaps in World Cup team markdown files."""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
TEAMS_DIR = ROOT / "teams"
REQUIRED_SECTIONS = (
    "## 1. 基础信息",
    "## 2. 教练组",
    "## 3. 球员",
    "## 4. 补充分析数据",
    "## 5. 来源",
)
SUPPLEMENTAL_FIELDS = (
    "近 10 场正式比赛",
    "进攻创造特征",
    "终结/锋线特征",
    "中场控制特征",
    "防守组织特征",
    "转换攻防特征",
    "定位球攻防数据",
    "教练/临场调整记录",
    "比赛节奏特征",
    "关键球员依赖度",
    "心理/韧性记录",
    "伤病/停赛风险名单",
    "同大洲/强队历史战绩摘要",
)
PLAYER_ROW_RE = re.compile(r"^\| (?:待补充|\d+)\s*\|")


@dataclass
class TeamAudit:
    team: str
    file: str
    player_rows: int
    todo_count: int
    public_unavailable_count: int
    physical_todo_count: int
    physical_public_unavailable_count: int
    supplemental_field_count: int
    missing_supplemental_fields: list[str]
    matchday_confirmation_count: int
    xg_numeric: bool
    xga_numeric: bool
    clean_sheet_mentioned: bool
    source_conflict_count: int
    missing_sections: list[str]

    @property
    def status(self) -> str:
        if self.missing_sections:
            return "结构缺失"
        if self.player_rows < 23:
            return "球员不足"
        if self.missing_supplemental_fields:
            return "补充字段缺失"
        if self.todo_count:
            return "待补充"
        if self.public_unavailable_count:
            return "公开源缺口"
        return "已补齐"


def slug_to_path(slug: str) -> Path:
    path = TEAMS_DIR / f"{slug}.md"
    if not path.exists():
        raise FileNotFoundError(f"Unknown team slug: {slug}")
    return path


def team_files(selected: Iterable[str] | None) -> list[Path]:
    if selected:
        return [slug_to_path(slug) for slug in selected]
    return sorted(TEAMS_DIR.glob("*.md"))


def audit_file(path: Path) -> TeamAudit:
    text = path.read_text(encoding="utf-8")
    missing_sections = [section for section in REQUIRED_SECTIONS if section not in text]
    supplemental_fields = set()
    try:
        file_label = str(path.relative_to(ROOT))
    except ValueError:
        file_label = str(path)
    player_rows = 0
    physical_todo_count = 0
    physical_public_unavailable_count = 0
    for line in text.splitlines():
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if cells and cells[0] in SUPPLEMENTAL_FIELDS:
            supplemental_fields.add(cells[0])
        if not PLAYER_ROW_RE.match(line):
            continue
        player_rows += 1
        if len(cells) >= 5:
            physical_cell = cells[4]
            if "待补充" in physical_cell:
                physical_todo_count += 1
            if "公开源未取得" in physical_cell:
                physical_public_unavailable_count += 1
    missing_supplemental_fields = [
        field for field in SUPPLEMENTAL_FIELDS if field not in supplemental_fields
    ]
    return TeamAudit(
        team=path.stem,
        file=file_label,
        player_rows=player_rows,
        todo_count=text.count("待补充"),
        public_unavailable_count=text.count("公开源未取得"),
        physical_todo_count=physical_todo_count,
        physical_public_unavailable_count=physical_public_unavailable_count,
        supplemental_field_count=len(supplemental_fields),
        missing_supplemental_fields=missing_supplemental_fields,
        matchday_confirmation_count=text.count("待赛前确认"),
        xg_numeric=bool(re.search(r"\bxG\s*场均\s*\d", text)),
        xga_numeric=bool(re.search(r"\bxGA\s*场均\s*\d", text)),
        clean_sheet_mentioned="零封率" in text,
        source_conflict_count=text.count("来源冲突"),
        missing_sections=missing_sections,
    )


def print_table(audits: list[TeamAudit]) -> None:
    headers = [
        "team",
        "players",
        "补充字段",
        "待补充",
        "公开源未取得",
        "身高脚待补",
        "身高脚公开缺",
        "P1待确认",
        "xG",
        "xGA",
        "零封率",
        "来源冲突",
        "status",
    ]
    rows = [
        [
            audit.team,
            str(audit.player_rows),
            f"{audit.supplemental_field_count}/{len(SUPPLEMENTAL_FIELDS)}",
            str(audit.todo_count),
            str(audit.public_unavailable_count),
            str(audit.physical_todo_count),
            str(audit.physical_public_unavailable_count),
            str(audit.matchday_confirmation_count),
            "Y" if audit.xg_numeric else "-",
            "Y" if audit.xga_numeric else "-",
            "Y" if audit.clean_sheet_mentioned else "-",
            str(audit.source_conflict_count),
            audit.status,
        ]
        for audit in audits
    ]
    totals = [
        "TOTAL",
        str(sum(a.player_rows for a in audits)),
        f"{sum(a.supplemental_field_count for a in audits)}/{len(SUPPLEMENTAL_FIELDS) * len(audits)}",
        str(sum(a.todo_count for a in audits)),
        str(sum(a.public_unavailable_count for a in audits)),
        str(sum(a.physical_todo_count for a in audits)),
        str(sum(a.physical_public_unavailable_count for a in audits)),
        str(sum(a.matchday_confirmation_count for a in audits)),
        str(sum(1 for a in audits if a.xg_numeric)),
        str(sum(1 for a in audits if a.xga_numeric)),
        str(sum(1 for a in audits if a.clean_sheet_mentioned)),
        str(sum(a.source_conflict_count for a in audits)),
        "",
    ]
    rows.append(totals)
    widths = [len(header) for header in headers]
    for row in rows:
        widths = [max(width, len(cell)) for width, cell in zip(widths, row)]

    def fmt(row: list[str]) -> str:
        return " | ".join(cell.ljust(width) for cell, width in zip(row, widths))

    print(fmt(headers))
    print("-+-".join("-" * width for width in widths))
    for row in rows:
        print(fmt(row))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--teams",
        nargs="+",
        help="Team file slugs without .md, for example: canada united-states",
    )
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON")
    args = parser.parse_args()

    audits = [audit_file(path) for path in team_files(args.teams)]
    if args.json:
        print(json.dumps([asdict(audit) | {"status": audit.status} for audit in audits], ensure_ascii=False, indent=2))
    else:
        print_table(audits)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
