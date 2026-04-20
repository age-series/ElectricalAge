#!/usr/bin/env python3
import argparse
import os
from glob import glob

CSS_BLOCK = '<style type="text/css"><![CDATA[image{image-rendering:pixelated;image-rendering:crisp-edges;}]]></style>'


def patch_svg(path: str) -> bool:
    with open(path, 'r', encoding='utf-8') as f:
        data = f.read()
    # Clean previous misplaced injections (outside <svg> root).
    data = data.replace(CSS_BLOCK + '\n\n', '')
    data = data.replace(CSS_BLOCK + '\n', '')
    data = data.replace(CSS_BLOCK, '')

    svg_idx = data.find('<svg')
    if svg_idx == -1:
        return False
    insert_at = data.find('>', svg_idx)
    if insert_at == -1:
        return False
    if 'image-rendering:pixelated' in data[svg_idx:]:
        return False

    # Insert style after opening <svg ...>
    patched = data[:insert_at + 1] + '\n' + CSS_BLOCK + '\n' + data[insert_at + 1:]
    with open(path, 'w', encoding='utf-8') as f:
        f.write(patched)
    return True


def main():
    ap = argparse.ArgumentParser(description='Inject pixelated image-rendering CSS into SVG files')
    ap.add_argument('svg_dir', help='Directory containing SVG files')
    args = ap.parse_args()

    files = sorted(glob(os.path.join(args.svg_dir, '*.svg')))
    changed = 0
    for p in files:
        if patch_svg(p):
            changed += 1

    print(f'files={len(files)}')
    print(f'patched={changed}')


if __name__ == '__main__':
    main()
