#!/usr/bin/env python3
"""Vendor cited MPC UI screenshots and render relevant PDF pages for offline use."""
from __future__ import annotations
import argparse, hashlib, html, json, os, re, sys, time, tempfile
from pathlib import Path
from urllib.parse import parse_qs, urldefrag, urlparse
import fitz
import requests
from bs4 import BeautifulSoup
from PIL import Image

DOCS = [
    "docs/MPC3-UI-VISUAL-REFERENCE-CATALOG.md",
    "docs/MPC3-9-OS-KNOWLEDGE-BASE.md",
    "docs/MPC3-REFERENCE-INDEX.md",
    "docs/MPC3-UI-SCREEN-BLUEPRINT.md",
    "docs/MPC3-UI-MIGRATION-MASTER-SPEC.md",
    "docs/MPC3-USER-ACTION-CATALOG.md",
    "docs/MPC-UX-REFERENCE.md",
    "docs/MPC-STYLE-UI-ROADMAP.md",
    "docs/MPC3-FIRMWARE-UI-RESEARCH.md",
]
CATALOG = DOCS[0]
ASSET_REL = Path("docs/MPC3-UI-REFERENCE-ASSETS")
IMG_RE = re.compile(r'!\[([^\]]*)\]\((https?://[^)\s]+)\)')
PDF_LINK_RE = re.compile(r'https?://[^\s<>"\)]+?\.pdf(?:\?[^#\s<>"\)]+)?(?:#page=\d+)?', re.I)
PAGE_RE = re.compile(r'#page=(\d+)', re.I)
MD_LINK_RE = re.compile(r'\[[^\]]*\]\((https?://[^)\s]+)\)')
RAW_URL_RE = re.compile(r'https?://[^\s<>"`]+', re.I)
SOURCE_PAGE_LIMIT = 180
SOURCE_PAGE_IMAGE_LIMIT = 500
PDF_SOURCES = {
    "mpc-v39": [
        "https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf",
        "https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf",
    ],
    "mpc-v37": ["https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf"],
    "mpc-v36": ["https://cdn.inmusicbrands.com/Software/36/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.6.pdf"],
    "mpc-v213-legacy": [
        "https://cdn.inmusicbrands.com/akai/attachments/MPC%20X%2C%20X%20SE%2C%20Live%2C%20Live%20II%2C%20One%2C%20One%2B%2C%20Key%2061%2C%20Key%2037%2C%20Studio%20mk2%2C%20Touch%20-%20User%20Guide%20-%20v2.13.pdf",
        "https://manuals.plus/m/98138a9e32b84f6146aee38f6d2efd1ad8996d06f7ec4c5bf91ecb369b27a3c4.pdf",
    ],
    "mpc-v2112-legacy": [
        "https://cdn.inmusicbrands.com/akai/MPC/2121_kdhebxog/MPC%20X%2C%20X%20SE%2C%20Live%2C%20Live%20II%2C%20One%2C%20One%2B%2C%20Key%2061%2C%20Studio%20mk2%2C%20Touch%20-%20User%20Guide%20-%20v2.11.12.pdf",
    ],
    "mpc-v2117-legacy": ["https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf"],
}
# Printed pages whose figures were identified in the current v3.9 atlas.
V39_ATLAS_PAGES = {
    15,56,57,59,60,61,62,63,64,65,66,67,68,69,75,77,78,80,81,84,87,100,109,
    112,117,126,129,133,136,139,140,141,142,143,144,145,146,147,148,149,150,
    151,152,153,154,167,179,184,190,191,197,203,212,215,221,230,254,255,256,
    257,260,261,264,266,269,296,297,298,304,305,306,314,316,318,320,328,333,
    334,335,336,337,338,340,
}
# Current OS 3.9 manual pages mapped to the 82 named contexts. Pages are
# rendered in full; large embedded figures are separately cropped when available.
V39_CONTEXT_PAGES = {}
def assign_pages(ids, pages):
    for vis_id in ids.split():
        V39_CONTEXT_PAGES[vis_id] = list(pages)

assign_pages("VIS-001", [15, 109])
assign_pages("VIS-002", [57])
assign_pages("VIS-003", [56, 59])
assign_pages("VIS-004", [15])
assign_pages("VIS-005 VIS-006", [133])
assign_pages("VIS-007", [56])
assign_pages("VIS-008 VIS-009 VIS-010 VIS-012", [136])
assign_pages("VIS-011", [139])
assign_pages("VIS-013", [112])
assign_pages("VIS-014 VIS-081", [117])
assign_pages("VIS-015", [109, 197, 203])
assign_pages("VIS-016 VIS-019 VIS-020", [129, 154])
assign_pages("VIS-017", [266])
assign_pages("VIS-018", [261, 266])
assign_pages("VIS-021 VIS-022 VIS-023", [167])
assign_pages("VIS-024 VIS-025", [179])
assign_pages("VIS-026 VIS-027", [184])
assign_pages("VIS-028", [190])
assign_pages("VIS-029", [197, 203])
assign_pages("VIS-030", [257])
assign_pages("VIS-031", [212])
assign_pages("VIS-032", [215])
assign_pages("VIS-033", [254])
assign_pages("VIS-034", [255])
assign_pages("VIS-035", [256])
assign_pages("VIS-036 VIS-037 VIS-038", [269])
assign_pages("VIS-039 VIS-040 VIS-041 VIS-042", [296, 297, 298])
assign_pages("VIS-043", [304, 305])
assign_pages("VIS-044 VIS-045 VIS-046 VIS-047", [140, 141, 142, 143, 144])
assign_pages("VIS-048 VIS-049", [145, 146, 147, 148])
assign_pages("VIS-050", [80])
assign_pages("VIS-051", [81])
assign_pages("VIS-052", [84])
assign_pages("VIS-053", [87])
assign_pages("VIS-054", [318])
assign_pages("VIS-055", [328])
assign_pages("VIS-056", [340])
assign_pages("VIS-057", [151, 152, 153])
assign_pages("VIS-058", [306, 314])
assign_pages("VIS-059", [316])
assign_pages("VIS-060 VIS-061 VIS-062", [333, 334, 335, 336, 337])
assign_pages("VIS-063", [338])
assign_pages("VIS-064", [149, 150])
assign_pages("VIS-065", [60])
assign_pages("VIS-066", [69])
assign_pages("VIS-067", [62])
assign_pages("VIS-068", [63])
assign_pages("VIS-069", [63, 64])
assign_pages("VIS-070", [64])
assign_pages("VIS-071", [65])
assign_pages("VIS-072", [66])
assign_pages("VIS-073", [67])
assign_pages("VIS-074", [68])
assign_pages("VIS-075", [75])
assign_pages("VIS-076", [77])
assign_pages("VIS-077", [78])
assign_pages("VIS-078", [100])
assign_pages("VIS-079", [339])
assign_pages("VIS-080", [60, 69, 87])
assign_pages("VIS-082", [56, 69, 87, 100])

