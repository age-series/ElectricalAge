#!/usr/bin/env python3
import argparse
import os
import re
import zipfile
from collections import Counter, defaultdict, deque
from glob import glob

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC_FILE = os.path.join(REPO_ROOT, "src/main/kotlin/mods/eln/craft/CraftingRecipes.kt")
TEXTURES_ITEMS = os.path.join(REPO_ROOT, "src/main/resources/assets/eln/textures/items")
TEXTURES_BLOCKS = os.path.join(REPO_ROOT, "src/main/resources/assets/eln/textures/blocks")
TEXTURES_MOD_CACHE = os.path.join(REPO_ROOT, "docs/recipes/mod_textures")
MC_TEX_CACHE_DIR = os.path.join(REPO_ROOT, "docs/recipes/mc_textures")

PROCESS_TEX = {
    "crafting": "minecraft:blocks_crafting_table",
    "smelting": "src/main/resources/assets/eln/textures/blocks/electricalfurnace.png",
    "collection": "src/main/resources/assets/eln/textures/blocks/treeresincollector.png",
    "macerator": "src/main/resources/assets/eln/textures/blocks/50vmacerator.png",
    "arc_furnace": "src/main/resources/assets/eln/textures/blocks/800varcfurnace.png",
    "plate_machine": "src/main/resources/assets/eln/textures/blocks/50vplatemachine.png",
    "compressor": "src/main/resources/assets/eln/textures/blocks/50vcompressor.png",
    "magnetizer": "src/main/resources/assets/eln/textures/blocks/50vmagnetizer.png",
}

PROCESS_LABEL = {
    "crafting": "Crafting",
    "smelting": "Smelting",
    "collection": "Collection",
    "macerator": "Macerator",
    "arc_furnace": "Arc Furnace",
    "plate_machine": "Plate Machine",
    "compressor": "Compressor",
    "magnetizer": "Magnetizer",
}

MACHINE_LIST_TO_PROCESS = {
    "maceratorRecipes": "macerator",
    "arcFurnaceRecipes": "arc_furnace",
    "plateMachineRecipes": "plate_machine",
    "compressorRecipes": "compressor",
    "magnetiserRecipes": "magnetizer",
}

_MC_JAR_PATH = None
_MC_TEX_MAP = {}


def strip_comments(src: str) -> str:
    out = []
    i = 0
    n = len(src)
    in_str = False
    in_char = False
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        if in_str:
            out.append(c)
            if c == "\\":
                i += 2
                if i - 1 < n:
                    out.append(src[i - 1])
                continue
            if c == '"':
                in_str = False
            i += 1
            continue
        if in_char:
            out.append(c)
            if c == "\\":
                i += 2
                if i - 1 < n:
                    out.append(src[i - 1])
                continue
            if c == "'":
                in_char = False
            i += 1
            continue

        if c == '"':
            in_str = True
            out.append(c)
            i += 1
            continue
        if c == "'":
            in_char = True
            out.append(c)
            i += 1
            continue

        if c == "/" and nxt == "/":
            while i < n and src[i] != "\n":
                i += 1
            continue
        if c == "/" and nxt == "*":
            i += 2
            while i + 1 < n and not (src[i] == "*" and src[i + 1] == "/"):
                i += 1
            i += 2
            continue

        out.append(c)
        i += 1
    return "".join(out)


def find_matching_paren(src: str, open_idx: int) -> int:
    depth = 0
    in_str = False
    in_char = False
    i = open_idx
    while i < len(src):
        c = src[i]
        if in_str:
            if c == "\\":
                i += 2
                continue
            if c == '"':
                in_str = False
            i += 1
            continue
        if in_char:
            if c == "\\":
                i += 2
                continue
            if c == "'":
                in_char = False
            i += 1
            continue

        if c == '"':
            in_str = True
        elif c == "'":
            in_char = True
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def split_args(s: str):
    args = []
    cur = []
    depth = 0
    in_str = False
    in_char = False
    i = 0
    while i < len(s):
        c = s[i]
        if in_str:
            cur.append(c)
            if c == "\\":
                if i + 1 < len(s):
                    cur.append(s[i + 1])
                    i += 2
                    continue
            elif c == '"':
                in_str = False
            i += 1
            continue
        if in_char:
            cur.append(c)
            if c == "\\":
                if i + 1 < len(s):
                    cur.append(s[i + 1])
                    i += 2
                    continue
            elif c == "'":
                in_char = False
            i += 1
            continue

        if c == '"':
            in_str = True
            cur.append(c)
        elif c == "'":
            in_char = True
            cur.append(c)
        elif c == "(":
            depth += 1
            cur.append(c)
        elif c == ")":
            depth -= 1
            cur.append(c)
        elif c == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
        i += 1
    tail = "".join(cur).strip()
    if tail:
        args.append(tail)
    return args


def humanize_identifier(name: str) -> str:
    name = re.sub(r"(Descriptor|descriptor|Desc|desc)$", "", name)
    name = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", name)
    name = name.replace("_", " ")
    return name.strip().title()


def parse_string_literal(expr: str):
    m = re.search(r'"([^"]+)"', expr)
    return m.group(1) if m else None


def parse_concat_string(expr: str):
    parts = re.findall(r'"([^"]*)"', expr)
    if not parts:
        return None
    remainder = re.sub(r'"[^"]*"', "", expr)
    if re.fullmatch(r"[\s+()]*", remainder):
        return "".join(parts)
    return None


def parse_count_literal(expr: str):
    expr = expr.strip()
    m = re.fullmatch(r"([0-9]+)(?:[fFdD])?", expr)
    if m:
        return int(m.group(1))
    return None


def canonicalize_item_name(name: str):
    if not name:
        return name

    # Normalize display dust names to ore-dict-style keys used elsewhere
    # (e.g. "Iron Dust" -> "dustIron") so chains connect consistently.
    md = re.fullmatch(r"\s*([A-Za-z0-9 ]+?)\s+Dust\s*", name)
    if md and not name.lower().startswith("ore:"):
        mat = "".join(w.capitalize() for w in md.group(1).split())
        if mat:
            name = f"dust{mat}"

    # Canonicalize ore-dict dust keys to plain dust keys.
    if name.startswith("ore:dust") and len(name) > len("ore:dust"):
        name = "dust" + name[len("ore:dust"):]

    # Normalize selected ore-dict ingot keys to concrete item names so dependency
    # chains can connect to actual producer recipes.
    if name == "ore:ingotCopper":
        name = "Copper Ingot"
    if name == "ore:ingotSilicon":
        name = "Silicon Ingot"
    if name == "ore:ingotLead":
        name = "Lead Ingot"
    if name == "ore:ingotIron":
        name = "minecraft:items_iron_ingot"
    if name == "ore:ingotGold":
        name = "minecraft:items_gold_ingot"
    if name == "ore:ingotAlloy":
        name = "Alloy Ingot"
    if name == "ore:plateIron":
        name = "Iron Plate"
    if name == "ore:plateCopper":
        name = "Copper Plate"
    if name == "ore:plateSilicon":
        name = "Silicon Plate"
    if name == "ore:plateAlloy":
        name = "Alloy Plate"
    if name == "ore:plateCoal":
        name = "Coal Plate"
    if name == "ore:plateGold":
        name = "Gold Plate"
    if name == "ore:blockIron":
        name = "minecraft:blocks_iron_block"

    # Canonicalize rubber ore-dict names to concrete item to expose production chain.
    if name in {"ore:itemRubber", "itemRubber"}:
        name = "Rubber"

    # Canonicalize dict placeholders to concrete item names when possible.
    if name.startswith("Dict "):
        name = name[5:]

    # Fix source typo variants to keep dependency chains connected.
    if name in {"Meduim Voltage Cable", "meduim voltage cable"}:
        name = "Medium Voltage Cable"

    # Canonicalize dict tungsten dust placeholders to the produced dust item.
    if name in {"dustDictTungsten", "Dust Dict Tungsten", "Dict Tungsten Dust"}:
        name = "dustTungsten"

    # Resolve common Eln.instance block field outputs to user-facing names.
    if name == "Computer Probe Block":
        name = "Eln Computer Probe"
    if name == "Eln To Other Block Converter":
        name = "Energy Exporter"

    return name


