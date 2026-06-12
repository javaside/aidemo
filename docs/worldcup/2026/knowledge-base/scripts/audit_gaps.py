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
    source_conflict_count: int
    missing_sections: list[str]

    @property
    def status(self) -> str:
        if self.missing_sections:
            return "结构缺失"
        if self.player_rows < 23:
            return "球员不足"
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
    player_rows = 0
    physical_todo_count = 0
    physical_public_unavailable_count = 0
    for line in text.splitlines():
        if not PLAYER_ROW_RE.match(line):
            continue
        player_rows += 1
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if len(cells) >= 5:
            physical_cell = cells[4]
            if "待补充" in physical_cell:
                physical_todo_count += 1
            if "公开源未取得" in physical_cell:
                physical_public_unavailable_count += 1
    return TeamAudit(
        team=path.stem,
        file=str(path.relative_to(ROOT)),
        player_rows=player_rows,
        todo_count=text.count("待补充"),
        public_unavailable_count=text.count("公开源未取得"),
        physical_todo_count=physical_todo_count,
        physical_public_unavailable_count=physical_public_unavailable_count,
        source_conflict_count=text.count("来源冲突"),
        missing_sections=missing_sections,
    )


def print_table(audits: list[TeamAudit]) -> None:
    headers = ["team", "players", "待补充", "公开源未取得", "身高脚待补", "身高脚公开缺", "来源冲突", "status"]
    rows = [
        [
            audit.team,
            str(audit.player_rows),
            str(audit.todo_count),
            str(audit.public_unavailable_count),
            str(audit.physical_todo_count),
            str(audit.physical_public_unavailable_count),
            str(audit.source_conflict_count),
            audit.status,
        ]
        for audit in audits
    ]
    totals = [
        "TOTAL",
        str(sum(a.player_rows for a in audits)),
        str(sum(a.todo_count for a in audits)),
        str(sum(a.public_unavailable_count for a in audits)),
        str(sum(a.physical_todo_count for a in audits)),
        str(sum(a.physical_public_unavailable_count for a in audits)),
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