# Version 2.13 hardware guide: broad supplementary screenshots for actual MPC One
# / One+ and sibling standalone devices. Never use this guide to override MPC 3.9.
V213_CONTEXT_PAGES = {}
def assign_legacy_pages(ids, pages):
    for vis_id in ids.split():
        V213_CONTEXT_PAGES[vis_id] = list(pages)

assign_legacy_pages("VIS-001", [134, 139])
assign_legacy_pages("VIS-002", [134])
assign_legacy_pages("VIS-003 VIS-004 VIS-007", [91])
assign_legacy_pages("VIS-005 VIS-006 VIS-044 VIS-045 VIS-046 VIS-047", [293, 298])
assign_legacy_pages("VIS-008 VIS-009 VIS-010 VIS-012", [270])
assign_legacy_pages("VIS-011", [273])
assign_legacy_pages("VIS-013", [139])
assign_legacy_pages("VIS-014 VIS-081", [147, 156])
assign_legacy_pages("VIS-015", [156, 223])
assign_legacy_pages("VIS-016 VIS-019 VIS-020", [134, 139])
assign_legacy_pages("VIS-017 VIS-018", [176, 177, 178, 179])
assign_legacy_pages("VIS-021 VIS-022 VIS-023", [162, 167])
assign_legacy_pages("VIS-024 VIS-025", [181])
assign_legacy_pages("VIS-026 VIS-027", [266])
assign_legacy_pages("VIS-028", [221])
assign_legacy_pages("VIS-029", [223])
assign_legacy_pages("VIS-030", [231])
assign_legacy_pages("VIS-031", [234, 235, 236])
assign_legacy_pages("VIS-032", [238, 242, 249])
assign_legacy_pages("VIS-033", [262])
assign_legacy_pages("VIS-034", [263])
assign_legacy_pages("VIS-035", [264])
assign_legacy_pages("VIS-036 VIS-038", [188, 191, 195])
assign_legacy_pages("VIS-037", [202, 205, 208])
assign_legacy_pages("VIS-039 VIS-040 VIS-041 VIS-042", [274, 277, 279, 280, 281])
assign_legacy_pages("VIS-043", [282])
assign_legacy_pages("VIS-048 VIS-049", [288, 290, 291, 292])
assign_legacy_pages("VIS-050", [111])
assign_legacy_pages("VIS-051", [336])
assign_legacy_pages("VIS-052", [405])
assign_legacy_pages("VIS-053", [112])
assign_legacy_pages("VIS-054", [310])
assign_legacy_pages("VIS-055", [312])
assign_legacy_pages("VIS-056", [283])
assign_legacy_pages("VIS-057", [185])
assign_legacy_pages("VIS-058", [315])
assign_legacy_pages("VIS-059", [331])
assign_legacy_pages("VIS-060 VIS-061 VIS-062", [327, 328, 329, 330, 331, 332])
assign_legacy_pages("VIS-063", [325])
assign_legacy_pages("VIS-064", [506])
assign_legacy_pages("VIS-065", [317])
assign_legacy_pages("VIS-066", [92])
assign_legacy_pages("VIS-067", [95])
assign_legacy_pages("VIS-068", [99])
assign_legacy_pages("VIS-069", [96])
assign_legacy_pages("VIS-070", [96])
assign_legacy_pages("VIS-071", [97])
assign_legacy_pages("VIS-072", [98])
assign_legacy_pages("VIS-073", [99])
assign_legacy_pages("VIS-074", [99])
assign_legacy_pages("VIS-075", [107])
assign_legacy_pages("VIS-076", [109])
assign_legacy_pages("VIS-077", [110])
assign_legacy_pages("VIS-078", [129])
assign_legacy_pages("VIS-079", [334])
assign_legacy_pages("VIS-080", [317, 319, 92, 99])
assign_legacy_pages("VIS-082", [91, 92, 99, 112, 190])

SESSION = requests.Session()
SESSION.headers.update({"User-Agent":"Mozilla/5.0 (compatible; MPC-UI-reference-catalog/1.0)"})
FAILURES = []

def slug(s, max_len=60):
    s = html.unescape(s or "").lower()
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return (s[:max_len].strip("-") or "reference")

def digest(s):
    return hashlib.sha256(s.encode("utf-8")).hexdigest()

def classify_pdf_url(url):
    u = url.lower()
    if "b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b" in u or "15jm26psbc" in u or "v3.9.pdf" in u:
        return "mpc-v39"
    if "/software/37/" in u or "v3.7.pdf" in u: return "mpc-v37"
    if "/software/36/" in u or "v3.6.pdf" in u: return "mpc-v36"
    if "98138a9e32b84f6146aee38f6d2efd1ad8996d06f7ec4c5bf91ecb369b27a3c4" in u or ("v2.13.pdf" in u and "software" not in u):
        return "mpc-v213-legacy"
    if "2121_kdhebxog" in u or "v2.11.12.pdf" in u:
        return "mpc-v2112-legacy"
    if "99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0" in u:
        return "mpc-v2117-legacy"
    return None

