#!/usr/bin/env python3
"""Prepare two verified official Akai Program-track supplemental screenshots."""
from __future__ import annotations
import hashlib, json, struct, urllib.request
from pathlib import Path

ROOT = Path.cwd()
STAGE = Path("/tmp/mpc-reference-supplement")
ASSET_STAGE = STAGE / "images"
DOC_STAGE = STAGE / "docs" / "MPC3-UI-REFERENCE-ASSETS"
SUPPORT_PAGE = "https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs"
MIRROR_PAGE = "https://inmusicsupport.freshdesk.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs"
RECORDS = [
    {
        "filename": "akai-support-midi-program-track.png",
        "alt": "MIDI Program track and MIDI port/bank fields — Akai support reference",
        "description": "Supplemental Akai support screenshot of MIDI Program track assignment/port/bank controls. Reference crop only; not a complete MPC OS 3.9 manual page.",
        "source_url": "https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69135881931/original/bqZGmdu8u4pgkbgocrWUvj5ORztigcvR3Q.png?1732017125",
        "bytes": 36212,
        "sha256": "a4fb47c0670e527ca8dd601ca3602ea7d5eb76aca41cc851d42c0bdf20d6d08e",
        "width": 762, "height": 286,
    },
    {
        "filename": "akai-support-cv-program-track.png",
        "alt": "CV Program track and CV/Gate port fields — Akai support reference",
        "description": "Supplemental Akai support screenshot of CV Program track and CV/Gate port assignments. Reference crop only; not a complete MPC OS 3.9 manual page.",
        "source_url": "https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69135882043/original/8CqFcu1LB9zW826nl4wgBnCXtXM1kFHc3Q.png?1732017213",
        "bytes": 32208,
        "sha256": "da62f86d249d4380a50cf330371c07eff431e98d3d52fc7e2b5e432a1239bb03",
        "width": 764, "height": 289,
    },
]

def download_and_verify(record: dict) -> None:
    req = urllib.request.Request(record["source_url"], headers={"User-Agent":"Mozilla/5.0 MPC-reference-archive/1.0"})
    with urllib.request.urlopen(req, timeout=35) as response:
        data = response.read(8 * 1024 * 1024 + 1)
    if len(data) != record["bytes"]:
        raise RuntimeError(f"unexpected byte count: {record['filename']} ({len(data)})")
    if hashlib.sha256(data).hexdigest() != record["sha256"]:
        raise RuntimeError(f"SHA-256 mismatch: {record['filename']}")
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise RuntimeError(f"not PNG: {record['filename']}")
    width, height = struct.unpack(">II", data[16:24])
    if (width, height) != (record["width"], record["height"]):
        raise RuntimeError(f"unexpected dimensions: {record['filename']} {width}x{height}")
    ASSET_STAGE.mkdir(parents=True, exist_ok=True)
    (ASSET_STAGE / record["filename"]).write_bytes(data)

