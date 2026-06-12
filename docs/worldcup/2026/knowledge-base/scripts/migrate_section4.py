#!/usr/bin/env python3
"""One-off: 把球队文件第4节「补充分析数据」从旧7行2列结构迁移到与11维对齐的13行3列结构。

- 保留每个文件已有的7个字段值，按新顺序归位。
- 6个新增分析行填统一占位「结构化数据公开源未取得」，符合 completion-rules，不臆造。
- 幂等：已是3列表头（| 维度 | 对应分析维度 | 内容 |）的文件自动跳过。
"""

from __future__ import annotations

import sys
from pathlib import Path

TEAMS_DIR = Path(__file__).resolve().parents[1] / "teams"

OLD_HEADER = "| 维度 | 内容 |"
NEW_HEADER = "| 维度 | 对应分析维度 | 内容 |"

PLACEHOLDER = "结构化数据公开源未取得；可结合基础信息.战术风格与对应位置球员技术特点归纳"

# 新增行：(字段名, 对应分析维度, 占位内容)
NEW_ROWS = {
    "进攻创造特征": ("5 进攻创造", PLACEHOLDER),
    "终结/锋线特征": ("6 终结能力", "结构化数据（xG/射正）公开源未取得；可用前锋国家队进球率与锋线配置代理"),
    "中场控制特征": ("7 中场控制", "结构化控球/压迫数据公开源未取得；可用中场球员俱乐部层级与阵型代理"),
    "防守组织特征": ("8 防守组织", "结构化失球数据公开源未取得；可用后防与门将俱乐部层级、中卫身高代理"),
    "转换攻防特征": ("9 转换攻防", "结构化反击数据公开源未取得；可用边路速度型球员数量与战术风格代理"),
    "教练/临场调整记录": ("11 教练与临场调整", "结构化换人影响数据公开源未取得；见教练组.战术偏好与心理/韧性记录"),
}

# 旧字段 → 对应分析维度（用于补第2列）
OLD_DIM = {
    "近 10 场正式比赛": "4 近期状态",
    "比赛节奏特征": "5/7/9 综合",
    "关键球员依赖度": "3 阵容可用性",
    "定位球攻防数据": "10 定位球攻防",
    "心理/韧性记录": "11 教练与临场调整",
    "伤病/停赛风险名单": "3 阵容可用性",
    "同大洲/强队历史战绩摘要": "1 场地与适应性 / 历史交锋",
}

# 新表最终顺序
ORDER = [
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
]


def parse_old_values(block_lines: list[str]) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in block_lines:
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) != 2:
            continue
        key, val = cells
        if key in ("维度", "---") or set(key) <= {"-"}:
            continue
        values[key] = val
    return values


def migrate(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()

    # 定位第4节范围
    start = next((i for i, l in enumerate(lines) if l.startswith("## 4.")), None)
    if start is None:
        return "无第4节，跳过"
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("## 5.")), len(lines))

    section = lines[start:end]
    if any(l.strip() == NEW_HEADER for l in section):
        return "已是新结构，跳过"
    if not any(l.strip() == OLD_HEADER for l in section):
        return "未识别旧结构，跳过"

    old_vals = parse_old_values(section)

    # 构建新表
    new_tbl = [NEW_HEADER, "| --- | --- | --- |"]
    for field in ORDER:
        if field in NEW_ROWS:
            dim, content = NEW_ROWS[field]
        else:
            dim = OLD_DIM[field]
            content = old_vals.get(field, "公开源未取得")
        new_tbl.append(f"| {field} | {dim} | {content} |")

    # 用新表替换旧表（保留 ## 4. 标题行与其后的空行/说明，重建表格部分）
    # 找到旧表头行的位置
    tbl_start = next(i for i, l in enumerate(section) if l.strip() == OLD_HEADER)
    # 表格结束：表头之后第一个非表格行
    tbl_end = tbl_start + 1
    while tbl_end < len(section) and section[tbl_end].lstrip().startswith("|"):
        tbl_end += 1

    new_section = section[:tbl_start] + new_tbl + section[tbl_end:]
    new_lines = lines[:start] + new_section + lines[end:]
    path.write_text("\n".join(new_lines) + "\n", encoding="utf-8")
    return f"已迁移（{len(old_vals)} 个旧字段保留）"


def main() -> int:
    targets = sys.argv[1:] or [p.stem for p in sorted(TEAMS_DIR.glob("*.md"))]
    for slug in targets:
        path = TEAMS_DIR / f"{slug}.md"
        if not path.exists():
            print(f"{slug:32} 文件不存在")
            continue
        print(f"{slug:32} {migrate(path)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