def fetch_bytes(url, timeout=60):
    last = None
    for attempt in range(3):
        try:
            with SESSION.get(url, stream=True, timeout=timeout, allow_redirects=True) as response:
                response.raise_for_status()
                ctype = response.headers.get("content-type", "").split(";")[0].strip().lower()
                chunks, total = [], 0
                for chunk in response.iter_content(1024 * 1024):
                    if not chunk: continue
                    total += len(chunk)
                    if total > 250 * 1024 * 1024: raise RuntimeError("source exceeds 250 MiB safety limit")
                    chunks.append(chunk)
                data = b"".join(chunks)
                if not data: raise RuntimeError("empty response")
                return data, ctype
        except Exception as exc:
            last = exc
            time.sleep(attempt + 1)
    raise RuntimeError(f"failed after retries: {last}")

def ext_from_bytes(data, ctype, url):
    if data.startswith(b"\x89PNG\r\n\x1a\n"): return ".png"
    if data.startswith(b"\xff\xd8\xff"): return ".jpg"
    if data.startswith((b"GIF87a",b"GIF89a")): return ".gif"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP": return ".webp"
    if b"<svg" in data[:2048].lower() or "svg" in ctype: return ".svg"
    suffix = Path(urlparse(url).path).suffix.lower()
    if suffix in {".png",".jpg",".jpeg",".webp",".gif",".svg"}: return ".jpg" if suffix==".jpeg" else suffix
    return ".bin"

def read_docs(root):
    return {p:(root/p).read_text(encoding="utf-8") for p in DOCS if (root/p).exists()}

def collect_images(docs):
    seen, entries = set(), []
    for doc, text in docs.items():
        for match in IMG_RE.finditer(text):
            alt, url = match.group(1).strip(), match.group(2).strip()
            if url not in seen:
                seen.add(url)
                entries.append({"doc":doc,"alt":alt,"url":url,"kind":"inline-image"})
    return entries

def collect_source_page_urls(docs):
    allow_hosts = {
        "support.akaipro.com", "akaipro.com", "www.akaipro.com",
        "mpc-tutor.com", "www.mpc-tutor.com",
        "soundonsound.com", "www.soundonsound.com",
        "musicradar.com", "www.musicradar.com",
        "elektronauts.com", "www.elektronauts.com",
        "ragtime.online", "heikkisiltala.net",
        "marronfieldsproduction.hatenablog.com", "note.com", "www.note.com",
        "manuals.plus", "www.manuals.plus",
        "manualslib.com", "www.manualslib.com",
        "manuals.co.uk", "www.manuals.co.uk", "manuals.ca", "www.manua.ls",
        "manualpdf.in", "www.manualpdf.in", "manual.nz", "www.manual.nz",
        "usermanuals.au", "www.usermanuals.au", "pdfmanualer.dk", "www.pdfmanualer.dk",
    }
    found, seen = [], set()
    for text in docs.values():
        raw_urls=[m.group(1) for m in MD_LINK_RE.finditer(text)]
        raw_urls.extend(m.group(0) for m in RAW_URL_RE.finditer(text))
        for raw_url in raw_urls:
            url=raw_url.strip().rstrip(".,;)]}")
            clean,_=urldefrag(url)
            parts=urlparse(clean)
            host=parts.netloc.lower().split(":")[0]
            if host not in allow_hosts:
                continue
            if classify_pdf_url(clean) or parts.path.lower().endswith(".pdf"):
                continue
            if parts.path.lower().endswith((".png",".jpg",".jpeg",".webp",".gif",".svg",".ico")):
                continue
            if clean in seen:
                continue
            seen.add(clean)
            found.append(clean)
    manual_base="https://www.manualslib.com/manual/3930924/Akai-Mpc-Key-37.html"
    manual_pages=sorted({int(page) for page_list in V213_CONTEXT_PAGES.values() for page in page_list})
    manual_urls=[f"{manual_base}?page={page}" for page in manual_pages]
    ordered=[]
    for url in manual_urls+found:
        if url not in ordered:
            ordered.append(url)
    return ordered[:SOURCE_PAGE_LIMIT]

def largest_srcset(srcset):
    candidates=[]
    for item in (srcset or "").split(","):
        bits=item.strip().split()
        if not bits:
            continue
        width=0
        if len(bits)>1:
            try:
                width=int(re.sub(r"[^0-9]","",bits[1]) or 0)
            except ValueError:
                width=0
        candidates.append((width,bits[0]))
    return max(candidates,default=(0,""),key=lambda x:x[0])[1]

def is_probable_screenshot(url,alt="",width=0,height=0):
    parsed=urlparse(url)
    path=parsed.path.lower()
    text=(alt+" "+path).lower()
    if path.endswith((".svg",".ico")) and "screenshot" not in text:
        return False
    if any(word in text for word in ("favicon","avatar","social-icon","twitter-icon","facebook-icon","linkedin-icon","author-photo","logo","play-button","icon-")):
        if "attachments/production/" not in path:
            return False
    is_attachment=("attachments/production/" in path or "helpdesk/attachments/" in path)
    has_image_suffix=path.endswith((".png",".jpg",".jpeg",".webp",".gif",".bmp",".tif",".tiff"))
    dimension_ok=(width>=240 and height>=120)
    alt_suggests_visual=any(word in text for word in ("screen","screenshot","mpc","sampler","browser","mixer","sequencer","waveform","pad perform","track edit","preferences"))
    return is_attachment or has_image_suffix or dimension_ok or alt_suggests_visual

