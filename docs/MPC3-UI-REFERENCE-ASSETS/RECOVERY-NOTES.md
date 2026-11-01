# MPC UI Reference Asset Recovery Notes

**Status checked:** 2026-10-31  
**Policy:** exact source-image failures remain failures unless the exact original image is recovered. New supplemental screenshots are labelled separately and are never silently mapped to missing source records.

## Original catalogue inventory

The original catalogue still contains 402 source-image records: 370 downloaded and 32 failed. The 32 failures consist of:
- 30 obsolete Akai Professional 2020 MPC-101 images under the old /wp-content/uploads/sites/2/2020/05/ path;
- one old Akai 2020 banner (Screen-Shot-03-05-20-at-04.11-PM.png), which is not a missing UI screen;
- one expired signed Akai support article-logo URL from the Tuning Basics article. A retry using a refreshed signed URL recovered the 5,974-byte red AKAI Professional wordmark (SHA-256: `74cf29e68930a11ba25b2f7faf67f9ea066f056aa2f5f93df6b0c7ae4b7cc3d2`); it is a brand logo, not a UI screenshot, so it was excluded from the visual gallery. The original stale signed URL remains recorded as failed in the source manifest.

The 30 obsolete MPC-101 image URLs return HTTP 404. Retrying the originals did not recover them. Wayback CDX and the Availability API did not return usable archived copies for those old URLs. The 9 MPC-Tutor source-page scans remain HTTP 403, and archive lookup did not return retrievable page captures. One older v2.11.7 manual PDF source also remains unavailable (403).

The MPC-Tutor page failures remain in manifest.json with their source URLs and errors. Do not count page text, unrelated thumbnails, or replacement images as recovered page screenshots. Exact-name web searches for representative MPC-101 filenames also found no indexed external mirrors.

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