def main() -> None:
    manifest_path = ROOT / "docs/MPC3-UI-REFERENCE-ASSETS/manifest.json"
    index_path = ROOT / "docs/MPC3-UI-REFERENCE-ASSETS/INDEX.md"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    index = index_path.read_text(encoding="utf-8")
    for record in RECORDS:
        download_and_verify(record)

    supplementals = manifest.setdefault("supplemental_images", [])
    for record in RECORDS:
        entry = {
            "kind": "official-support-supplemental",
            "status": "downloaded",
            "alt": record["alt"],
            "description": record["description"],
            "source_url": record["source_url"],
            "source_page": SUPPORT_PAGE,
            "source_page_mirror": MIRROR_PAGE,
            "local_path": "docs/MPC3-UI-REFERENCE-ASSETS/images/" + record["filename"],
            "bytes": record["bytes"],
            "sha256": record["sha256"],
            "width": record["width"],
            "height": record["height"],
            "format": "PNG",
            "reference_scope": "supplemental Akai support article; not an MPC OS 3.9 manual page capture",
        }
        existing = [x for x in supplementals if x.get("source_url") == record["source_url"]]
        if existing:
            if existing[0] != entry:
                raise RuntimeError(f"existing supplemental record differs: {record['filename']}")
        else:
            supplementals.append(entry)

    inventory = "Materialized inventory: 402 source-image records; 370 downloaded; 32 failed; 248 PDF page captures; 281 embedded-figure crops."
    if inventory not in index:
        raise RuntimeError("original inventory line not found")
    extended = inventory + "\n\nSupplemental inventory: 2 additional official Akai support screenshots (MIDI Program and CV Program, both under VIS-081). These do not replace any of the 32 failed original source-image records."
    index = index.replace(inventory, extended, 1)

    marker = "Catalog evidence label: Inline Main and Program references\n"
    insert = marker + "\n**Supplementary official Akai support article screenshots (reference crops; not MPC OS 3.9 manual page captures):**\n\n![MIDI Program track and MIDI port/bank fields — Akai support reference](images/akai-support-midi-program-track.png)\n\n![CV Program track and CV/Gate port fields — Akai support reference](images/akai-support-cv-program-track.png)\n\n"
    if index.count(marker) != 1:
        raise RuntimeError("VIS-081 catalog label not found exactly once")
    if "images/akai-support-midi-program-track.png" not in index:
        index = index.replace(marker, insert, 1)
    if index.count("### VIS-") != 82:
        raise RuntimeError("VIS index must retain exactly 82 named VIS sections")

    notes = """# MPC UI Reference Asset Recovery Notes

**Status checked:** 2026-10-31  
**Policy:** exact source-image failures remain failures unless the exact original image is recovered. New supplemental screenshots are labelled separately and are never silently mapped to missing source records.

## Original catalogue inventory

The original catalogue still contains 402 source-image records: 370 downloaded and 32 failed. The 32 failures consist of:
- 30 obsolete Akai Professional 2020 MPC-101 images under the old /wp-content/uploads/sites/2/2020/05/ path;
- one old Akai 2020 banner (Screen-Shot-03-05-20-at-04.11-PM.png), which is not a missing UI screen;
- one generic Freshdesk image retrieval failure, which is not a useful MPC screen reference.

The 30 obsolete MPC-101 image URLs return HTTP 404. Retrying the originals did not recover them. Wayback CDX and the Availability API did not return usable archived copies for those old URLs. The 9 MPC-Tutor source-page scans remain HTTP 403, and archive lookup did not return retrievable page captures. One older v2.11.7 manual PDF source also remains unavailable (403).

The MPC-Tutor page failures remain in manifest.json with their source URLs and errors. Do not count page text, unrelated thumbnails, or replacement images as recovered page screenshots.

## New supplemental official Akai screenshots

The current Akai support article “Understanding and Loading Programs” yielded two additional official support-reference images. They are useful for Program-specific Main / Track Type context (VIS-081), but are reference crops from a support article, not complete MPC OS 3.9 manual pages and not recovered copies of deleted 2020 MPC-101 assets.

1. MIDI Program track
   - Source page: https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs
   - Mirror: https://inmusicsupport.freshdesk.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs
   - Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69135881931/original/bqZGmdu8u4pgkbgocrWUvj5ORztigcvR3Q.png?1732017125
   - Local file: images/akai-support-midi-program-track.png
   - Dimensions / bytes: 762 x 286 / 36,212
   - SHA-256: a4fb47c0670e527ca8dd601ca3602ea7d5eb76aca41cc851d42c0bdf20d6d08e

2. CV Program track
   - Source page: same Akai support article above.
   - Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69135882043/original/8CqFcu1LB9zW826nl4wgBnCXtXM1kFHc3Q.png?1732017213
   - Local file: images/akai-support-cv-program-track.png
   - Dimensions / bytes: 764 x 289 / 32,208
   - SHA-256: da62f86d249d4380a50cf330371c07eff431e98d3d52fc7e2b5e432a1239bb03

The support article also returned four other screenshot files whose SHA-256 values match images already present in the catalogue. The four duplicates add no new images; one banner is not a UI reference and is excluded. A generic Freshdesk image was also excluded.

## Accounting

- inline_and_extracted_images remains the original 402 records (370 downloaded / 32 failed).
- The two new images live in the separate top-level supplemental_images list.
- The VIS gallery therefore has two additional local image embeds while retaining the 82 named screen contexts.
- The new images are not packaged into the Android APK; they remain documentation/reference assets.
"""
    DOC_STAGE.mkdir(parents=True, exist_ok=True)
    (DOC_STAGE / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (DOC_STAGE / "INDEX.md").write_text(index, encoding="utf-8")
    (DOC_STAGE / "RECOVERY-NOTES.md").write_text(notes, encoding="utf-8")
    print(json.dumps({
        "supplementals": len(RECORDS),
        "original_source_image_records": len(manifest["inline_and_extracted_images"]),
        "failed_original_source_images": sum(x.get("status") != "downloaded" for x in manifest["inline_and_extracted_images"]),
        "supplemental_manifest_records": len(manifest["supplemental_images"]),
        "VIS_headings": index.count("### VIS-"),
        "image_embeds": index.count("!["),
        "staging": str(STAGE),
    }, indent=2))

if __name__ == "__main__":
    main()