def extract_source_page_images(docs):
    entries=[]
    pages=collect_source_page_urls(docs)
    seen=set()
    for page_no,page_url in enumerate(pages,1):
        try:
            response=SESSION.get(page_url,timeout=(4,8),allow_redirects=True)
            response.raise_for_status()
            ctype=response.headers.get("content-type","").lower()
            if "html" not in ctype and "xhtml" not in ctype:
                continue
            soup=BeautifulSoup(response.text,"html.parser")
            page_title=(soup.title.get_text(" ",strip=True) if soup.title else urlparse(page_url).netloc)
            is_manualslib="manualslib.com" in urlparse(response.url).netloc.lower()
            candidates=[]
            for img in soup.find_all("img"):
                raw=(img.get("data-original") or img.get("data-src") or img.get("data-lazy-src")
                    or img.get("data-image") or img.get("src") or largest_srcset(img.get("srcset") or img.get("data-srcset")))
                if not raw:
                    continue
                image_url=requests.compat.urljoin(response.url,raw.strip())
                if image_url.startswith("data:"):
                    continue
                alt=(img.get("alt") or img.get("title") or "").strip()
                classes=" ".join(img.get("class",[])).lower()
                element_id=(img.get("id") or "").lower()
                parent_classes=" ".join(img.parent.get("class",[])).lower() if img.parent else ""
                combined=classes+" "+element_id+" "+parent_classes
                try: width=int(re.sub(r"[^0-9]","",str(img.get("width",""))) or 0)
                except ValueError: width=0
                try: height=int(re.sub(r"[^0-9]","",str(img.get("height",""))) or 0)
                except ValueError: height=0
                if is_manualslib:
                    # The page navigation contains hundreds of tiny page thumbnails.
                    # Prefer the active full-page viewer and reject nav thumbnails.
                    if any(word in combined for word in ("thumb","thumbnail","nav","carousel","related","recommend","logo","avatar")):
                        continue
                    page_specific=("page" in element_id or "page" in classes or "manual-page" in combined)
                    large=(width>=500 or height>=600)
                    # Manual page viewer is a portrait page scan; tiny horizontal
                    # controls/icons are never a useful match for the referenced screen.
                    if not (page_specific or large):
                        continue
                    if width and height and width<420 and height<550:
                        continue
                if not is_probable_screenshot(image_url,alt,width,height):
                    continue
                score=0
                if is_manualslib:
                    score += 100 if ("page" in element_id or "page" in classes or "manual-page" in combined) else 0
                    score += 30 if width>=500 or height>=600 else 0
                    score += min(width,1200)//40
                    score += min(height,1600)//50
                candidates.append((score,image_url,alt or f"Reference image from {page_title}"))
            if not is_manualslib:
                for meta in soup.find_all("meta"):
                    prop=(meta.get("property") or meta.get("name") or "").lower()
                    if prop in ("og:image","twitter:image","twitter:image:src"):
                        raw=meta.get("content")
                        if raw:
                            image_url=requests.compat.urljoin(response.url,raw.strip())
                            if is_probable_screenshot(image_url,page_title,0,0):
                                candidates.append((0,image_url,f"Article image: {page_title}"))
            candidates.sort(key=lambda x:x[0],reverse=True)
            page_count=0
            per_page_limit=1 if is_manualslib else 12
            for _,image_url,alt in candidates:
                if image_url in seen:
                    continue
                seen.add(image_url)
                entries.append({"doc":"source-page-extraction","alt":alt,"url":image_url,
                    "kind":"extracted-source-image","parent_url":page_url})
                page_count+=1
                if page_count>=per_page_limit or len(entries)>=SOURCE_PAGE_IMAGE_LIMIT:
                    break
            print(f"[source page {page_no}/{len(pages)}] scanned {page_url}; candidate screenshots/images: {page_count}")
            if len(entries)>=SOURCE_PAGE_IMAGE_LIMIT:
                print("[source page scan] reached screenshot image safety limit")
                break
        except Exception as exc:
            item={"kind":"source-page-scan","source_url":page_url,"status":"failed","error":str(exc)}
            FAILURES.append(item)
            print(f"[source page {page_no}/{len(pages)}] FAILED {page_url}: {exc}",file=sys.stderr)
    return entries

def is_image_payload(data,ctype):
    if data.startswith((b"\x89PNG\r\n\x1a\n",b"\xff\xd8\xff",b"GIF87a",b"GIF89a")):
        return True
    if data[:4]==b"RIFF" and data[8:12]==b"WEBP":
        return True
    if b"<svg" in data[:2048].lower():
        return True
    return (ctype or "").lower().startswith("image/")

def download_images(root, docs):
    outdir = root / ASSET_REL / "images"
    outdir.mkdir(parents=True, exist_ok=True)
    mapping, manifest = {}, []
    entries=collect_images(docs)
    seen={item["url"] for item in entries}
    for item in extract_source_page_images(docs):
        if item["url"] not in seen:
            entries.append(item)
            seen.add(item["url"])
    for n, entry in enumerate(entries, 1):
        url=entry["url"]
        name=f"{slug(entry['alt'])}-{digest(url)[:12]}"
        try:
            data,ctype=fetch_bytes(url)
            if not is_image_payload(data,ctype):
                raise RuntimeError(f"response does not look like an image (content-type={ctype})")
            ext=ext_from_bytes(data,ctype,url)
            if ext==".bin":
                raise RuntimeError(f"unknown image format (content-type={ctype})")
            dest=outdir/f"{name}{ext}"
            if not dest.exists() or dest.stat().st_size!=len(data):
                dest.write_bytes(data)
            mapping[url]=(ASSET_REL/"images"/dest.name).as_posix()
            record={"kind":entry.get("kind","inline-image"),"alt":entry["alt"],"source_url":url,
                "local_path":dest.relative_to(root).as_posix(),"bytes":len(data),
                "sha256":hashlib.sha256(data).hexdigest(),"status":"downloaded"}
            if entry.get("parent_url"): record["parent_url"]=entry["parent_url"]
            if entry.get("doc"): record["referenced_from"]=entry["doc"]
            manifest.append(record)
            print(f"[image {n}/{len(entries)}] saved {dest.name}, {len(data)} bytes")
        except Exception as exc:
            item={"kind":entry.get("kind","inline-image"),"alt":entry["alt"],"source_url":url,
                "status":"failed","error":str(exc)}
            if entry.get("parent_url"): item["parent_url"]=entry["parent_url"]
            manifest.append(item); FAILURES.append(item)
            print(f"[image {n}/{len(entries)}] FAILED {url}: {exc}",file=sys.stderr)
    url_to_local={url:path for url,path in mapping.items()
        if any(i["url"]==url and i.get("kind")=="inline-image" for i in entries)}
    return url_to_local, manifest