def normalize_expr(expr: str):
    expr = expr.strip()
    if not expr:
        return None
    if re.fullmatch(r"'[^']'", expr):
        return None
    if re.fullmatch(r"[0-9.]+[fFdD]?", expr):
        return None

    m = re.search(r"(?:^|\b)(?:findItemStack|Eln\.findItemStack)\s*\((.*)\)$", expr, flags=re.S)
    if m:
        args = split_args(m.group(1))
        if args:
            s = parse_concat_string(args[0]) or parse_string_literal(args[0])
            if s:
                return s
            # Handle variable-based lookups inside loops, e.g. findItemStack(panel).
            ident = re.fullmatch(r"\s*`?([A-Za-z_][A-Za-z0-9_]*)`?\s*", args[0])
            if ident:
                return humanize_identifier(ident.group(1))
    if re.search(r"(?:^|\b)Eln\.findItemStack\s*\(", expr):
        return None

    m = re.search(r"(?:^|\b)firstExistingOre\s*\(([^)]*)\)", expr)
    if m:
        inner = split_args(m.group(1))
        for a in inner:
            s = parse_string_literal(a)
            if s:
                return f"ore:{s}"

    m = re.search(r"ItemStack\s*\(\s*(Items|Blocks)\.([A-Za-z0-9_]+)", expr)
    if m:
        # Preserve dye metadata so recipes reflect specific color requirements.
        if m.group(1) == "Items" and m.group(2) == "dye":
            md = re.search(
                r"ItemStack\s*\(\s*Items\.dye\s*,\s*[0-9]+(?:[fFdD])?\s*,\s*([0-9]+)\s*\)",
                expr,
            )
            if md:
                dye_map = {
                    0: "black",
                    1: "red",
                    2: "green",
                    3: "brown",
                    4: "blue",
                    5: "purple",
                    6: "cyan",
                    7: "light_gray",
                    8: "gray",
                    9: "pink",
                    10: "lime",
                    11: "yellow",
                    12: "light_blue",
                    13: "magenta",
                    14: "orange",
                    15: "white",
                }
                color = dye_map.get(int(md.group(1)))
                if color:
                    return f"minecraft:items_dye_{color}"
        return f"minecraft:{m.group(1).lower()}_{m.group(2)}"

    # ItemStack(Eln.instance.someBlockField)
    m = re.search(r"ItemStack\s*\(\s*Eln\.instance\.([A-Za-z0-9_]+)", expr)
    if m:
        return humanize_identifier(m.group(1))

    m = re.search(r"(Eln\.instance\.)?([A-Za-z0-9_]+)\s*\.newItemStack\s*\(", expr)
    if m:
        return humanize_identifier(m.group(2))

    s = parse_concat_string(expr) or parse_string_literal(expr)
    if s is not None:
        if len(s) <= 3 and re.fullmatch(r"[ A-Za-z]{1,3}", s):
            return None
        if s and s[0].islower():
            return f"ore:{s}"
        return s

    m = re.search(r"Eln\.([A-Za-z0-9_]+)", expr)
    if m:
        if m.group(1) in {"findItemStack"}:
            return None
        return humanize_identifier(m.group(1))

    if any(ch.isalpha() for ch in expr) and "(" not in expr:
        return humanize_identifier(expr)

    return None


def parse_item_and_count(expr: str, var_map=None):
    if var_map is None:
        var_map = {}

    # Resolve references like `in`.item / in.item / in.itemDamage to the source findItemStack item.
    vm = re.search(r"^`?([A-Za-z_][A-Za-z0-9_]*)`?\.(?:item|itemDamage)\b", expr.strip())
    if vm:
        mapped = var_map.get(vm.group(1))
        if mapped:
            return canonicalize_item_name(mapped), 1
    # Resolve simple variable references.
    v = re.fullmatch(r"\s*`?([A-Za-z_][A-Za-z0-9_]*)`?\s*", expr)
    if v:
        mapped = var_map.get(v.group(1))
        if mapped:
            return canonicalize_item_name(mapped), 1
    # Resolve findItemStack(variable) / Eln.findItemStack(variable).
    mv = re.search(r"(?:^|\b)(?:findItemStack|Eln\.findItemStack)\s*\(\s*`?([A-Za-z_][A-Za-z0-9_]*)`?\s*\)\s*$", expr.strip())
    if mv:
        mapped = var_map.get(mv.group(1))
        if mapped:
            return canonicalize_item_name(mapped), 1

    name = normalize_expr(expr)
    if not name:
        return None, 1

    name = canonicalize_item_name(name)

    # findItemStack("Name", N)
    m = re.search(r"(?:^|\b)(?:findItemStack|Eln\.findItemStack)\s*\((.*)\)$", expr.strip(), flags=re.S)
    if m:
        args = split_args(m.group(1))
        if len(args) >= 2:
            c = parse_count_literal(args[1])
            if c is not None:
                return name, c
        return name, 1

    # *.newItemStack(N)
    m = re.search(r"\.newItemStack\s*\((.*)\)$", expr.strip(), flags=re.S)
    if m:
        args = split_args(m.group(1))
        if args:
            c = parse_count_literal(args[0])
            if c is not None:
                return name, c
        return name, 1

    # ItemStack(item, N, ...)
    m = re.search(r"ItemStack\s*\((.*)\)$", expr.strip(), flags=re.S)
    if m:
        args = split_args(m.group(1))
        if len(args) >= 2:
            c = parse_count_literal(args[1])
            if c is not None:
                return name, c
        return name, 1

    return name, 1


def build_function_var_map(src: str, fn_start: int, fn_end: int):
    segment = src[fn_start:fn_end]
    out = {}
    for m in re.finditer(r"^\s*(?:val\s+)?(`?[A-Za-z_][A-Za-z0-9_]*`?)\s*=\s*(?:findItemStack|Eln\.findItemStack)\s*\((.*)\)\s*$", segment, flags=re.M):
        var_name = m.group(1).strip("`")
        args = split_args(m.group(2))
        if not args:
            continue
        s = parse_concat_string(args[0]) or parse_string_literal(args[0])
        if s:
            out[var_name] = s
    # Resolve loop variables sourced from arrayOf(...) to first option.
    for m in re.finditer(
        r"for\s*\(\s*`?([A-Za-z_][A-Za-z0-9_]*)`?\s+in\s+arrayOf(?:<[^>]+>)?\(([^)]*)\)\s*\)",
        segment,
        flags=re.M,
    ):
        var_name = m.group(1)
        args = split_args(m.group(2))
        if not args:
            continue
        s = parse_concat_string(args[0]) or parse_string_literal(args[0])
        if not s:
            continue
        if s and s[0].islower():
            s = f"ore:{s}"
        out[var_name] = s
    return out


