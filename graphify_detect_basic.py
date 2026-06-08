#!/usr/bin/env python3
import os, sys, json
from pathlib import Path
ROOT = Path('.').resolve()
EXT_CODE = {'.py','.java','.js','.ts','.go','.cpp','.c','.cs','.rb'}
EXT_DOCS = {'.md','.txt','.rst','.adoc'}
EXT_PAPERS = {'.pdf','.docx'}
EXT_IMAGES = {'.png','.jpg','.jpeg','.gif','.svg'}
EXT_VIDEO = {'.mp4','.mp3','.mov','.avi','.mkv'}

counts = {'total_files':0,'total_words':0,'code':[], 'document':[], 'paper':[], 'image':[], 'video':[]}
for dirpath, dirnames, filenames in os.walk('.'):
    # skip .git and graphify-out
    if dirpath.startswith('./.git') or 'graphify-out' in dirpath:
        continue
    for f in filenames:
        counts['total_files'] += 1
        p = Path(dirpath) / f
        ext = p.suffix.lower()
        if ext in EXT_CODE:
            counts['code'].append(str(p))
            try:
                text = p.read_text(encoding='utf-8')
                counts['total_words'] += len(text.split())
            except Exception:
                pass
        elif ext in EXT_DOCS:
            counts['document'].append(str(p))
            try:
                text = p.read_text(encoding='utf-8')
                counts['total_words'] += len(text.split())
            except Exception:
                pass
        elif ext in EXT_PAPERS:
            counts['paper'].append(str(p))
        elif ext in EXT_IMAGES:
            counts['image'].append(str(p))
        elif ext in EXT_VIDEO:
            counts['video'].append(str(p))
        else:
            # treat others as docs if small text files
            try:
                text = p.read_text(encoding='utf-8')
                if len(text.strip())>0 and len(text.split())<20000:
                    counts['document'].append(str(p))
                    counts['total_words'] += len(text.split())
            except Exception:
                pass

os.makedirs('graphify-out', exist_ok=True)
Path('graphify-out/.graphify_detect.json').write_text(json.dumps(counts, ensure_ascii=False), encoding='utf-8')
print(json.dumps({'summary': {
    'Corpus': f"{counts['total_files']} files · ~{counts['total_words']} words",
    'code': len(counts['code']),
    'docs': len(counts['document']),
    'papers': len(counts['paper']),
    'images': len(counts['image']),
    'video': len(counts['video'])
}}, ensure_ascii=False, indent=2))
