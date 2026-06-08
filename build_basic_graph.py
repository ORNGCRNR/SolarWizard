#!/usr/bin/env python3
import json
from pathlib import Path
import re
from collections import defaultdict, Counter

ROOT = Path('.').resolve()
detect = json.loads(Path('graphify-out/.graphify_detect.json').read_text(encoding='utf-8'))
code_files = [Path(p) for p in detect.get('code', [])]
# normalize windows paths
code_files = [ (ROOT / p).resolve() for p in detect.get('code', []) ]

package_re = re.compile(r'^\s*package\s+([a-zA-Z0-9_.]+)\s*;')
import_re = re.compile(r'^\s*import\s+([a-zA-Z0-9_.]+)(?:\s*;|\s*\.\*\s*;)')
class_re = re.compile(r'\bclass\s+([A-Za-z0-9_]+)')

# map package.Class -> file
pkg_class_to_file = {}
file_nodes = {}

for f in code_files:
    try:
        text = Path(f).read_text(encoding='utf-8')
    except Exception:
        text = ''
    pkg = None
    for line in text.splitlines():
        m = package_re.match(line)
        if m:
            pkg = m.group(1)
            break
    class_names = class_re.findall(text)
    main_class = class_names[0] if class_names else f.stem
    if pkg:
        key = pkg + '.' + main_class
    else:
        key = main_class
    pkg_class_to_file[key] = str(f.relative_to(ROOT)).replace('\\','/')
    file_nodes[str(f.relative_to(ROOT)).replace('\\','/')] = {
        'path': str(f.relative_to(ROOT)).replace('\\','/'),
        'package': pkg or '',
        'class': main_class,
    }

# build edges via imports
edges = []
for f in code_files:
    try:
        text = Path(f).read_text(encoding='utf-8')
    except Exception:
        text = ''
    imports = import_re.findall(text)
    src = str(Path(f).relative_to(ROOT)).replace('\\','/')
    for imp in imports:
        # try to resolve exact class or package.*
        if imp.endswith('.*'):
            imp_base = imp[:-2]
            # connect to any file whose package startswith imp_base
            for key, target in pkg_class_to_file.items():
                if key.startswith(imp_base + '.') or key == imp_base:
                    edges.append({'source': src, 'target': target, 'label': 'imports'})
        else:
            # imp may be package.Class
            target = None
            if imp in pkg_class_to_file:
                target = pkg_class_to_file[imp]
            else:
                # try matching by package only
                for key, t in pkg_class_to_file.items():
                    if key.startswith(imp + '.') or key.startswith(imp + '$'):
                        target = t
                        break
            if target:
                edges.append({'source': src, 'target': target, 'label': 'imports'})

# Add README and pom.xml as doc nodes linking to main app
docs = detect.get('document', [])
nodes = []
for path, meta in file_nodes.items():
    nodes.append({'id': path, 'label': meta.get('class') or Path(path).name, 'type': 'code', 'meta': meta})
for d in docs:
    dpath = str(Path(d).as_posix())
    nodes.append({'id': dpath, 'label': Path(d).name, 'type': 'doc'})
    # link README to MainApp if present
    if Path(d).name.lower().startswith('readme'):
        # find MainApp.java
        for k, meta in file_nodes.items():
            if meta.get('class','').lower()=='mainapp':
                edges.append({'source': dpath, 'target': k, 'label': 'documents'})
                break

# compute degrees
deg = Counter()
for e in edges:
    deg[e['source']] += 1
    deg[e['target']] += 1

# god nodes: top 5 by degree
god_nodes = [n for n,_ in deg.most_common(5)]

# surprising connections: edges between different top-level packages
surprises = []
for e in edges:
    s_pkg = file_nodes.get(e['source'],{}).get('package','').split('.')[0] if e['source'] in file_nodes else ''
    t_pkg = file_nodes.get(e['target'],{}).get('package','').split('.')[0] if e['target'] in file_nodes else ''
    if s_pkg and t_pkg and s_pkg!=t_pkg:
        surprises.append({'source': e['source'], 'target': e['target'], 's_pkg': s_pkg, 't_pkg': t_pkg})

# suggested questions: pick cross-package edges
questions = []
if surprises:
    q = f"How does {surprises[0]['source']} relate to {surprises[0]['target']}?"
    questions.append(q)
else:
    questions.append('What are the main modules and how are they connected?')

out = {
    'nodes': nodes,
    'edges': edges,
}
Path('graphify-out/graph.json').write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding='utf-8')

# write simple report
report_lines = []
report_lines.append('# Graph Report\n')
report_lines.append('## God Nodes\n')
for g in god_nodes:
    report_lines.append(f'- {g}')
report_lines.append('\n## Surprising Connections\n')
for s in surprises[:10]:
    report_lines.append(f'- {s["source"]} ({s["s_pkg"]}) → {s["target"]} ({s["t_pkg"]})')
report_lines.append('\n## Suggested Questions\n')
for q in questions:
    report_lines.append(f'- {q}')

Path('graphify-out/GRAPH_REPORT.md').write_text('\n'.join(report_lines), encoding='utf-8')
print('Built graph.json with', len(nodes), 'nodes and', len(edges), 'edges')
print('Report written to graphify-out/GRAPH_REPORT.md')