def collect_pdf_pages(docs):
    pages={k:set() for k in PDF_SOURCES}
    for text in docs.values():
        for url in PDF_LINK_RE.findall(text):
            key=classify_pdf_url(url); match=PAGE_RE.search(url)
            if key and match: pages[key].add(int(match.group(1)))
    pages["mpc-v39"].update(V39_ATLAS_PAGES)
    pages["mpc-v39"].update(p for values in V39_CONTEXT_PAGES.values() for p in values)
    legacy_page_numbers={p for values in V213_CONTEXT_PAGES.values() for p in values}
    pages["mpc-v213-legacy"].update(legacy_page_numbers)
    pages["mpc-v2112-legacy"].update(legacy_page_numbers)
    pages["mpc-v37"].update({75,161,184,203,332})
    pages["mpc-v36"].update({203,329})
    return pages

def download_pdf(root, key):
    folder=Path(tempfile.gettempdir())/"mpc-ui-reference-source-pdfs"; folder.mkdir(parents=True,exist_ok=True)
    target=folder/f"{key}.pdf"
    if target.exists() and target.stat().st_size>100_000:
        try:
            with fitz.open(target) as existing:
                if existing.page_count: return target, "cached"
        except Exception: pass
    last=None
    for url in PDF_SOURCES[key]:
        try:
            data,ctype=fetch_bytes(url,timeout=120)
            if not data.startswith(b"%PDF") and "pdf" not in ctype:
                raise RuntimeError(f"not a PDF: content-type={ctype}, prefix={data[:10]!r}")
            target.write_bytes(data)
            with fitz.open(target) as doc:
                if doc.page_count<1: raise RuntimeError("PDF has zero pages")
                count=doc.page_count
            print(f"[pdf] {key}: downloaded {len(data)} bytes, {count} pages")
            return target,url
        except Exception as exc:
            last=exc; print(f"[pdf] {key} failed {url}: {exc}",file=sys.stderr)
    fail={"kind":"pdf","source_key":key,"status":"failed","error":str(last)}
    FAILURES.append(fail)
    return None,None

def render_pdf(root,key,pdf_path,pages,source_url):
    outdir=root/ASSET_REL/"pdf"/key; cropdir=outdir/"figures"
    outdir.mkdir(parents=True,exist_ok=True); cropdir.mkdir(parents=True,exist_ok=True)
    results=[]
    with fitz.open(pdf_path) as doc:
        for pos,page_no in enumerate(sorted(pages),1):
            if page_no<1 or page_no>doc.page_count:
                results.append({"kind":"pdf-page","source_key":key,"page":page_no,"status":"out-of-range","page_count":doc.page_count})
                continue
            page=doc[page_no-1]; dest=outdir/f"page-{page_no:03d}.jpg"
            pix=page.get_pixmap(matrix=fitz.Matrix(1.45,1.45),alpha=False,colorspace=fitz.csRGB)
            pix.save(str(dest),jpg_quality=78)
            record={"kind":"pdf-page","source_key":key,"page":page_no,
                "local_path":dest.relative_to(root).as_posix(),"bytes":dest.stat().st_size,
                "status":"rendered","source_url":source_url,"figures":[]}
            seen=set(); number=0
            try: info_list=page.get_image_info(xrefs=True)
            except Exception: info_list=[]
            for info in info_list:
                rect=fitz.Rect(info.get("bbox",(0,0,0,0)))
                if rect.is_empty or rect.width<145 or rect.height<75 or rect.width*rect.height<18000: continue
                coords=tuple(round(v,1) for v in (rect.x0,rect.y0,rect.x1,rect.y1))
                if coords in seen: continue
                seen.add(coords)
                clip=fitz.Rect(max(page.rect.x0,rect.x0-2),max(page.rect.y0,rect.y0-2),
                    min(page.rect.x1,rect.x1+2),min(page.rect.y1,rect.y1+2))
                number+=1; crop=cropdir/f"page-{page_no:03d}-figure-{number:02d}.jpg"
                try:
                    cpix=page.get_pixmap(matrix=fitz.Matrix(2.0,2.0),clip=clip,alpha=False,colorspace=fitz.csRGB)
                    cpix.save(str(crop),jpg_quality=88)
                    rel=crop.relative_to(root).as_posix(); record["figures"].append(rel)
                    results.append({"kind":"pdf-figure-crop","source_key":key,"page":page_no,
                        "local_path":rel,"bytes":crop.stat().st_size,"status":"rendered-crop"})
                except Exception as exc: print(f"[crop] {key} page {page_no}: {exc}",file=sys.stderr)
            results.append(record)
            print(f"[page {key} {pos}/{len(pages)}] p{page_no}: {len(record['figures'])} crop(s)")
    return results

def relative_asset_link(root, doc_path, asset_path):
    return Path(os.path.relpath(root/asset_path,root/Path(doc_path).parent)).as_posix()

def rewrite_remote_images(root, docs, mapping):
    for doc_path, original in docs.items():
        def replace(match):
            alt,url=match.group(1),match.group(2)
            if url not in mapping: return match.group(0)
            return f"![{alt}]({relative_asset_link(root,doc_path,mapping[url])})"
        text=IMG_RE.sub(replace,original)
        if doc_path==CATALOG:
            text=text.replace(
                "These are externally hosted images embedded from their source pages, not copied into the application.",
                "Where retrieval succeeded, reference images are vendored locally under MPC3-UI-REFERENCE-ASSETS; source URLs and provenance are recorded in the local gallery manifest. These are documentation-only and are not packaged into the Android app."
            )
        (root/doc_path).write_text(text,encoding="utf-8")