def build_var_map_until(src: str, fn_start: int, call_pos: int):
    segment = src[fn_start:call_pos]
    out = {}
    for m in re.finditer(r"^\s*(?:val\s+)?(`?[A-Za-z_][A-Za-z0-9_]*`?)\s*=\s*(?:findItemStack|Eln\.findItemStack)\s*\((.*)\)\s*$", segment, flags=re.M):
        var_name = m.group(1).strip("`")
        args = split_args(m.group(2))
        if not args:
            continue
        s = parse_concat_string(args[0]) or parse_string_literal(args[0])
        if s:
            out[var_name] = s
    # Resolve loop variables sourced from arrayOf(...) to first option.
    for m in re.finditer(
        r"for\s*\(\s*`?([A-Za-z_][A-Za-z0-9_]*)`?\s+in\s+arrayOf(?:<[^>]+>)?\(([^)]*)\)\s*\)",
        segment,
        flags=re.M,
    ):
        var_name = m.group(1)
        args = split_args(m.group(2))
        if not args:
            continue
        s = parse_concat_string(args[0]) or parse_string_literal(args[0])
        if not s:
            continue
        if s and s[0].islower():
            s = f"ore:{s}"
        out[var_name] = s
    return out


def parse_function_ranges(src: str):
    ranges = []
    for m in re.finditer(r"^\s*(?:private\s+)?fun\s+([A-Za-z0-9_]+)\s*\(", src, flags=re.M):
        name = m.group(1)
        start = m.start()
        ranges.append((start, name))
    ranges.sort()
    out = []
    for i, (start, name) in enumerate(ranges):
        end = ranges[i + 1][0] if i + 1 < len(ranges) else len(src)
        out.append((start, end, name))
    return out


def function_for_pos(ranges, pos):
    lo, hi = 0, len(ranges) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        s, e, name = ranges[mid]
        if s <= pos < e:
            return name
        if pos < s:
            hi = mid - 1
        else:
            lo = mid + 1
    return "global"


def build_texture_index():
    idx = {}
    for root in (TEXTURES_ITEMS, TEXTURES_BLOCKS, TEXTURES_MOD_CACHE):
        if not os.path.isdir(root):
            continue
        for fn in os.listdir(root):
            if not fn.lower().endswith(".png"):
                continue
            key = re.sub(r"[^a-z0-9]", "", os.path.splitext(fn)[0].lower())
            rel = os.path.relpath(os.path.join(root, fn), REPO_ROOT)
            idx[key] = rel
    idx.setdefault("emptytexture", "src/main/resources/assets/eln/textures/items/empty-texture.png")
    return idx


