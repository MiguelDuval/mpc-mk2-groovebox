#!/usr/bin/env python3
"""Recover failed MPC UI-reference images without silently substituting unrelated media.

Downloads are placed in an isolated report directory. Exact original assets and
page-derived supplemental candidates are kept in separate trees and are never
written into the product repository by this script.
"""
from __future__ import annotations

import concurrent.futures
import hashlib
import html
import json
import os
import re
import sys
import time
import zipfile
from pathlib import Path
from urllib.parse import quote, urljoin, urlparse

import requests
from bs4 import BeautifulSoup

MANIFEST = Path("docs/MPC3-UI-REFERENCE-ASSETS/manifest.json")
OUT = Path("/tmp/mpc-reference-recovery")
USER_AGENT = "Mozilla/5.0 (compatible; MPC-Reference-Archive-Recovery/1.0; +https://github.com/MiguelDuval/mpc-mk2-groovebox)"
MAX_BYTES = 12 * 1024 * 1024
TIMEOUT = (12, 35)

SESSION = requests.Session()
SESSION.headers.update({"User-Agent": USER_AGENT, "Accept": "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"})

def safe_name(value: str, fallback: str = "asset") -> str:
    name = Path(urlparse(value).path).name or fallback
    name = re.sub(r"[^A-Za-z0-9._-]+", "-", name).strip(".-_")
    return name[:150] or fallback

def image_magic(data: bytes) -> bool:
    return (
        data.startswith(b"\x89PNG\r\n\x1a\n")
        or data.startswith(b"\xff\xd8\xff")
        or data.startswith((b"GIF87a", b"GIF89a"))
        or (len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WEBP")
        or data.lstrip().startswith(b"<svg")
    )

def request_bytes(url: str):
    r = SESSION.get(url, timeout=TIMEOUT, allow_redirects=True, stream=True)
    r.raise_for_status()
    body = bytearray()
    for chunk in r.iter_content(128 * 1024):
        if not chunk:
            continue
        body.extend(chunk)
        if len(body) > MAX_BYTES:
            raise ValueError(f"response exceeds {MAX_BYTES} bytes")
    return bytes(body), r.url, r.headers.get("Content-Type", "")

def save_image(url: str, dest: Path, min_bytes: int = 1500):
    data, final_url, content_type = request_bytes(url)
    if len(data) < min_bytes or not image_magic(data):
        raise ValueError(f"not a usable image (type={content_type!r}, bytes={len(data)})")
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)
    return {
        "url": url, "final_url": final_url, "path": str(dest.relative_to(OUT)),
        "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(),
        "content_type": content_type,
    }

def wayback_snapshots(url: str):
    cdx = "https://web.archive.org/cdx/search/cdx?" + (
        "url=" + quote(url, safe=":/?=&%") +
        "&output=json&filter=statuscode:200&fl=timestamp,original,digest&collapse=digest"
    )
    try:
        data, _, _ = request_bytes(cdx)
        rows = json.loads(data.decode("utf-8", "replace"))
        if len(rows) > 1:
            entries = rows[1:]
            entries.sort(key=lambda row: row[0], reverse=True)
            return entries[:8]
    except Exception:
        pass
    return []

def download_original(record: dict):
    src = html.unescape(record.get("source_url") or "")
    if not src.startswith(("http://", "https://")):
        return {"status": "failed", "reason": "source URL is missing or invalid", "record": record}
    local = record.get("local_path") or ""
    filename = safe_name(local or src)
    dest = OUT / "exact-originals" / filename
    attempts = []
    for candidate in [src]:
        try:
            saved = save_image(candidate, dest)
            return {"status": "recovered-direct", "source_record": record, **saved}
        except Exception as exc:
            attempts.append({"url": candidate, "error": str(exc)[:500]})
    for timestamp, original, _digest in wayback_snapshots(src):
        archived_url = f"https://web.archive.org/web/{timestamp}id_/{original}"
        try:
            saved = save_image(archived_url, dest)
            return {
                "status": "recovered-wayback", "source_record": record,
                "archive_timestamp": timestamp, "archived_original": original,
                **saved,
            }
        except Exception as exc:
            attempts.append({"url": archived_url, "error": str(exc)[:500]})
    return {"status": "failed", "source_record": record, "attempts": attempts}

