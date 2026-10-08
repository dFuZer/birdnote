# BirdNote asset attribution status

Updated 4 October 2026. Covers bundled media and fonts; software dependency
notices are a separate review. Source mappings for the SVGs were supplied by the
developer. Licenses were checked on their Wikimedia Commons file-description
pages, rather than using the website footer's license.

## Required notices - packaged

- **29 piano samples, C2–C6**: Salamander Grand Piano v3 by Alexander Holm,
  CC BY 3.0 Unported. Replaced every sample from the unidentified pack.
  Derived from lossless Yamaha C5 recordings, velocity layer 8; credits also
  acknowledge kinwie and the sfzinstruments distribution. Full upstream license
  bundled in `assets/licenses/SALAMANDER-CC-BY-3.0.txt`.
  Modified to mono 44.1 kHz, nearest-note pitch shifting (at most one semitone),
  leading-silence trimming, common gain, 2.5-second length with a 0.25-second fade,
  and OGG Vorbis quality 4. **566,331 bytes total**, below the strict 1,000,000-byte cap.
- **Amaranth Regular and Bold**: Gesine Todt, copyright 2011, SIL OFL 1.1.
  Full copyright/license bundled in `assets/licenses/AMARANTH-OFL.txt`.
- **`bg-score.svg`**: replaced the previous artwork with Music notes on wave
  lines sheet by juicy_fish, Magnific resource 290241273, from the supplied SVG.
  Free license and required attribution checked on 4 October 2026. Removed the white background rectangle, recolored the artwork blue and cropped
  empty margins; artwork remains transparent. Creator, source,
  “Designed by Magnific”, platform and license links are included in app credits.
  Preserve the original download/license document as evidence; the stock terms
  prohibit standalone redistribution and use as a logo/trademark.
- **`bg-piano.svg`**: Music design yellow illustration by studiogstock, Magnific
  resource 4801089. Asset page checked on 4 October 2026: Free license,
  attribution required. Included “Designed by Magnific”, platform/creator/source
  links and licensing guidance in the app's credits. Magnific's guidance permits
  modified artwork in commercial apps and accepts attribution in app credits.
  Preserve the original download's license document as permission evidence;
  stock terms prohibit standalone redistribution and use as a logo/trademark.
- **`note.svg`**: Quarter note with upwards stem.svg; Lachaume, Cdang,
  PianistHere and Luca Ghio credited according to the source history.
  CC BY-SA 4.0; BirdNote modifications identified and distributed under CC BY-SA 4.0.
- **`key-ut.svg`**: CClef.svg by Wikimedia user っ. Selected CC BY-SA 3.0
  from its alternative licenses; BirdNote modifications identified and distributed
  under CC BY-SA 3.0.
- **`croche.svg`**: Dotted eighth note stem up.svg; JLTB34 and PianistHere.
  Developer confirmed removing the dot and transforming the SVG.
  Modified version distributed under CC BY-SA 4.0. Replaced its incorrect local
  Public Domain Mark metadata with CC BY-SA 4.0.

Credits, source URLs, license/legal-code URLs and modification notices are in
`assets/ATTRIBUTIONS.txt`. Settings → Open-source licenses displays these credits
and the complete piano and font licenses offline. The share-alike notices apply to the named
modified SVG assets.

Piano source revision, SHA-256 checksums, source-to-output mappings and processing
settings are recorded in `licenses/PIANO-PROVENANCE.json`; the upstream README
is preserved in `licenses/SALAMANDER-SOURCE-README.md`. Rebuild with Python 3 and
FFmpeg (libvorbis/soxr):

```sh
python3 scripts/build_piano_samples.py --source-dir /tmp/birdnote-salamander-source --download
```

The script verifies cached/downloaded sources against the pinned checksums and
checks the total size before replacing the 29 assets. Original source recordings
are build inputs only and are not shipped in the app.

## No attribution condition identified - provenance packaged

- `key-sol.svg`: G-clef.svg, Rémi Cormier (Luccas); public domain on source page.
- `key-fa.svg`: Bass Clef (34502) - The Noun Project.svg, Jeffri Natasastra;
  CC0 1.0 on source page.
- `diese.svg`: Dièse.svg; author unknown, uploaded by Coyau; public domain.
- `bemol.svg`: Bémol.svg, Coyau; public domain.
- `becarre.svg`: Bécarre.svg, Coyau; public domain.

These are included in the credits for provenance even though their stated
copyright status does not impose an attribution-license condition.

## Original BirdNote artwork

- `assets/bg-note.svg`: home-made, confirmed by the developer on 4 October 2026.
  No third-party artwork attribution required.
- `assets/low-poly-bird.svg`: created by the developer, as confirmed on 4 October
  2026. Replaces `assets/icon.svg`, which is no longer present; the app references
  the new SVG. No third-party attribution required for this original artwork.
  The final launcher icon has not yet been created, as confirmed by the developer.