def find_minecraft_client_jar():
    global _MC_JAR_PATH
    if _MC_JAR_PATH is not None:
        return _MC_JAR_PATH

    candidates = [
        os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"),
    ]
    candidates += sorted(
        glob(os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_*_client.jar")),
        reverse=True,
    )

    for c in candidates:
        if os.path.isfile(c):
            _MC_JAR_PATH = c
            return _MC_JAR_PATH
    _MC_JAR_PATH = ""
    return _MC_JAR_PATH


def minecraft_texture_from_jar(category: str, name: str):
    key = f"{category}:{name}"
    if key in _MC_TEX_MAP:
        return _MC_TEX_MAP[key]

    jar_path = find_minecraft_client_jar()
    if not jar_path:
        _MC_TEX_MAP[key] = None
        return None

    # 1.7 uses items/blocks; newer jars use item/block.
    if category == "items":
        entries = [
            f"assets/minecraft/textures/items/{name}.png",
            f"assets/minecraft/textures/item/{name}.png",
        ]
        if name.startswith("dye_") and len(name) > 4:
            color = name[4:]
            entries.extend(
                [
                    f"assets/minecraft/textures/items/{color}_dye.png",
                    f"assets/minecraft/textures/item/{color}_dye.png",
                ]
            )
    else:
        entries = [
            f"assets/minecraft/textures/blocks/{name}.png",
            f"assets/minecraft/textures/block/{name}.png",
        ]
        if name == "log":
            entries.extend(
                [
                    "assets/minecraft/textures/blocks/oak_log.png",
                    "assets/minecraft/textures/block/oak_log.png",
                ]
            )
        if name == "planks":
            entries.extend(
                [
                    "assets/minecraft/textures/blocks/oak_planks.png",
                    "assets/minecraft/textures/block/oak_planks.png",
                ]
            )
        if name == "glass_pane":
            entries.extend(
                [
                    "assets/minecraft/textures/blocks/glass_pane_top.png",
                    "assets/minecraft/textures/block/glass_pane_top.png",
                    "assets/minecraft/textures/blocks/glass.png",
                    "assets/minecraft/textures/block/glass.png",
                ]
            )
        if name == "crafting_table":
            entries.extend(
                [
                    "assets/minecraft/textures/blocks/crafting_table_top.png",
                    "assets/minecraft/textures/block/crafting_table_top.png",
                    "assets/minecraft/textures/blocks/crafting_table_front.png",
                    "assets/minecraft/textures/block/crafting_table_front.png",
                ]
            )
        if name == "daylight_detector":
            entries.extend(
                [
                    "assets/minecraft/textures/blocks/daylight_detector_top.png",
                    "assets/minecraft/textures/block/daylight_detector_top.png",
                    "assets/minecraft/textures/blocks/daylight_detector_side.png",
                    "assets/minecraft/textures/block/daylight_detector_side.png",
                ]
            )
        if name == "furnace":
            entries.extend(
                [
                    "assets/minecraft/textures/blocks/furnace_front.png",
                    "assets/minecraft/textures/block/furnace_front.png",
                    "assets/minecraft/textures/blocks/furnace_top.png",
                    "assets/minecraft/textures/block/furnace_top.png",
                    "assets/minecraft/textures/blocks/furnace_side.png",
                    "assets/minecraft/textures/block/furnace_side.png",
                ]
            )
        if name == "quartz_ore":
            entries.extend(
                [
                    "assets/minecraft/textures/blocks/nether_quartz_ore.png",
                    "assets/minecraft/textures/block/nether_quartz_ore.png",
                ]
            )
        if name == "sapling":
            entries.extend(
                [
                    "assets/minecraft/textures/blocks/oak_sapling.png",
                    "assets/minecraft/textures/block/oak_sapling.png",
                ]
            )

    os.makedirs(MC_TEX_CACHE_DIR, exist_ok=True)
    out_path = os.path.join(MC_TEX_CACHE_DIR, f"{category}_{name}.png")
    if os.path.isfile(out_path):
        rel = os.path.relpath(out_path, REPO_ROOT)
        _MC_TEX_MAP[key] = rel
        return rel

    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            names = set(zf.namelist())
            for ent in entries:
                if ent in names:
                    with zf.open(ent) as src, open(out_path, "wb") as dst:
                        dst.write(src.read())
                    rel = os.path.relpath(out_path, REPO_ROOT)
                    _MC_TEX_MAP[key] = rel
                    return rel
    except Exception:
        _MC_TEX_MAP[key] = None
        return None

    _MC_TEX_MAP[key] = None
    return None


def item_to_texture(item: str, tex_index):
    base = item.lower().strip()
    original = base
    base = base.replace("ore:", "")
    base = base.replace("minecraft:", "")
    base = base.replace("items_", "").replace("blocks_", "")
    key = re.sub(r"[^a-z0-9]", "", base)

    # Common naming aliases produced by source parsing.
    alias_candidates = []
    # "Dict Advanced Chip" -> advancedchip
    if key.startswith("dict"):
        alias_candidates.append(key[4:])
    # ore dictionary plates: ore:plateIron -> ironplate
    m_plate = re.match(r"plate([a-z0-9]+)", key)
    if m_plate:
        alias_candidates.append(f"{m_plate.group(1)}plate")
    # ore dictionary ingots: ore:ingotCopper -> copperingot
    m_ingot = re.match(r"ingot([a-z0-9]+)", key)
    if m_ingot:
        alias_candidates.append(f"{m_ingot.group(1)}ingot")
    # ore dictionary blocks: ore:blockSteel -> steelblock
    m_block = re.match(r"block([a-z0-9]+)", key)
    if m_block:
        alias_candidates.append(f"{m_block.group(1)}block")
    # ore dictionary dusts: dustIron / ore:dustIron -> irondust
    m_dust = re.match(r"dust([a-z0-9]+)", key)
    if m_dust:
        alias_candidates.append(f"{m_dust.group(1)}dust")
    # Tool/armor naming variants: "Axe Copper" vs "copper_axe".
    m_tool = re.match(r"(axe|pickaxe|shovel|hoe|sword|boots|helmet|chestplate|leggings)([a-z0-9]+)", key)
    if m_tool:
        alias_candidates.append(f"{m_tool.group(2)}{m_tool.group(1)}")
    # Generic parser placeholders from recipe loops.
    if key == "blocktype":
        mc_tex = minecraft_texture_from_jar("blocks", "stone")
        if mc_tex:
            return mc_tex
    if key == "ingottype":
        mc_tex = minecraft_texture_from_jar("items", "iron_ingot")
        if mc_tex:
            return mc_tex
    if key == "metal":
        if "blocksteel" in tex_index:
            return tex_index["blocksteel"]
    if key in {"materialstring", "string"}:
        mc_tex = minecraft_texture_from_jar("items", "string")
        if mc_tex:
            return mc_tex
    if key == "panel":
        if "smallsolarpanel" in tex_index:
            return tex_index["smallsolarpanel"]
    if key in {"plankwood", "plank"}:
        mc_tex = minecraft_texture_from_jar("blocks", "planks")
        if mc_tex:
            return mc_tex
    if key in {"logwood", "log"}:
        mc_tex = minecraft_texture_from_jar("blocks", "log")
        if mc_tex:
            return mc_tex
    if key in {"itemrubber", "rubberitem"} and "rubber" in tex_index:
        return tex_index["rubber"]
    if key == "casingmachineadvanced" and "advancedmachineblock" in tex_index:
        return tex_index["advancedmachineblock"]
    # Antenna naming in recipes vs texture files.
    if "receiverantenna" in key or "recieverantenna" in key:
        alias_candidates.extend(["electricalantennarx", "signalantenna"])
    if "transmitterantenna" in key:
        alias_candidates.extend(["electricalantennatx", "signalantenna"])
    # Incandescent bulb recipe/item naming variants.
    if (
        "carbonincandescentlightbulb" in key
        or "carbonincandesentlightbulb" in key
        or "incandescentlightbulb" in key
        or "incandesentlightbulb" in key
    ):
        alias_candidates.append("incandescentcarbonlamp")
    if "economiclightbulb" in key or "ledbulb" in key:
        alias_candidates.append("ledlamp")
    if "emergencylamp" in key:
        alias_candidates.append("emergencylamp")
    if "farminglamp" in key:
        alias_candidates.append("farminglamp")
    if "signaltrimmer" in key:
        alias_candidates.append("trimmer")
    if "batterycharger" in key:
        alias_candidates.append("batterycharger")
    if "directutilitypole" in key:
        alias_candidates.append("utilitypole")
    if "elncomputerprobe" in key or "computerprobeblock" in key:
        alias_candidates.extend(["computerprobeyp", "computerprobe"])
    if "energyexporter" in key or "elntootherblockconverter" in key:
        alias_candidates.extend(["elntoic2lvu", "energyconverter"])

    candidates = [key] + alias_candidates
    for cand in candidates:
        if cand in tex_index:
            return tex_index[cand]

    # Fallback for vanilla minecraft:* references via local Gradle Minecraft jar.
    mm = re.match(r"minecraft:(items|blocks)_([a-z0-9_]+)$", original)
    if mm:
        mc_tex = minecraft_texture_from_jar(mm.group(1), mm.group(2))
        if mc_tex:
            return mc_tex
    return tex_index["emptytexture"]


def resolve_texture(path_or_key: str, tex_index):
    if path_or_key.startswith("minecraft:"):
        mm = re.match(r"minecraft:(items|blocks)_([a-z0-9_]+)$", path_or_key.lower())
        if mm:
            mc_tex = minecraft_texture_from_jar(mm.group(1), mm.group(2))
            if mc_tex:
                return mc_tex
        return tex_index["emptytexture"]
    return path_or_key


def sanitize_id(prefix: str, s: str):
    return f"{prefix}_" + re.sub(r"[^a-zA-Z0-9_]", "_", s)


def escape_html(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def safe_filename(s: str) -> str:
    out = re.sub(r"[^a-zA-Z0-9._-]+", "_", s.strip())
    out = out.strip("._")
    return out or "unnamed"


def slugify(s: str) -> str:
    out = re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")
    return out or "x"


def image_src_path(path: str) -> str:
    if os.path.isabs(path):
        p = path
    else:
        p = os.path.abspath(os.path.join(REPO_ROOT, path))
    return p.replace("\\", "/")


def img_tag(path: str, size_px: int) -> str:
    src = image_src_path(path)
    return f'<TD FIXEDSIZE="TRUE" WIDTH="{size_px}" HEIGHT="{size_px}"><IMG SRC="{src}" SCALE="BOTH"/></TD>'


def item_img_block(path: str, size_px: int) -> str:
    # Nest the fixed-size image cell so long labels do not stretch the icon width.
    return (
        "<TD>"
        '<TABLE BORDER="0" CELLBORDER="0" CELLPADDING="0" CELLSPACING="0">'
        f"<TR>{img_tag(path, size_px)}</TR>"
        "</TABLE>"
        "</TD>"
    )


def placeholder_options(item: str):
    k = item.strip().lower()
    if k == "ingot type":
        return ["ore:ingotAluminum", "ore:ingotAluminium", "ore:ingotSteel"]
    if k == "block type":
        return ["ore:blockAluminum", "ore:blockAluminium", "ore:blockSteel"]
    if k == "metal":
        return ["ore:blockSteel", "ore:blockAluminum", "ore:blockAluminium", "casingMachineAdvanced"]
    if k == "ore name":
        return ["ore:ingotAluminum", "ore:ingotAluminium", "ore:ingotSteel", "ore:ingotIron"]
    return None


def option_label(s: str) -> str:
    t = s[4:] if s.startswith("ore:") else s
    return escape_html(t)


def placeholder_item_block(item: str, tex_index):
    opts = placeholder_options(item)
    if not opts:
        return None
    img_cells = []
    txt_cells = []
    for o in opts:
        tex = resolve_texture(item_to_texture(o, tex_index), tex_index)
        img_cells.append(img_tag(tex, 24))
        txt_cells.append(f'<TD><FONT POINT-SIZE="8">{option_label(o)}</FONT></TD>')
    return (
        '<TABLE BORDER="0" CELLBORDER="1" COLOR="#AAAAAA" CELLPADDING="2" CELLSPACING="0">'
        f"<TR>{''.join(img_cells)}</TR>"
        f"<TR>{''.join(txt_cells)}</TR>"
        "</TABLE>"
    )


def build_crafting_grid(pattern_rows, symbol_map):
    grid = []
    rows = list(pattern_rows[:3])
    while len(rows) < 3:
        rows.append("")
    for row in rows:
        rr = (row + "   ")[:3]
        for ch in rr:
            if ch == " ":
                grid.append(None)
            else:
                grid.append(symbol_map.get(ch))
    return grid


def crafting_grid_html(grid, tex_index):
    if not grid or len(grid) != 9:
        return ""
    cells = []
    cell_px = 16
    for cell in grid:
        if cell:
            tex = resolve_texture(item_to_texture(cell, tex_index), tex_index)
            cells.append(
                f'<TD FIXEDSIZE="TRUE" WIDTH="{cell_px}" HEIGHT="{cell_px}"><IMG SRC="{image_src_path(tex)}" SCALE="BOTH"/></TD>'
            )
        else:
            cells.append(f'<TD FIXEDSIZE="TRUE" WIDTH="{cell_px}" HEIGHT="{cell_px}"></TD>')
    rows = ['<TABLE BORDER="0" CELLBORDER="1" CELLPADDING="0" CELLSPACING="0">']
    for i in range(0, 9, 3):
        rows.append("<TR>" + "".join(cells[i : i + 3]) + "</TR>")
    rows.append("</TABLE>")
    return "".join(rows)


def parse_recipes(src: str):
    src = strip_comments(src)
    ranges = parse_function_ranges(src)
    recipes = []
    fn_starts = {name: s for s, e, name in ranges}

    def is_function_declaration(call_pos: int) -> bool:
        line_start = src.rfind("\n", 0, call_pos) + 1
        prefix = src[line_start:call_pos]
        return bool(re.search(r"\bfun\s+$", prefix))

    # addRecipe / addShapelessRecipe / addSmelting
    call_patterns = ["addRecipe(", "addShapelessRecipe(", "addSmelting("]
    for pat in call_patterns:
        i = 0
        while True:
            i = src.find(pat, i)
            if i == -1:
                break
            if is_function_declaration(i):
                i += len(pat)
                continue
            open_idx = i + len(pat) - 1
            close_idx = find_matching_paren(src, open_idx)
            if close_idx == -1:
                i += len(pat)
                continue
            args = split_args(src[open_idx + 1 : close_idx])
            fn = function_for_pos(ranges, i)
            fn_start = fn_starts.get(fn, 0)
            var_map = build_var_map_until(src, fn_start, i)
            if pat == "addRecipe(":
                if not args:
                    i = close_idx + 1
                    continue
                output, output_count = parse_item_and_count(args[0], var_map)

                # Parse shaped pattern and symbol map when possible.
                pattern_rows = []
                pidx = 1
                while pidx < len(args):
                    s = parse_string_literal(args[pidx])
                    if s is None or len(s) > 3:
                        break
                    pattern_rows.append(s)
                    pidx += 1

                input_counts = defaultdict(int)
                symbol_map = {}
                consumed = False
                if pattern_rows and pidx < len(args):
                    j = pidx
                    while j + 1 < len(args):
                        key = args[j].strip()
                        if not re.fullmatch(r"'[^']'", key):
                            break
                        symbol = key[1:-1]
                        in_name, in_count = parse_item_and_count(args[j + 1], var_map)
                        if in_name and symbol:
                            symbol_map[symbol] = in_name
                            occurrences = sum(row.count(symbol) for row in pattern_rows)
                            if occurrences > 0:
                                input_counts[in_name] += occurrences * max(1, in_count)
                                consumed = True
                        j += 2

                # Fallback if we couldn't decode shaped rows cleanly.
                if not consumed:
                    for idx, a in enumerate(args[1:], start=1):
                        if idx > 1 and re.fullmatch(r"'[^']'", args[idx - 1]):
                            in_name, in_count = parse_item_and_count(a, var_map)
                            if in_name:
                                input_counts[in_name] += max(1, in_count)
                    if not input_counts:
                        for a in args[1:]:
                            in_name, in_count = parse_item_and_count(a, var_map)
                            if in_name:
                                input_counts[in_name] += max(1, in_count)

                inputs = sorted(input_counts.keys())
                if output and inputs:
                    grid = build_crafting_grid(pattern_rows, symbol_map) if pattern_rows and symbol_map else None
                    recipes.append(
                        {
                            "kind": "crafting",
                            "section": fn,
                            "inputs": inputs,
                            "input_counts": dict(input_counts),
                            "output": output,
                            "output_count": max(1, output_count),
                            "grid": grid,
                        }
                    )
            elif pat == "addShapelessRecipe(":
                if not args:
                    i = close_idx + 1
                    continue
                output, output_count = parse_item_and_count(args[0], var_map)
                input_counts = defaultdict(int)
                for a in args[1:]:
                    in_name, in_count = parse_item_and_count(a, var_map)
                    if in_name:
                        input_counts[in_name] += max(1, in_count)
                inputs = sorted(input_counts.keys())
                if output and inputs:
                    recipes.append(
                        {
                            "kind": "crafting",
                            "section": fn,
                            "inputs": inputs,
                            "input_counts": dict(input_counts),
                            "output": output,
                            "output_count": max(1, output_count),
                            "grid": None,
                        }
                    )
            elif pat == "addSmelting(":
                if len(args) >= 3:
                    inp, inp_count = parse_item_and_count(args[0], var_map)
                    out, out_count = parse_item_and_count(args[2], var_map)
                    if inp and out:
                        recipes.append(
                            {
                                "kind": "smelting",
                                "section": fn,
                                "inputs": [inp],
                                "input_counts": {inp: max(1, inp_count)},
                                "output": out,
                                "output_count": max(1, out_count),
                                "grid": None,
                            }
                        )
            i = close_idx + 1

    # Eln.instance.<machine>Recipes.addRecipe(...)
    for m in re.finditer(r"Eln\.instance\.([A-Za-z0-9_]+)\.addRecipe\s*\(", src):
        list_name = m.group(1)
        process = MACHINE_LIST_TO_PROCESS.get(list_name)
        if not process:
            continue
        open_idx = src.find("(", m.end() - 1)
        close_idx = find_matching_paren(src, open_idx)
        if close_idx == -1:
            continue
        outer_args = split_args(src[open_idx + 1 : close_idx])
        if not outer_args:
            continue
        fn = function_for_pos(ranges, m.start())
        fn_start = fn_starts.get(fn, 0)
        var_map = build_var_map_until(src, fn_start, m.start())

        for arg in outer_args:
            rm = re.search(r"Recipe\s*\((.*)\)", arg, flags=re.S)
            if not rm:
                continue
            rargs = split_args(rm.group(1))
            if len(rargs) < 2:
                continue
            inp, inp_count = parse_item_and_count(rargs[0], var_map)
            out, out_count = parse_item_and_count(rargs[1], var_map)
            if inp and out:
                recipes.append(
                    {
                        "kind": process,
                        "section": fn,
                        "inputs": [inp],
                        "input_counts": {inp: max(1, inp_count)},
                        "output": out,
                        "output_count": max(1, out_count),
                        "grid": None,
                    }
                )

    # Gameplay source process (not crafting table/machine recipe registration):
    # Tree Resin Collector passively generates Tree Resin over time.
    recipes.append(
        {
            "kind": "collection",
            "section": "treeResinCollector",
            "inputs": [],
            "input_counts": {},
            "output": "Tree Resin",
            "output_count": 1,
            "grid": None,
        }
    )

    return recipes


def choose_preferred_recipe(recipes_for_item):
    def key_fn(r):
        is_crafting = 0 if r["kind"] == "crafting" else 1
        return (is_crafting, len(r["inputs"]), r["section"], r["kind"])

    return sorted(recipes_for_item, key=key_fn)[0]


def build_per_item_subgraph(
    recipes,
    target_item,
    include_alternatives=False,
    max_depth=30,
    forced_root_recipe_id=None,
):
    by_output = defaultdict(list)
    for idx, r in enumerate(recipes):
        by_output[r["output"]].append((idx, r))

    recipe_ids = set()
    item_seen = set()

    def parse_qty_name(item_name: str):
        s = item_name.strip()
        m = re.match(r"^\s*(\d+)x\s+(.+?)\s*$", s, flags=re.I)
        if m:
            qty = int(m.group(1))
            base = m.group(2).strip().lower()
        else:
            qty = 1
            base = s.lower()
        base = re.sub(r"[^a-z0-9]+", "", base)
        if base.endswith("s") and len(base) > 3:
            base = base[:-1]
        return qty, base

    def is_unpacking_recipe(recipe):
        out_qty, out_base = parse_qty_name(recipe["output"])
        for inp in recipe["inputs"]:
            in_qty, in_base = parse_qty_name(inp)
            if in_base and in_base == out_base and in_qty > out_qty:
                return True
        return False

    kind_pref = {
        "collection": 0,
        "smelting": 0,
        "crafting": 1,
        "macerator": 2,
        "compressor": 3,
        "plate_machine": 4,
        "arc_furnace": 5,
        "magnetizer": 6,
    }
    cost_memo = {}
    reach_memo = {}

    def can_reach_item(start_item, target_item, visiting):
        key = (start_item, target_item)
        if key in reach_memo:
            return reach_memo[key]
        if start_item == target_item:
            reach_memo[key] = True
            return True
        if start_item in visiting:
            reach_memo[key] = False
            return False
        visiting.add(start_item)
        for _, r in by_output.get(start_item, []):
            for nxt in r["inputs"]:
                if can_reach_item(nxt, target_item, visiting):
                    visiting.discard(start_item)
                    reach_memo[key] = True
                    return True
        visiting.discard(start_item)
        reach_memo[key] = False
        return False

    def recipe_is_backedge(recipe):
        out = recipe["output"]
        return any(can_reach_item(inp, out, set()) for inp in recipe["inputs"])

    def min_item_cost(item, stack_items):
        key = (item, tuple(sorted(stack_items)))
        if key in cost_memo:
            return cost_memo[key]
        if item in stack_items:
            cost_memo[key] = 0
            return 0
        producers = by_output.get(item, [])
        if not producers:
            cost_memo[key] = 0
            return 0
        best = None
        for _, recipe in producers:
            if is_unpacking_recipe(recipe):
                continue
            if any(inp in stack_items for inp in recipe["inputs"]):
                continue
            if recipe_is_backedge(recipe):
                continue
            next_stack = set(stack_items)
            next_stack.add(item)
            dep_cost = sum(min_item_cost(inp, next_stack) for inp in recipe["inputs"])
            total = 1 + dep_cost
            score = (
                total,
                kind_pref.get(recipe["kind"], 99),
                len(recipe["inputs"]),
                recipe["section"],
                recipe["kind"],
            )
            if best is None or score < best:
                best = score
        if best is None:
            cost_memo[key] = 0
            return 0
        cost_memo[key] = best[0]
        return best[0]

    def choose_preferred_non_cyclic(producers, stack_items):
        # producers: list[(rid, recipe)]
        ordered = sorted(producers, key=lambda t: (1 if is_unpacking_recipe(t[1]) else 0,))
        filtered = [t for t in ordered if not is_unpacking_recipe(t[1])]
        if filtered:
            ordered = filtered
        scored = []
        for rid, recipe in ordered:
            if any(inp in stack_items for inp in recipe["inputs"]):
                continue
            if recipe_is_backedge(recipe):
                continue
            next_stack = set(stack_items)
            dep_cost = sum(min_item_cost(inp, next_stack | {recipe["output"]}) for inp in recipe["inputs"])
            total = 1 + dep_cost
            scored.append(
                (
                    (
                        kind_pref.get(recipe["kind"], 99),
                        total,
                        len(recipe["inputs"]),
                        recipe["section"],
                        recipe["kind"],
                    ),
                    rid,
                    recipe,
                )
            )
        if scored:
            scored.sort(key=lambda t: t[0])
            return scored[0][1], scored[0][2]
        return ordered[0]

    def visit_item(item, depth, stack_items):
        if depth > max_depth or item in item_seen:
            return
        item_seen.add(item)
        producers = by_output.get(item, [])
        if not producers:
            return

        if forced_root_recipe_id is not None and item == target_item:
            chosen = [t for t in producers if t[0] == forced_root_recipe_id]
            if not chosen:
                chosen = [choose_preferred_non_cyclic(producers, stack_items)]
        elif include_alternatives:
            chosen = producers
        else:
            chosen = [choose_preferred_non_cyclic(producers, stack_items)]

        for rid, recipe in chosen:
            if rid in recipe_ids:
                continue
            recipe_ids.add(rid)
            next_stack = set(stack_items)
            next_stack.add(item)
            for inp in recipe["inputs"]:
                visit_item(inp, depth + 1, next_stack)

    visit_item(target_item, 0, set())
    return [recipes[rid] for rid in sorted(recipe_ids)]


def finalized_items(recipes):
    outputs = set(r["output"] for r in recipes)
    used_as_inputs = set()
    for r in recipes:
        used_as_inputs.update(r["inputs"])
    return sorted(outputs - used_as_inputs)


def complexity_metrics_for_item(recipes, target_item, include_alternatives=False):
    sub = build_per_item_subgraph(
        recipes,
        target_item,
        include_alternatives=include_alternatives,
    )
    if not sub:
        return None

    # Build producer map inside this item's subgraph
    by_output = defaultdict(list)
    for r in sub:
        by_output[r["output"]].append(r)

    produced_items = set(by_output.keys())
    used_inputs = set()
    unique_items = set()
    machine_steps = 0
    for r in sub:
        unique_items.add(r["output"])
        unique_items.update(r["inputs"])
        used_inputs.update(r["inputs"])
        if r["kind"] != "crafting":
            machine_steps += 1

    base_inputs = sorted(used_inputs - produced_items)

    # Longest dependency chain, following preferred recipe path per intermediate.
    depth_cache = {}

    visiting = set()

    def item_depth(item):
        if item in depth_cache:
            return depth_cache[item]
        if item in visiting:
            # Break dependency cycles gracefully.
            return 0
        visiting.add(item)
        producers = by_output.get(item, [])
        if not producers:
            visiting.discard(item)
            depth_cache[item] = 0
            return 0
        recipe = choose_preferred_recipe(producers)
        d = 1 + (max((item_depth(inp) for inp in recipe["inputs"]), default=0))
        visiting.discard(item)
        depth_cache[item] = d
        return d

    depth = item_depth(target_item)
    steps = len(sub)
    unique_item_count = len(unique_items)
    base_input_count = len(base_inputs)

    # Weighted score tuned for relative ranking between items.
    score = (
        steps * 10
        + depth * 8
        + unique_item_count * 3
        + base_input_count * 2
        + machine_steps * 6
    )

    return {
        "item": target_item,
        "score": score,
        "steps": steps,
        "depth": depth,
        "unique_items": unique_item_count,
        "base_inputs": base_input_count,
        "machine_steps": machine_steps,
        "base_inputs_list": base_inputs,
    }


def write_complexity_report(recipes, out_path, include_alternatives=False, item_filter=None):
    finals = finalized_items(recipes)
    selected = finals
    if item_filter:
        selected = [x for x in finals if x == item_filter]

    rows = []
    for item in selected:
        m = complexity_metrics_for_item(
            recipes,
            item,
            include_alternatives=include_alternatives,
        )
        if m:
            rows.append(m)

    rows.sort(key=lambda r: (r["score"], r["steps"], r["depth"], r["item"]))
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as fh:
        fh.write("rank\tscore\tsteps\tdepth\tunique_items\tbase_inputs\tmachine_steps\titem\n")
        for idx, r in enumerate(rows, start=1):
            fh.write(
                f"{idx}\t{r['score']}\t{r['steps']}\t{r['depth']}\t{r['unique_items']}\t{r['base_inputs']}\t{r['machine_steps']}\t{r['item']}\n"
            )
    return rows


def compute_levels(recipes):
    items = set()
    adj = defaultdict(set)
    indeg = defaultdict(int)

    for r in recipes:
        out = r["output"]
        items.add(out)
        for inp in r["inputs"]:
            items.add(inp)
            if out not in adj[inp]:
                adj[inp].add(out)
                indeg[out] += 1

    for it in items:
        indeg.setdefault(it, 0)

    q = deque([it for it in items if indeg[it] == 0])
    level = {it: 0 for it in q}
    seen = set(q)

    while q:
        cur = q.popleft()
        for nxt in adj[cur]:
            level[nxt] = max(level.get(nxt, 0), level[cur] + 1)
            indeg[nxt] -= 1
            if indeg[nxt] == 0:
                q.append(nxt)
                seen.add(nxt)

    for it in items:
        if it not in level:
            level[it] = 0

    return level


def compute_layered_ranks(recipes, process_nodes):
    # Start with item-only topological depth.
    item_level = dict(compute_levels(recipes))
    proc_level = {pid: 1 for pid, _ in process_nodes}

    # Relax a few rounds to place process nodes between item layers.
    # Bounded rounds avoid runaway escalation if the recipe graph has cycles.
    for _ in range(8):
        changed = False
        for pid, r in process_nodes:
            in_lvl = max((item_level.get(inp, 0) for inp in r["inputs"]), default=0)
            want_proc = in_lvl + 1
            if proc_level.get(pid, 1) != want_proc:
                proc_level[pid] = want_proc
                changed = True

            out = r["output"]
            want_out = max(item_level.get(out, 0), want_proc + 1)
            if item_level.get(out, 0) != want_out:
                item_level[out] = want_out
                changed = True
        if not changed:
            break

    return item_level, proc_level


def write_dot(recipes, output_path, title="Electrical Age Crafting + Machine Recipes"):
    tex_index = build_texture_index()

    items = set()
    for r in recipes:
        items.add(r["output"])
        items.update(r["inputs"])

    process_nodes = []
    for i, r in enumerate(recipes):
        process_nodes.append((f"proc_{i}", r))

    item_level, proc_level = compute_layered_ranks(recipes, process_nodes)

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("digraph ElectricalAgeRecipes {\n")
        f.write(
            f"  graph [rankdir=TB, splines=true, overlap=false, newrank=true, ranksep=0.15, nodesep=0.1, labelloc=t, label=\"{escape_html(title)}\"];\n"
        )
        f.write("  node [shape=plain];\n")
        f.write("  edge [color=\"#555555\", arrowsize=0.7];\n\n")

        # Item nodes
        for item in sorted(items):
            nid = sanitize_id("item", item)
            label = escape_html(item)
            ph = placeholder_item_block(item, tex_index)
            if ph:
                f.write(
                    f'  {nid} [label=<<TABLE BORDER="0" CELLBORDER="1" COLOR="#AAAAAA" CELLPADDING="3"><TR><TD><B>{label}</B></TD></TR><TR><TD>{ph}</TD></TR></TABLE>>];\n'
                )
            else:
                tex = item_to_texture(item, tex_index)
                f.write(
                    f'  {nid} [label=<<TABLE BORDER="0" CELLBORDER="0" CELLPADDING="1"><TR>{item_img_block(tex, 40)}</TR><TR><TD><FONT POINT-SIZE="10">{label}</FONT></TD></TR></TABLE>>];\n'
                )

        f.write("\n")

        # Process nodes
        for pid, r in process_nodes:
            kind = r["kind"]
            ptex = resolve_texture(PROCESS_TEX.get(kind, PROCESS_TEX["crafting"]), tex_index)
            plabel = escape_html(PROCESS_LABEL.get(kind, kind))
            section = escape_html(r["section"])
            if kind == "crafting" and r.get("grid"):
                grid_html = crafting_grid_html(r.get("grid"), tex_index)
                f.write(
                    f'  {pid} [label=<<TABLE BORDER="0" CELLBORDER="1" COLOR="#AAAAAA" CELLPADDING="3">'
                    f'<TR>{img_tag(ptex, 32)}<TD><B>{plabel}</B><BR/><FONT POINT-SIZE="9">{section}</FONT></TD></TR>'
                    f'<TR><TD COLSPAN="2">{grid_html}</TD></TR>'
                    f'</TABLE>>];\n'
                )
            else:
                f.write(
                    f'  {pid} [label=<<TABLE BORDER="0" CELLBORDER="1" COLOR="#AAAAAA" CELLPADDING="3"><TR>{img_tag(ptex, 32)}<TD><B>{plabel}</B><BR/><FONT POINT-SIZE="9">{section}</FONT></TD></TR></TABLE>>];\n'
                )

        f.write("\n")

        # Edges
        for pid, r in process_nodes:
            out_id = sanitize_id("item", r["output"])
            for inp in r["inputs"]:
                in_id = sanitize_id("item", inp)
                cnt = r.get("input_counts", {}).get(inp, 1)
                f.write(f"  {in_id} -> {pid} [label=\"x{cnt}\", fontsize=9];\n")
            out_cnt = r.get("output_count", 1)
            f.write(f"  {pid} -> {out_id} [color=\"#2F7D32\", label=\"x{out_cnt}\", fontsize=9];\n")

        f.write("\n")

        # Rank levels for vertical progression
        levels = defaultdict(list)
        for item, lvl in item_level.items():
            levels[lvl].append(item)
        for pid, lvl in proc_level.items():
            levels[lvl].append(pid)

        for lvl in sorted(levels):
            f.write("  { rank=same; ")
            ids = []
            for n in sorted(levels[lvl]):
                if n.startswith("proc_"):
                    ids.append(n)
                else:
                    ids.append(sanitize_id("item", n))
            f.write(" ".join(ids))
            f.write(" }\n")

        f.write("}\n")


def alternative_suffixes_for_item(recipes, item):
    by_output = defaultdict(list)
    for idx, r in enumerate(recipes):
        by_output[r["output"]].append((idx, r))
    producers = by_output.get(item, [])
    if len(producers) <= 1:
        return []

    base = []
    for rid, r in producers:
        kind_label = PROCESS_LABEL.get(r["kind"], r["kind"])
        kind_slug = slugify(kind_label)
        first_input = ""
        if r.get("inputs"):
            first_input = slugify(r["inputs"][0])
        base.append((rid, kind_slug, slugify(r.get("section", "")), first_input))

    # If kinds are unique, suffix is just process kind (e.g. smelting).
    kind_counts = Counter(k for _, k, _, _ in base)
    out = []
    used = Counter()
    for rid, kslug, sslug, islug in base:
        if kind_counts[kslug] == 1:
            suffix = kslug
        else:
            # If same process appears multiple times, include distinguishing step/input.
            suffix = f"{kslug}-{islug}" if islug else (f"{kslug}-{sslug}" if sslug else kslug)
        used[suffix] += 1
        if used[suffix] > 1:
            suffix = f"{suffix}-{used[suffix]}"
        out.append((rid, suffix))
    return out


def main():
    parser = argparse.ArgumentParser(description="Generate Electrical Age recipe graph DOT from source code")
    parser.add_argument(
        "--src",
        default=SRC_FILE,
        help="Path to CraftingRecipes.kt",
    )
    parser.add_argument(
        "--out",
        default=os.path.join(REPO_ROOT, "docs/recipes/electricalage_recipes.dot"),
        help="Output .dot file path",
    )
    parser.add_argument(
        "--per-final-item-dir",
        default=None,
        help="If set, write one .dot file per finalized output item into this directory",
    )
    parser.add_argument(
        "--include-alternatives",
        action="store_true",
        help="Include all alternative producer recipes per item in per-item charts",
    )
    parser.add_argument(
        "--item",
        default=None,
        help="Limit per-item output to a single final item name",
    )
    parser.add_argument(
        "--complexity-out",
        default=os.path.join(REPO_ROOT, "docs/recipes/final_item_complexity.tsv"),
        help="Write an easiest->hardest complexity ranking report (TSV) for finalized items",
    )
    parser.add_argument(
        "--emit-alternative-variants",
        action="store_true",
        help="Emit separate per-item .dot variants for items with alternative producers, suffixed by process (e.g. -smelting, -arc-furnace)",
    )
    args = parser.parse_args()

    with open(args.src, "r", encoding="utf-8") as fh:
        src = fh.read()

    recipes = parse_recipes(src)
    recipes = [r for r in recipes if r["output"] and (r["inputs"] or r["kind"] == "collection")]
    print(f"recipes={len(recipes)}")

    if args.per_final_item_dir:
        out_dir = args.per_final_item_dir
        os.makedirs(out_dir, exist_ok=True)
        finals = finalized_items(recipes)
        by_output_all = defaultdict(list)
        for rid, rr in enumerate(recipes):
            by_output_all[rr["output"]].append((rid, rr))
        written = 0
        written_files = set()
        index_path = os.path.join(out_dir, "index.txt")
        with open(index_path, "w", encoding="utf-8") as idxf:
            selected = finals
            if args.item:
                if args.item in finals:
                    selected = [args.item]
                else:
                    # Allow explicit non-final item targets.
                    selected = [args.item] if args.item in by_output_all else []
            for item in selected:
                sub = build_per_item_subgraph(
                    recipes,
                    item,
                    include_alternatives=args.include_alternatives,
                )
                if not sub:
                    continue
                fn = safe_filename(item) + ".dot"
                out_path = os.path.join(out_dir, fn)
                if fn not in written_files:
                    write_dot(sub, out_path, title=f"Electrical Age Recipe Tree: {item}")
                    idxf.write(f"{item}\t{fn}\n")
                    written += 1
                    written_files.add(fn)

                if args.emit_alternative_variants:
                    for rid, suffix in alternative_suffixes_for_item(recipes, item):
                        sub_alt = build_per_item_subgraph(
                            recipes,
                            item,
                            include_alternatives=False,
                            forced_root_recipe_id=rid,
                        )
                        if not sub_alt:
                            continue
                        alt_name = f"{item} -{suffix}"
                        alt_fn = safe_filename(f"{item}-{suffix}") + ".dot"
                        alt_path = os.path.join(out_dir, alt_fn)
                        if alt_fn not in written_files:
                            write_dot(sub_alt, alt_path, title=f"Electrical Age Recipe Tree: {alt_name}")
                            idxf.write(f"{alt_name}\t{alt_fn}\n")
                            written += 1
                            written_files.add(alt_fn)

            # Also emit alternative-item charts for non-final outputs with multiple producers.
            if args.emit_alternative_variants and not args.item:
                alt_items = sorted([it for it, prods in by_output_all.items() if len(prods) > 1])
                for item in alt_items:
                    # Base chart for this alternative item.
                    sub = build_per_item_subgraph(
                        recipes,
                        item,
                        include_alternatives=False,
                    )
                    if sub:
                        fn = safe_filename(item) + ".dot"
                        out_path = os.path.join(out_dir, fn)
                        if fn not in written_files:
                            write_dot(sub, out_path, title=f"Electrical Age Recipe Tree: {item}")
                            idxf.write(f"{item}\t{fn}\n")
                            written += 1
                            written_files.add(fn)

                    # One chart per alternative root producer.
                    for rid, suffix in alternative_suffixes_for_item(recipes, item):
                        sub_alt = build_per_item_subgraph(
                            recipes,
                            item,
                            include_alternatives=False,
                            forced_root_recipe_id=rid,
                        )
                        if not sub_alt:
                            continue
                        alt_name = f"{item} -{suffix}"
                        alt_fn = safe_filename(f"{item}-{suffix}") + ".dot"
                        alt_path = os.path.join(out_dir, alt_fn)
                        if alt_fn in written_files:
                            continue
                        write_dot(sub_alt, alt_path, title=f"Electrical Age Recipe Tree: {alt_name}")
                        idxf.write(f"{alt_name}\t{alt_fn}\n")
                        written += 1
                        written_files.add(alt_fn)
        print(f"final_items={len(finals)}")
        if args.item:
            print(f"selected_items={len(selected)}")
        print(f"wrote={written}")
        print(f"index={index_path}")
    else:
        write_dot(recipes, args.out)
        print(f"wrote={args.out}")

    complexity_rows = write_complexity_report(
        recipes,
        args.complexity_out,
        include_alternatives=args.include_alternatives,
        item_filter=args.item,
    )
    print(f"complexity_rows={len(complexity_rows)}")
    print(f"complexity_out={args.complexity_out}")


if __name__ == "__main__":
    main()
