#!/usr/bin/env python3

from __future__ import annotations

import subprocess
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_PATH = REPO_ROOT / "docs" / "tree.md"
EXCLUDED_PATHS = {
    "docs/tree.md",
}


def tracked_paths() -> list[str]:
    result = subprocess.run(
        ["git", "ls-files", "--cached"],
        cwd=REPO_ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    paths = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    return sorted(path for path in paths if path not in EXCLUDED_PATHS)


def build_tree(paths: list[str]) -> list[str]:
    root: dict[str, dict] = {}
    for path in paths:
        cursor = root
        for part in path.split("/"):
            cursor = cursor.setdefault(part, {})

    lines = [REPO_ROOT.name]

    def walk(node: dict[str, dict], prefix: str = "") -> None:
        names = sorted(node.keys())
        for index, name in enumerate(names):
            is_last = index == len(names) - 1
            branch = "└── " if is_last else "├── "
            lines.append(f"{prefix}{branch}{name}")
            walk(node[name], prefix + ("    " if is_last else "│   "))

    walk(root)
    return lines


def write_tree() -> None:
    tree_lines = build_tree(tracked_paths())
    content = "\n".join(
        [
            "# Project Tree",
            "",
            "Generated from tracked repository files by `scripts/generate_tree.py`.",
            "",
            "```text",
            *tree_lines,
            "```",
            "",
        ]
    )
    OUTPUT_PATH.write_text(content, encoding="utf-8")


if __name__ == "__main__":
    write_tree()