## Still need licenses, redistribution rights or sources

Do not assume these require attribution or that credit alone grants permission.
The following licenses/ownership have not yet been established:

1. **Wrong-answer sound:** `assets/notes/wrong.mp3`.
   Source supplied by developer: https://www.youtube.com/watch?v=Rk7QGp46eg4
   Video: “8-bit error game sound effects - wrong invalid denied unauthorized sounds”,
   uploaded by Sound4effects | Free Sound effects for editting, 4 September 2023.
   Expanded description checked on 4 October 2026: advertises free downloads and
   use in projects, including by game developers, but does not specify a license,
   attribution terms or explicit app-redistribution conditions. Source/uploader
   credit is bundled; app-redistribution permission remains unverified. Obtain
   applicable terms or written permission from the rights holder, or replace it
   with a sound whose license clearly permits the intended distribution.

For original artwork created by the developer, record that ownership. For third-party
assets, preserve the original license/permission evidence and add any required
credits and notices before publication.

## Attribution audit — 4 October 2026

All 12 current SVGs are listed in the bundled credits. The 29 piano samples match
the provenance manifest; their total is 566,331 bytes. Both Amaranth font files
contain the Gesine Todt 2011 copyright and OFL notice matching the packaged text.
The current debug APK contains the exact current credits and both complete
piano/font licenses. Settings renders the source/license URLs as clickable links.
The supplied SVG source mappings and developer statements of original ownership
remain the provenance basis for transformed artwork; no independent ownership
clearance is implied by this audit.

**Additional distribution issue:** `dFuZer/birdnote` is a PUBLIC GitHub repository.
The public `assets/bg-piano.svg` is byte-for-byte the current Magnific-derived
asset (Git blob SHA `dd843afac6aaf82540b96b7042767d39401e03c1`). Magnific permits
use in app projects with attribution, but its guidance prohibits standalone file
redistribution, and Terms section 8.1 also restricts distribution of modifications.
The public `assets/bg-score.svg` is still the old artwork the developer identified
as copyrighted; it differs from the new local replacement. The new Magnific
replacement should not be exposed as a freely downloadable source asset either.

Resolve the public source distribution separately from in-app credits: obtain
explicit source-redistribution permission, replace the stock assets with original
or openly licensed artwork, or arrange for restricted assets to stay outside
public source distribution. Existing Git history must also be considered; deleting
only the latest file would not remove earlier downloadable copies. No repository
visibility changes, remote deletions or history rewrites were performed in this audit.

Also retain the original download/license records for both Magnific resources;
those records have not been provided. Attribution wording is implemented, while
the error sound's permission and the public source-distribution issue remain open.

References:
- https://github.com/dFuZer/birdnote/blob/master/assets/bg-piano.svg
- https://github.com/dFuZer/birdnote/blob/master/assets/bg-score.svg
- https://www.magnific.com/ai/docs/licenses-attribution
- https://www.magnific.com/legal/terms-of-use

## Asset still to create

- **Final Android launcher icon:** the developer confirmed this is not yet available.
  Existing `ic_launcher*` resources are not the final publication artwork. Create
  the launcher resources before release; using the original `low-poly-bird.svg`
  would introduce no third-party attribution requirement.

## Sources

- https://github.com/sfzinstruments/SalamanderGrandPiano/tree/3382bf9496bba2486f5ab0de55a264d1dfc38404
- https://archive.org/details/SalamanderGrandPianoV3
- https://creativecommons.org/licenses/by/3.0/
- https://www.magnific.com/free-vector/music-design-yellow-illustration_4801089.htm
- https://www.magnific.com/free-vector/music-notes-wave-lines-sheet_290241273.htm
- https://www.magnific.com/ai/docs/licenses-attribution
- https://commons.wikimedia.org/wiki/File:Quarter_note_with_upwards_stem.svg
- https://commons.wikimedia.org/wiki/File:G-clef.svg
- https://commons.wikimedia.org/wiki/File:Bass_Clef_(34502)_-_The_Noun_Project.svg
- https://commons.wikimedia.org/wiki/File:CClef.svg
- https://commons.wikimedia.org/wiki/File:Di%C3%A8se.svg
- https://commons.wikimedia.org/wiki/File:B%C3%A9mol.svg
- https://commons.wikimedia.org/wiki/File:B%C3%A9carre.svg
- https://commons.wikimedia.org/wiki/File:Dotted_eighth_note_stem_up.svg
- https://www.youtube.com/watch?v=Rk7QGp46eg4

The `croche.svg` link label mentioned Music-eighthnote.svg, but its actual destination
was Dotted eighth note stem up.svg. The developer's dot-removal confirmation resolves
that ambiguity; the dotted-note source is the recorded provenance.
