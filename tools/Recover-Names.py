"""Reconstruct the supplied binary for auditing; never label output original source.

Requires a Vineflower jar, MCP joined.tsrg and Mojang client mappings supplied
through command-line arguments. Work/output directories should be outside Git.
"""
import argparse
import collections
import json
from pathlib import Path
import re


def mappings(mojang, tsrg):
    lines = Path(mojang).read_text(encoding="utf-8").splitlines()
    classes = {}
    for line in lines:
        if line and not line.startswith((" ", "#")) and " -> " in line:
            named, obf = line[:-1].split(" -> ")
            classes[named] = obf
    primitives = dict(zip(
        ["void", "boolean", "byte", "char", "short", "int", "long", "float", "double"],
        "VZBCSIJFD"))

    def descriptor(value):
        dimensions = value.count("[]")
        value = value.replace("[]", "")
        return "[" * dimensions + (primitives[value] if value in primitives else
            "L" + classes.get(value, value).replace(".", "/") + ";")

    members = {}
    owner = None
    for line in lines:
        if not line or line.startswith("#"):
            continue
        if not line.startswith(" "):
            owner = line[:-1].split(" -> ")[1].replace(".", "/")
            continue
        if " -> " not in line:
            continue
        signature, obf = line.strip().split(" -> ")
        signature = re.sub(r"^\d+:\d+:", "", signature)
        signature = re.sub(r":\d+(?::\d+)?$", "", signature)
        result, member = signature.split(" ", 1)
        if "(" in member:
            name, args = member[:-1].split("(", 1)
            desc = "(" + "".join(descriptor(a) for a in args.split(",") if a) + ")" + descriptor(result)
            members[(owner, obf, desc)] = name
        else:
            members[(owner, obf, None)] = member
    candidates = collections.defaultdict(set)
    owner = None
    for line in Path(tsrg).read_text(encoding="utf-8").splitlines()[1:]:
        if not line.startswith("\t"):
            owner = line.split()[0]
        elif not line.startswith("\t\t"):
            tokens = line.split()
            if len(tokens) == 3:
                obf, srg, _ = tokens
                desc = None
            elif len(tokens) == 4:
                obf, desc, srg, _ = tokens
            else:
                continue
            name = members.get((owner, obf, desc))
            if name and re.fullmatch(r"[mf]_\d+_", srg):
                candidates[srg].add(name)
    conflicts = {k: sorted(v) for k, v in candidates.items() if len(v) != 1}
    if conflicts:
        raise ValueError(f"Ambiguous member mappings: {conflicts}")
    return {k: next(iter(v)) for k, v in candidates.items()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decompiled", type=Path, required=True)
    parser.add_argument("--mojang", type=Path, required=True)
    parser.add_argument("--tsrg", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    names = mappings(args.mojang, args.tsrg)
    unresolved = collections.Counter()
    count = 0
    corrections = []
    for source in sorted(args.decompiled.rglob("*.java")):
        text = source.read_text(encoding="utf-8")
        def replace(match):
            key = match.group()
            if key not in names:
                unresolved[key] += 1
            return names.get(key, key)
        # Preserve quoted resource IDs, reflective strings and comments. Only
        # mapped Java identifier tokens should change during this recovery pass.
        tokens = re.compile(
            r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|'
            r"'(?:\\.|[^'\\])*'|//[^\n]*|/\*[\s\S]*?\*/|\b[mf]_\d+_\b"
        )
        text = tokens.sub(lambda m: replace(m) if re.fullmatch(r'[mf]_\d+_', m.group()) else m.group(), text)
        relative = source.relative_to(args.decompiled).as_posix()
        if relative == 'com/Akeltroll/dragonsofberk/entity/base/dragon/ADragonBase.java':
            # Vineflower 1.12 loses the exit of the synthetic finally iterator.
            # Reference javap: normal cleanup has ifeq 176, then goto 240;
            # exceptional cleanup has ifeq 237, then aload throwable / athrow.
            # Both terminate at iterator exhaustion, rather than spinning forever.
            broken = 'if (!var9.hasNext()) {\n               ;'
            fixed = 'if (!var9.hasNext()) {\n               break;'
            if text.count(broken) != 1:
                raise ValueError('ADragonBase finally recovery shape changed; re-audit bytecode before proceeding')
            text = text.replace(broken, fixed)
            corrections.append({'source': relative, 'method': 'changeDimension(ServerLevel, ITeleporter)',
                                'correction': 'Restore finally iterator exit from reference bytecode'})
        target = args.output / source.relative_to(args.decompiled)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8", newline="\n")
        count += 1
    report = {"javaFiles": count, "memberMappings": len(names),
              "unresolvedOccurrences": dict(sorted(unresolved.items())),
              "controlFlowCorrections": corrections}
    (args.output / "reconstruction-report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
