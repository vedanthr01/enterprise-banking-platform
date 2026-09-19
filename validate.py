"""Structural sanity check (also run in CI): every pom.xml must be
well-formed XML and every .yml/.yaml file must parse as YAML.

Paths are resolved relative to this script, so it works from any checkout.
"""
import os
import sys
import xml.etree.ElementTree as ET

import yaml

ROOT = os.path.dirname(os.path.abspath(__file__))
SKIP_DIRS = {".git", "target", "node_modules", ".idea"}

poms, ymls = [], []
for dirpath, dirnames, filenames in os.walk(ROOT):
    dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
    for name in filenames:
        path = os.path.join(dirpath, name)
        if name == "pom.xml":
            poms.append(path)
        elif name.endswith((".yml", ".yaml")):
            ymls.append(path)

errors = []
for f in poms:
    try:
        ET.parse(f)
    except Exception as e:
        errors.append(f"[XML] {os.path.relpath(f, ROOT)}: {e}")

for f in ymls:
    try:
        with open(f, encoding="utf-8") as fh:
            list(yaml.safe_load_all(fh))
    except Exception as e:
        errors.append(f"[YAML] {os.path.relpath(f, ROOT)}: {e}")

if not poms:
    errors.append("No pom.xml files found - run this from the repository root.")

if errors:
    print(f"FOUND {len(errors)} ISSUE(S):")
    for e in errors:
        print(" -", e)
    sys.exit(1)

print(f"OK: {len(poms)} pom.xml files and {len(ymls)} YAML files are all well-formed.")
