#!/usr/bin/env python3
"""Retry MPC UI reference retrieval via original URLs, Wayback CDX and Availability API.

Exact original assets and page-derived supplemental candidates are separated.
This probe never changes or substitutes product/reference files automatically.
"""
from __future__ import annotations
import concurrent.futures, hashlib, html, json, re, zipfile
from pathlib import Path
from urllib.parse import quote, urljoin, urlparse
import requests
from bs4 import BeautifulSoup

MANIFEST=Path("docs/MPC3-UI-REFERENCE-ASSETS/manifest.json")
OUT=Path("/tmp/mpc-reference-recovery")
USER_AGENT="Mozilla/5.0 (compatible; MPC-Reference-Archive-Recovery/2.0; +https://github.com/MiguelDuval/mpc-mk2-groovebox)"
MAX_BYTES=12*1024*1024
TIMEOUT=(12,35)
SESSION=requests.Session()
SESSION.headers.update({"User-Agent":USER_AGENT,"Accept":"image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"})

def safe_name(value,fallback="asset"):
    name=Path(urlparse(value).path).name or fallback
    name=re.sub(r"[^A-Za-z0-9._-]+","-",name).strip(".-_")
    return name[:150] or fallback

def image_magic(data):
    return data.startswith(b"\x89PNG\r\n\x1a\n") or data.startswith(b"\xff\xd8\xff") or data.startswith((b"GIF87a",b"GIF89a")) or (len(data)>=12 and data[:4]==b"RIFF" and data[8:12]==b"WEBP") or data.lstrip().startswith(b"<svg")

def request_bytes(url):
    r=SESSION.get(url,timeout=TIMEOUT,allow_redirects=True,stream=True)
    r.raise_for_status()
    data=bytearray()
    for chunk in r.iter_content(128*1024):
        if chunk:
            data.extend(chunk)
            if len(data)>MAX_BYTES: raise ValueError(f"response exceeds {MAX_BYTES} bytes")
    return bytes(data),r.url,r.headers.get("Content-Type","")

def save_image(url,dest,min_bytes=1500):
    data,final_url,ctype=request_bytes(url)
    if len(data)<min_bytes or not image_magic(data):
        raise ValueError(f"not a usable image (type={ctype!r}, bytes={len(data)})")
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_bytes(data)
    return {"url":url,"final_url":final_url,"path":str(dest.relative_to(OUT)),"bytes":len(data),"sha256":hashlib.sha256(data).hexdigest(),"content_type":ctype}

def archive_snapshots(url):
    snapshots=[]
    errors=[]
    cdx="https://web.archive.org/cdx/search/cdx?"+("url="+quote(url,safe=":/?=&%")+"&output=json&filter=statuscode:200&fl=timestamp,original,digest&collapse=digest")
    try:
        data,_,_=request_bytes(cdx)
        rows=json.loads(data.decode("utf-8","replace"))
        if len(rows)>1:
            entries=rows[1:]
            entries.sort(key=lambda row:row[0],reverse=True)
            snapshots.extend((row[0],row[1],"cdx") for row in entries[:8])
        else:
            errors.append({"service":"cdx","result":"no archived captures"})
    except Exception as exc:
        errors.append({"service":"cdx","error":str(exc)[:400]})
    if not snapshots:
        for stamp in ("20200501","20201001","20210101","20230101","20250101"):
            api="https://archive.org/wayback/available?url="+quote(url,safe=":/?=&%")+"&timestamp="+stamp
            try:
                data,_,_=request_bytes(api)
                obj=json.loads(data.decode("utf-8","replace"))
                closest=obj.get("archived_snapshots",{}).get("closest")
                if closest and closest.get("available") and closest.get("status")=="200":
                    ts=closest.get("timestamp")
                    original=closest.get("url") or url
                    if ts:
                        snapshots.append((ts,original,"availability-api"))
                        break
                errors.append({"service":"availability","timestamp":stamp,"result":"no nearby capture"})
            except Exception as exc:
                errors.append({"service":"availability","timestamp":stamp,"error":str(exc)[:300]})
    dedup=[]
    seen=set()
    for item in snapshots:
        if item[0] not in seen:
            dedup.append(item); seen.add(item[0])
    return dedup,errors

def archived_url(timestamp,original):
    return f"https://web.archive.org/web/{timestamp}id_/{original}"

def download_original(record):
    src=html.unescape(record.get("source_url") or "")
    if not src.startswith(("http://","https://")):
        return {"status":"failed","reason":"source URL missing/invalid","record":record}
    dest=OUT/"exact-originals"/safe_name(record.get("local_path") or src)
    attempts=[]
    try:
        saved=save_image(src,dest)
        return {"status":"recovered-direct","source_record":record,**saved}
    except Exception as exc:
        attempts.append({"url":src,"error":str(exc)[:500]})
    snapshots,archive_errors=archive_snapshots(src)
    for timestamp,original,service in snapshots[:8]:
        candidate=archived_url(timestamp,original)
        try:
            saved=save_image(candidate,dest)
            return {"status":"recovered-wayback","source_record":record,"archive_timestamp":timestamp,"archive_service":service,"archived_original":original,**saved}
        except Exception as exc:
            attempts.append({"url":candidate,"error":str(exc)[:500]})
    return {"status":"failed","source_record":record,"attempts":attempts,"archive_lookup":archive_errors}