def page_image_candidates(page_url: str):
    attempts = []
    pages = [(page_url, "live")]
    for timestamp, original, _digest in wayback_snapshots(page_url):
        pages.append((f"https://web.archive.org/web/{timestamp}id_/{original}", timestamp))
    found = []
    seen = set()
    for fetch_url, version in pages[:3]:
        try:
            response = SESSION.get(fetch_url, timeout=TIMEOUT, allow_redirects=True)
            response.raise_for_status()
            soup = BeautifulSoup(response.text, "html.parser")
            candidates = []
            for img in soup.find_all("img"):
                for attr in ("src", "data-src", "data-original", "data-lazy-src"):
                    val = img.get(attr)
                    if val:
                        candidates.append((urljoin(response.url, html.unescape(val)), img.get("alt") or ""))
                srcset = img.get("srcset")
                if srcset:
                    for item in srcset.split(","):
                        val = item.strip().split(" ")[0]
                        if val:
                            candidates.append((urljoin(response.url, html.unescape(val)), img.get("alt") or ""))
            og = soup.find("meta", attrs={"property": "og:image"})
            if og and og.get("content"):
                candidates.append((urljoin(response.url, html.unescape(og["content"])), "OpenGraph image"))
            for img_url, alt in candidates:
                if img_url in seen or not img_url.startswith(("http://", "https://")):
                    continue
                seen.add(img_url)
                pathpart = urlparse(img_url).path.lower()
                if not re.search(r"\.(png|jpe?g|webp|gif)(?:$|/)", pathpart):
                    continue
                if any(token in pathpart for token in ("logo", "avatar", "no-results", "views-count", "favicon")):
                    continue
                found.append((img_url, alt, version))
        except Exception as exc:
            attempts.append({"url": fetch_url, "error": str(exc)[:500]})
    return found, attempts

def main():
    if not MANIFEST.is_file():
        raise SystemExit(f"manifest not found: {MANIFEST}")
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    failed_images = [
        item for item in manifest.get("inline_and_extracted_images", [])
        if item.get("status") != "downloaded"
    ]
    failed_pages = [
        item for item in manifest.get("failures", [])
        if item.get("kind") == "source-page-scan"
    ]
    OUT.mkdir(parents=True, exist_ok=True)

    exact_results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        futures = [pool.submit(download_original, row) for row in failed_images]
        for fut in concurrent.futures.as_completed(futures):
            exact_results.append(fut.result())
    exact_results.sort(key=lambda r: (r.get("source_record", {}).get("source_url") or ""))

    page_results = []
    for page in failed_pages:
        page_url = page.get("source_url") or ""
        candidates, attempts = page_image_candidates(page_url)
        saved_candidates = []
        for idx, (img_url, alt, version) in enumerate(candidates, 1):
            name = safe_name(img_url, f"candidate-{idx}.jpg")
            dest = OUT / "page-derived-candidates" / safe_name(page_url, "page") / f"{idx:03d}-{name}"
            try:
                saved = save_image(img_url, dest, min_bytes=5000)
                saved_candidates.append({"alt": alt, "version": version, **saved})
            except Exception as exc:
                attempts.append({"url": img_url, "error": str(exc)[:300], "alt": alt})
        page_results.append({
            "page_url": page_url, "candidates": saved_candidates,
            "candidate_failures": attempts,
        })

    report = {
        "repository": "MiguelDuval/mpc-mk2-groovebox",
        "generated_by": "scripts/recover_mpc_reference_assets.py",
        "policy": "Exact original images and page-derived alternatives are separated. No candidate is silently substituted for a failed source record.",
        "failed_image_records_attempted": len(failed_images),
        "exact_originals_recovered": sum(1 for r in exact_results if r["status"].startswith("recovered-")),
        "exact_originals_unrecovered": sum(1 for r in exact_results if r["status"] == "failed"),
        "failed_source_pages_attempted": len(failed_pages),
        "page_derived_candidate_images": sum(len(p["candidates"]) for p in page_results),
        "exact_results": exact_results,
        "page_results": page_results,
    }
    report_path = OUT / "recovery-report.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    archive = Path("/tmp/mpc-reference-recovery.zip")
    with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for path in OUT.rglob("*"):
            if path.is_file():
                zf.write(path, path.relative_to(OUT.parent))
    print(json.dumps({
        "archive": str(archive),
        "report": str(report_path),
        "failed_images": len(failed_images),
        "exact_originals_recovered": report["exact_originals_recovered"],
        "exact_originals_unrecovered": report["exact_originals_unrecovered"],
        "failed_pages": len(failed_pages),
        "page_derived_candidate_images": report["page_derived_candidate_images"],
        "archive_bytes": archive.stat().st_size,
    }, indent=2))

if __name__ == "__main__":
    main()