def catalog_rows(text):
    start=text.find("# 9. Full screen/context coverage index")
    end=text.find("# 10. Exact-version screenshot gaps and fallback references",start)
    section=text[start:end if end>=0 else len(text)] if start>=0 else ""
    rows=[]
    for line in section.splitlines():
        if not re.match(r"\|\s*VIS-\d{3}",line): continue
        cells=[x.strip() for x in line.strip().strip("|").split("|")]
        if len(cells)>=3:
            match=re.search(r"VIS-\d{3}",cells[0])
            if match: rows.append({"id":match.group(0),"title":cells[1],"evidence":cells[2],"row":line})
    return start,end,section,rows

def add_local_page_column(root,pages,pdf_ready):
    path=root/CATALOG
    text=path.read_text(encoding="utf-8")
    start,end,section,_=catalog_rows(text)
    if start<0:
        return
    lines=section.splitlines()
    header_idx=next((i for i,line in enumerate(lines) if line.startswith("| ID | Screen / sub-screen |")),None)
    if header_idx is None:
        return
    had_column="Stored screenshot asset(s)" in lines[header_idx]
    if not had_column:
        lines[header_idx]=lines[header_idx][:-1]+" | Stored screenshot asset(s) |"
        if header_idx+1<len(lines) and lines[header_idx+1].startswith("|---"):
            lines[header_idx+1]=lines[header_idx+1][:-1]+" |---|"
    for i,line in enumerate(lines):
        if not re.match(r"\|\s*VIS-\d{3}",line):
            continue
        vis_match=re.search(r"VIS-\d{3}",line)
        if not vis_match:
            continue
        vis_id=vis_match.group(0)
        # Remove the old generated column on repeat runs so updates stay idempotent.
        if had_column:
            line=line.rsplit("|",2)[0]+"|"
        found=[]
        candidates=[]
        candidates.extend(("mpc-v39",p) for p in V39_CONTEXT_PAGES.get(vis_id,[]))
        candidates.extend(("mpc-v213-legacy",p) for p in V213_CONTEXT_PAGES.get(vis_id,[]))
        candidates.extend(("mpc-v2112-legacy",p) for p in V213_CONTEXT_PAGES.get(vis_id,[]))
        # Preserve useful page anchors from this row for non-mapped contexts.
        for url in PDF_LINK_RE.findall(line):
            key=classify_pdf_url(url)
            pm=PAGE_RE.search(url)
            if key and pm:
                candidates.append((key,int(pm.group(1))))
        seen=set()
        for key,page_no in candidates:
            if (key,page_no) in seen or not pdf_ready.get(key,False):
                continue
            seen.add((key,page_no))
            rel=Path("MPC3-UI-REFERENCE-ASSETS")/"pdf"/key/f"page-{page_no:03d}.jpg"
            if (root/"docs"/rel).exists():
                found.append(f"[{key} p.{page_no}]({rel.as_posix()})")
        if not found:
            found.append(f"[local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#{vis_id.lower()})")
        lines[i]=line[:-1]+" "+"; ".join(found)+" |"
    (root/CATALOG).write_text(text[:start]+"\n".join(lines)+"\n"+text[end:],encoding="utf-8")

