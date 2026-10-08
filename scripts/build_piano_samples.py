#!/usr/bin/env python3
"""Rebuild BirdNote's piano subset from the pinned Salamander FLAC recordings.

Requires Python 3 and FFmpeg with libvorbis/soxr. Download only when --download
is supplied; cached source files are verified against the provenance manifest.
Outputs are staged and checked against the 1,000,000-byte cap before replacement.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import urllib.parse
import urllib.request


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "licenses/PIANO-PROVENANCE.json"
RATE = 44_100
MAX_BYTES = 1_000_000
OFFSETS = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
GAIN = 1.2  # Shared gain preserves relative recording levels; leaves chord headroom.


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--download", action="store_true")
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text())
    args.source_dir.mkdir(parents=True, exist_ok=True)
    for record in manifest["samples"]:
        path = args.source_dir / record["file"]
        if not path.exists() and args.download:
            url = ("https://raw.githubusercontent.com/sfzinstruments/SalamanderGrandPiano/"
                   + manifest["commit"] + "/Samples/" + urllib.parse.quote(record["file"]))
            request = urllib.request.Request(url, headers={"User-Agent": "BirdNote-asset-build"})
            with urllib.request.urlopen(request, timeout=60) as response:
                path.write_bytes(response.read())
        if hashlib.sha256(path.read_bytes()).hexdigest() != record["sha256"]:
            raise ValueError(f"Source checksum mismatch: {path}")

    output_records = []
    with tempfile.TemporaryDirectory(prefix="birdnote-piano-") as tmp:
        staged = Path(tmp)
        for octave in range(2, 7):
            for note, offset in OFFSETS.items():
                if octave == 6 and note != "C":
                    continue
                midi = 12 * (octave + 1) + offset
                source_midi = min(range(36, 85, 3), key=lambda candidate: abs(candidate - midi))
                source_octave, source_offset = divmod(source_midi - 12, 12)
                source_note = {0: "C", 3: "D#", 6: "F#", 9: "A"}[source_offset]
                source_name = f"{source_note}{source_octave}v8.flac"
                shift = midi - source_midi
                pitch_rate = round(48_000 * 2 ** (shift / 12))
                name = f"{octave}{note}.ogg"
                filters = (
                    "aformat=channel_layouts=mono,"
                    "silenceremove=start_periods=1:start_duration=0.002:"
                    "start_threshold=-60dB:start_silence=0.003,"
                    f"asetrate={pitch_rate},aresample={RATE}:resampler=soxr,"
                    f"atrim=duration=2.5,asetpts=PTS-STARTPTS,volume={GAIN},"
                    "afade=t=out:st=2.25:d=0.25"
                )
                subprocess.run([
                    "ffmpeg", "-v", "error", "-y", "-i", str(args.source_dir / source_name),
                    "-af", filters, "-ac", "1", "-ar", str(RATE), "-c:a", "libvorbis",
                    "-q:a", "4", "-map_metadata", "-1",
                    "-metadata", "artist=Alexander Holm",
                    "-metadata", "album=Salamander Grand Piano v3 - BirdNote subset",
                    "-metadata", "license=https://creativecommons.org/licenses/by/3.0/",
                    "-metadata", "comment=Modified for BirdNote; see assets/ATTRIBUTIONS.txt",
                    str(staged / name),
                ], check=True)
                data = (staged / name).read_bytes()
                output_records.append({"file": name, "source": source_name,
                                       "semitone_shift": shift, "bytes": len(data),
                                       "sha256": hashlib.sha256(data).hexdigest()})
        total = sum(record["bytes"] for record in output_records)
        if len(output_records) != 29 or total > MAX_BYTES:
            raise ValueError(f"Invalid pack: {len(output_records)} samples, {total} bytes")
        destination = ROOT / "assets/notes"
        expected = {record["file"] for record in output_records}
        existing = {path.name for path in destination.glob("*.ogg")}
        if existing != expected:
            raise ValueError("Existing note filenames differ from the expected 29-note pack")
        for record in output_records:
            shutil.copyfile(staged / record["file"], destination / record["file"])
    manifest["processing"] = {
        "velocity_layer": 8, "sample_rate_hz": RATE, "channels": 1,
        "duration_seconds": 2.5, "fade_out_seconds": 0.25,
        "trim_leading_silence_db": -60, "gain": GAIN,
        "pitch_shift": "nearest minor-third recording, at most one semitone",
        "resampler": "FFmpeg soxr", "codec": "OGG Vorbis", "vorbis_quality": 4,
        "ffmpeg_version": subprocess.check_output(["ffmpeg", "-version"], text=True).splitlines()[0],
        "max_total_bytes": MAX_BYTES, "total_bytes": total,
    }
    manifest["outputs"] = output_records
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"Replaced {len(output_records)} piano samples: {total:,} / {MAX_BYTES:,} bytes")


if __name__ == "__main__":
    main()