def page_image_candidates(page_url):
    attempts=[]
    pages=[(page_url,"live")]
    snapshots,archive_errors=archive_snapshots(page_url)
    attempts.extend({"archive_lookup":e} for e in archive_errors)
    for timestamp,original,service in snapshots[:3]:
        pages.append((archived_url(timestamp,original),f"{service}:{timestamp}"))
    found=[]
    seen=set()
    for fetch_url,version in pages[:4]:
        try:
            response=SESSION.get(fetch_url,timeout=TIMEOUT,allow_redirects=True)
            response.raise_for_status()
            soup=BeautifulSoup(response.text,"html.parser")
            candidates=[]
            for img in soup.find_all("img"):
                for attr in ("src","data-src","data-original","data-lazy-src"):
                    val=img.get(attr)
                    if val: candidates.append((urljoin(response.url,html.unescape(val)),img.get("alt") or ""))
                srcset=img.get("srcset")
                if srcset:
                    for item in srcset.split(","):
                        val=item.strip().split(" ")[0]
                        if val: candidates.append((urljoin(response.url,html.unescape(val)),img.get("alt") or ""))
            og=soup.find("meta",attrs={"property":"og:image"})
            if og and og.get("content"): candidates.append((urljoin(response.url,html.unescape(og["content"])),"OpenGraph image"))
            for img_url,alt in candidates:
                if img_url in seen or not img_url.startswith(("http://","https://")): continue
                seen.add(img_url)
                pathpart=urlparse(img_url).path.lower()
                if not re.search(r"\.(png|jpe?g|webp|gif)(?:$|/)",pathpart): continue
                if any(token in pathpart for token in ("logo","avatar","no-results","views-count","favicon")): continue
                found.append((img_url,alt,version))
        except Exception as exc:
            attempts.append({"url":fetch_url,"error":str(exc)[:500]})
    return found,attempts

def main():
    if not MANIFEST.is_file(): raise SystemExit(f"manifest not found: {MANIFEST}")
    manifest=json.loads(MANIFEST.read_text(encoding="utf-8"))
    failed_images=[x for x in manifest.get("inline_and_extracted_images",[]) if x.get("status")!="downloaded"]
    failed_pages=[x for x in manifest.get("failures",[]) if x.get("kind")=="source-page-scan"]
    # Also inspect the present official and current inMusic mirrors for replacement/supplemental images.
    extra_pages=[
      "https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs",
      "https://inmusicsupport.freshdesk.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs",
    ]
    OUT.mkdir(parents=True,exist_ok=True)
    exact=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        futures=[pool.submit(download_original,row) for row in failed_images]
        for fut in concurrent.futures.as_completed(futures): exact.append(fut.result())
    exact.sort(key=lambda r:r.get("source_record",{}).get("source_url") or "")
    pages=[]
    page_urls=[x.get("source_url","") for x in failed_pages]+extra_pages
    for page_url in page_urls:
        candidates,attempts=page_image_candidates(page_url)
        saved=[]
        for idx,(img_url,alt,version) in enumerate(candidates,1):
            dest=OUT/"page-derived-candidates"/safe_name(page_url,"page")/f"{idx:03d}-{safe_name(img_url,f'candidate-{idx}.jpg')}"
            try:
                item=save_image(img_url,dest,min_bytes=5000)
                saved.append({"alt":alt,"page_version":version,**item})
            except Exception as exc:
                attempts.append({"url":img_url,"error":str(exc)[:300],"alt":alt})
        pages.append({"page_url":page_url,"candidates":saved,"attempts":attempts})
    report={
      "repository":"MiguelDuval/mpc-mk2-groovebox",
      "generated_by":"scripts/recover_mpc_reference_assets.py v2",
      "policy":"Exact original images and page-derived alternatives are separated; no substitute is silently mapped to a failed source record.",
      "failed_image_records_attempted":len(failed_images),
      "exact_originals_recovered":sum(r["status"].startswith("recovered-") for r in exact),
      "exact_originals_unrecovered":sum(r["status"]=="failed" for r in exact),
      "source_pages_attempted":len(pages),
      "page_derived_candidate_images":sum(len(p["candidates"]) for p in pages),
      "exact_results":exact,"page_results":pages,
    }
    (OUT/"recovery-report.json").write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding="utf-8")
    archive=Path("/tmp/mpc-reference-recovery.zip")
    with zipfile.ZipFile(archive,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=6) as zf:
        for path in OUT.rglob("*"):
            if path.is_file(): zf.write(path,path.relative_to(OUT.parent))
    print(json.dumps({
      "archive":str(archive),"failed_images":len(failed_images),
      "exact_originals_recovered":report["exact_originals_recovered"],
      "exact_originals_unrecovered":report["exact_originals_unrecovered"],
      "source_pages":len(pages),"page_derived_candidate_images":report["page_derived_candidate_images"],
      "archive_bytes":archive.stat().st_size,
    },indent=2))

if __name__=="__main__": main()