def build_gallery(root,image_manifest,pdf_manifest,pdf_run_status):
    assets=root/ASSET_REL
    catalog=(root/CATALOG).read_text(encoding="utf-8")
    _,_,_,rows=catalog_rows(catalog)
    rendered={(x.get("source_key"),int(x.get("page"))):x for x in pdf_manifest
        if x.get("kind")=="pdf-page" and x.get("status")=="rendered"}
    crops_by_page={}
    for record in pdf_manifest:
        if record.get("kind")=="pdf-figure-crop" and record.get("status")=="rendered-crop":
            key=(record.get("source_key"),int(record.get("page")))
            crops_by_page.setdefault(key,[]).append(record)
    total_page_images=len(rendered)
    total_crops=sum(len(v) for v in crops_by_page.values())
    manualslib_images={}
    for item in image_manifest:
        if item.get("kind")!="extracted-source-image" or item.get("status")!="downloaded":
            continue
        parent=item.get("parent_url","")
        if "manualslib.com" not in parent.lower() or not item.get("local_path"):
            continue
        page_values=parse_qs(urlparse(parent).query).get("page",[])
        if not page_values:
            continue
        try:
            manual_page=int(page_values[0])
        except (TypeError,ValueError):
            continue
        manualslib_images.setdefault(manual_page,[]).append(item)
    md=[
        "# MPC UI Reference Screenshot Index","",
        "Repository-local visual reference gallery. Every VIS entry is mapped to current OS 3.9 manual page captures and, where downloaded, supplementary older MPC standalone hardware manual captures. Full-page captures retain captions/context; large embedded figure crops are included when the PDF renderer detects them.","",
        "- images/: local copies of screenshots cited by the catalog, MPC UI knowledge base, screen blueprint, migration spec, action catalog and related visual-reference docs, plus images extracted from linked support/tutorial/manual pages.",
        "- pdf/mpc-v39/page-NNN.jpg: current OS 3.9 guide page capture. Some pages contain screenshots; text-led pages are identified as manual-page captures, not falsely labelled whole-screen screenshots.",
        "- pdf/mpc-v213-legacy/page-NNN.jpg: older standalone hardware guide v2.13. Supplemental only; never overrides current MPC 3.9 visual evidence.",
        "- pdf/<source>/figures/: large image regions cropped from the relevant manual page where present.",
        "- manifest.json: source URLs, local paths, file hashes/sizes, extraction status and failures.",
        "- These are documentation/research references and are not packaged into Android app assets or APK.","",
        f"Materialized inventory: {len(image_manifest)} source-image records; {sum(x.get('status')=='downloaded' for x in image_manifest)} downloaded; {sum(x.get('status')=='failed' for x in image_manifest)} failed; {total_page_images} PDF page captures; {total_crops} embedded-figure crops.","",
        "## Screen and sub-screen gallery","",
    ]
    for row in rows:
        vis=row["id"]
        current_pages=V39_CONTEXT_PAGES.get(vis,[])
        legacy_pages=V213_CONTEXT_PAGES.get(vis,[])
        md.extend([f'<a id="{vis.lower()}"></a>',f"### {vis} — {row['title']}","",
            f"Catalog evidence label: {row['evidence']}",""])
        found_any=False
        # Associate page-indexed ManualsLib screenshots with each VIS context even when
        # direct PDF downloads are denied by the host. These are legacy OS 2.x captures.
        for page_no in legacy_pages:
            for item in manualslib_images.get(int(page_no),[]):
                found_any=True
                local=Path(item["local_path"]).relative_to(ASSET_REL).as_posix()
                md.extend([f"**Supplementary ManualsLib screenshot, manual p.{page_no}:** legacy visual only; verify current labels against OS 3.9.","",f"![{vis} ManualsLib page {page_no}]({local})",""])
        for key,select_pages,caption in [
            ("mpc-v39",current_pages,"Current MPC Standalone OS 3.9 manual page capture"),
            ("mpc-v213-legacy",legacy_pages,"Supplementary MPC standalone hardware guide v2.13 page capture — legacy visual only"),
            ("mpc-v2112-legacy",legacy_pages,"Supplementary MPC standalone hardware guide v2.11.12 page capture — legacy visual only"),
        ]:
            for page_no in select_pages:
                if not rendered.get((key,int(page_no))):
                    continue
                found_any=True
                md.extend([f"**{caption}:** page {page_no}. The image below is a full manual-page capture; it may contain a UI figure, a control-detail crop or text-only reference material.","",f"![{vis} {key} page {page_no}](pdf/{key}/page-{page_no:03d}.jpg)",""])
                for crop in crops_by_page.get((key,int(page_no)),[]):
                    crop_rel=Path(crop["local_path"]).relative_to(ASSET_REL).as_posix()
                    md.extend([f"![{vis} extracted figure from {key} page {page_no}]({crop_rel})",""])
        if not found_any:
            md.extend(["Capture status: no mapped local PDF page render was produced for this VIS entry. Consult the direct-image manifest below for images and retrieval failures.",""])
    md.extend(["## Downloaded/extracted direct image references",""])
    for item in image_manifest:
        label=item.get("alt") or "MPC reference image"
        md.extend([f"### {label}",""])
        if item.get("status")=="downloaded":
            local=Path(item["local_path"]).relative_to(ASSET_REL).as_posix()
            md.extend([f"![{label}]({local})","",f"Source page: {item.get('parent_url','direct image URL')}",f"Original image URL: {item['source_url']}",""])
        else:
            md.extend([f"Download failed: {item.get('error','unknown error')}","",f"Source image/page: {item.get('parent_url',item.get('source_url',''))}",""])
    (assets/"INDEX.md").write_text("\n".join(md)+"\n",encoding="utf-8")
    manifest={
        "generated_by":"tools/build_mpc_ui_reference_assets.py",
        "sources":pdf_run_status,
        "inline_and_extracted_images":image_manifest,
        "pdf_pages_and_crops":pdf_manifest,
        "failures":FAILURES
    }
    (assets/"manifest.json").write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    (assets/"README.md").write_text(
        "# MPC UI Reference Assets\n\n"
        "Repository-local reference images for reconstructing MPC One / MPC OS UI. This folder is documentation-only and is not copied into Android app assets or APK.\n\n"
        "- INDEX.md: gallery grouped by all VIS-001 through VIS-082 contexts and all downloaded/extracted source images.\n"
        "- images/: local copies of screenshots from the Markdown index and relevant cited pages.\n"
        "- pdf/mpc-v39/: current OS 3.9 manual page captures and detected embedded figure crops.\n"
        "- pdf/mpc-v213-legacy/: supplemental legacy standalone MPC hardware guide v2.13 page captures, if direct PDF retrieval succeeds.\n"
        "- pdf/mpc-v2112-legacy/: supplemental legacy standalone MPC hardware guide v2.11.12 page captures, if direct PDF retrieval succeeds.\n"
        "- images/: where available, page-specific legacy hardware screenshots recovered from the page-indexed ManualsLib viewer when direct PDF hosts deny retrieval.\n"
        "- pdf/: other cited MPC manuals (v3.6/v3.7) used as secondary historical evidence.\n"
        "- direct-image-source-index.json: original URLs for the 59 catalog-inline reference images after Markdown links have been rewritten locally.\n"
        "- manifest.json: source provenance, local paths, hashes, status, render inventory and failures.\n\n"
        "Original source links and firmware/device caveats remain in the catalog. A page capture is not described as a whole-screen UI screenshot unless the page actually shows that screen.\n",encoding="utf-8")

