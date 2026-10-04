#!/usr/bin/env python3
"""Generate the game's original sound effects using only Python's standard library.

Run from any directory: python3 scripts/generate-sounds.py
The checked-in PCM WAV files play directly; this script never runs in the app.
No recordings, samples, music, or sounds from other games are used.
"""

import math
from pathlib import Path
import random
import struct
import wave

SAMPLE_RATE = 48000
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"


def envelope(t, duration, decay):
    attack = min(1.0, t / .002)
    release = min(1.0, max(0.0, (duration - t) / .014))
    return attack * release * math.exp(-t / decay)


def chime(buffer, start, frequency, duration, gain):
    offset = round(start * SAMPLE_RATE)
    for i in range(round(duration * SAMPLE_RATE)):
        if offset + i >= len(buffer):
            break
        t = i / SAMPLE_RATE
        phase = 2 * math.pi * frequency * t
        # Soft mallet body and a little bell sparkle, with a click-free attack.
        value = (math.sin(phase) + .24 * math.sin(2.01 * phase) * math.exp(-t / .035)
                 + .07 * math.sin(3.98 * phase) * math.exp(-t / .022))
        buffer[offset + i] += gain * envelope(t, duration, duration / 3.4) * value


def impact(buffer, duration, gain):
    rng = random.Random(214)
    phase = 0.0
    for i in range(min(len(buffer), round(duration * SAMPLE_RATE))):
        t = i / SAMPLE_RATE
        frequency = 85 + 150 * math.exp(-t / .018)
        phase += 2 * math.pi * frequency / SAMPLE_RATE
        body = math.sin(phase) + .25 * math.sin(phase * 2)
        snap = rng.uniform(-1, 1) * .18 * math.exp(-t / .006)
        buffer[i] += gain * envelope(t, duration, .026) * (body + snap)


def save(name, buffer):
    # Leave headroom for the few voices mixed by SoundPool; all clips end at zero.
    peak = max(abs(value) for value in buffer) or 1
    pcm = [round(value / peak * .72 * 32767) for value in buffer]
    pcm[0] = pcm[-1] = 0
    with wave.open(str(OUTPUT / f"{name}.wav"), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(struct.pack(f"<{len(pcm)}h", *pcm))


def generate():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    melodies = [
        ("clear_one", .21, [523.25, 783.99]),
        ("clear_two", .28, [523.25, 659.25, 783.99]),
        ("clear_three", .35, [523.25, 659.25, 783.99, 1046.50]),
        ("clear_four", .44, [523.25, 659.25, 783.99, 1046.50, 1318.51]),
    ]
    for name, duration, notes in melodies:
        buffer = [0.0] * round(duration * SAMPLE_RATE)
        impact(buffer, .08, .28)
        for index, frequency in enumerate(notes):
            start = index * .033
            chime(buffer, start, frequency, duration - start, .48)
        if name == "clear_four":
            chime(buffer, .10, 261.63, .30, .30)
            chime(buffer, .13, 783.99, .30, .23)
        save(name, buffer)

    for name, duration, gain, frequency in [
        ("drop", .13, .75, 780), ("lock", .08, .4, 580), ("rotate", .055, .08, 880)
    ]:
        buffer = [0.0] * round(duration * SAMPLE_RATE)
        impact(buffer, duration, gain)
        chime(buffer, 0, frequency, min(duration, .045), .25)
        save(name, buffer)

    for name, duration, notes in [
        ("hold", .14, [523.25, 783.99]), ("undo", .22, [392.00, 523.25, 783.99])
    ]:
        buffer = [0.0] * round(duration * SAMPLE_RATE)
        for index, frequency in enumerate(notes):
            start = index * .045
            chime(buffer, start, frequency, duration - start, .40)
        save(name, buffer)


if __name__ == "__main__":
    generate()
    print(f"Generated 9 original game sounds in {OUTPUT}")