def preserve_catalog_inline_images(root,image_manifest):
    """Keep provenance for locally vendored catalog images after URLs are rewritten."""
    source_index=root/ASSET_REL/"direct-image-source-index.json"
    if not source_index.exists():
        print("[source index] original direct image URL index is missing; nothing to preserve")
        return
    try:
        payload=json.loads(source_index.read_text(encoding="utf-8"))
    except Exception as exc:
        FAILURES.append({"kind":"direct-image-source-index","status":"failed","error":str(exc)})
        return
    existing={item.get("source_url") for item in image_manifest if item.get("source_url")}
    image_dir=root/ASSET_REL/"images"
    preserved=0
    for source in payload.get("images",[]):
        url=source.get("source_url","")
        if not url or url in existing:
            continue
        base=f"{slug(source.get('alt','reference'))}-{digest(url)[:12]}"
        matches=sorted(image_dir.glob(base+".*"))
        if not matches:
            item={"kind":"catalog-inline-image","alt":source.get("alt","MPC reference image"),
                "source_url":url,"status":"failed","error":"original local image file not found",
                "referenced_from":source.get("source_doc")}
            image_manifest.append(item)
            existing.add(url)
            continue
        file=matches[0]
        data=file.read_bytes()
        record={"kind":"catalog-inline-image","alt":source.get("alt","MPC reference image"),
            "source_url":url,"local_path":file.relative_to(root).as_posix(),
            "bytes":len(data),"sha256":hashlib.sha256(data).hexdigest(),
            "status":"downloaded","referenced_from":source.get("source_doc")}
        image_manifest.append(record)
        existing.add(url)
        preserved+=1
    print(f"[source index] preserved {preserved} original direct screenshot records")

def append_extracted_images_to_catalog(root,image_manifest):
    path=root/CATALOG
    text=path.read_text(encoding="utf-8")
    index_start=text.find("# 9. Full screen/context coverage index")
    if index_start<0:
        return
    heading="## 8.28. Locally archived screenshots/images from cited source pages"
    previous=text.find(heading)
    if previous>=0 and previous<index_start:
        text=text[:previous]+text[index_start:]
        index_start=text.find("# 9. Full screen/context coverage index")
    extracted=[x for x in image_manifest if x.get("kind")=="extracted-source-image" and x.get("status")=="downloaded" and x.get("local_path")]
    failed=[x for x in image_manifest if x.get("kind")=="extracted-source-image" and x.get("status")=="failed"]
    groups={}
    for item in extracted:
        groups.setdefault(item.get("parent_url","unknown source page"),[]).append(item)
    md=[
        heading,"",
        "This section contains local image files discovered by scanning the support/tutorial/manual webpages already cited in the catalog, MPC3-9-OS-KNOWLEDGE-BASE.md and MPC3-REFERENCE-INDEX.md. The screenshots remain in docs/MPC3-UI-REFERENCE-ASSETS/images/ and are indexed individually. Source article links are retained so the context/version can be checked.",
        "",
        f"**Extraction result:** {len(extracted)} additional images saved from {len(groups)} cited source pages; {len(failed)} image-download failures. The same URL is stored only once even when it is used in multiple documents.",
        "",
    ]
    if not extracted:
        md.extend(["No additional source-page images were recovered in this run. Check the image manifest for article-fetch or image-download failures.",""])
    for page_url,items in sorted(groups.items()):
        page_label=page_url
        md.extend([f"### Source page: [{page_label}]({page_url})",""])
        for item in items:
            alt=(item.get("alt") or "MPC reference image").replace("[","").replace("]","")
            rel=Path(item["local_path"]).relative_to(Path("docs")).as_posix()
            md.extend([f"**{alt}**","",f"![{alt}]({rel})","",f"Original image: {item['source_url']}",""])
    if failed:
        md.extend(["### Images discovered but not downloaded",""])
        for item in failed:
            md.extend([f"- {item.get('alt','Reference image')}: {item.get('source_url','')} — {item.get('error','download failed')}",""])
    text=text[:index_start]+"\n".join(md)+"\n"+text[index_start:]
    path.write_text(text,encoding="utf-8")

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--root",default=".")
    root=Path(parser.parse_args().root).resolve()
    docs=read_docs(root)
    if CATALOG not in docs: raise SystemExit("Catalog missing: "+CATALOG)
    (root/ASSET_REL).mkdir(parents=True,exist_ok=True)
    pages=collect_pdf_pages(docs)
    image_map,image_manifest=download_images(root,docs)
    preserve_catalog_inline_images(root,image_manifest)
    rewrite_remote_images(root,docs,image_map)
    pdf_ready={}; pdf_manifest=[]; pdf_run_status={}
    for key in PDF_SOURCES:
        if not pages.get(key):
            pdf_ready[key]=False
            pdf_run_status[key]={"status":"not-needed","source_urls":PDF_SOURCES[key],"pages_requested":0}
            continue
        pdf_path,source_url=download_pdf(root,key)
        if not pdf_path:
            pdf_ready[key]=False
            pdf_run_status[key]={"status":"failed","source_urls":PDF_SOURCES[key],"pages_requested":len(pages[key])}
            continue
        pdf_ready[key]=True
        used_url=PDF_SOURCES[key][0] if source_url=="cached" else source_url
        pdf_run_status[key]={"status":"downloaded","source_urls":PDF_SOURCES[key],"used_url":used_url,"pages_requested":len(pages[key])}
        pdf_manifest.extend(render_pdf(root,key,pdf_path,pages[key],used_url))
    add_local_page_column(root,pages,pdf_ready)
    append_extracted_images_to_catalog(root,image_manifest)
    build_gallery(root,image_manifest,pdf_manifest,pdf_run_status)
    report={"image_references_found":len(image_manifest),
        "inline_images_downloaded":sum(x.get("status")=="downloaded" for x in image_manifest),
        "inline_images_failed":sum(x.get("status")=="failed" for x in image_manifest),
        "pdf_sources_downloaded":[k for k,v in pdf_ready.items() if v],
        "pdf_sources_failed":[k for k,v in pdf_ready.items() if not v],
        "pdf_pages_rendered":sum(x.get("kind")=="pdf-page" and x.get("status")=="rendered" for x in pdf_manifest),
        "pdf_figure_crops":sum(x.get("kind")=="pdf-figure-crop" for x in pdf_manifest),"failures":FAILURES}
    print("\nREFERENCE ASSET SUMMARY\n"+json.dumps(report,indent=2,ensure_ascii=False))
    return 0 if report["inline_images_downloaded"] or report["pdf_pages_rendered"] else 2

if __name__=="__main__": sys.exit(main())
