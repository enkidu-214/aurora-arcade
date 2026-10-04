#!/usr/bin/env python3
"""Original mechanical/space pinball PCM effects; Python standard library only.

No recordings, third-party samples or game assets are used. Checked-in WAV files
are loaded by SoundPool, and this offline generator never runs during gameplay.
Recreate from any directory: python3 scripts/generate-pinball-sounds.py
"""

import math
from pathlib import Path
import random
import struct
import wave

SAMPLE_RATE = 48000
OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"


def envelope(t, duration, decay):
    return min(1.0, t / .0015) * min(1.0, max(0, (duration - t) / .018)) * math.exp(-t / decay)


def metal(buffer, start, frequency, duration, gain):
    """Damped inharmonic steel resonance with a short, softer upper partial."""
    offset = round(start * SAMPLE_RATE)
    for i in range(min(round(duration * SAMPLE_RATE), len(buffer) - offset)):
        t = i / SAMPLE_RATE
        phase = 2 * math.pi * frequency * t
        tone = math.sin(phase) + .31 * math.sin(1.414 * phase) * math.exp(-t / .028)
        tone += .12 * math.sin(2.43 * phase) * math.exp(-t / .012)
        buffer[offset + i] += gain * envelope(t, duration, duration / 3.7) * tone


def impact(buffer, start, duration, gain, low=110, high=320):
    offset = round(start * SAMPLE_RATE)
    rng = random.Random(1928 + offset)
    phase = 0.0
    smooth = 0.0
    for i in range(min(round(duration * SAMPLE_RATE), len(buffer) - offset)):
        t = i / SAMPLE_RATE
        phase += 2 * math.pi * (low + (high - low) * math.exp(-t / .012)) / SAMPLE_RATE
        smooth = .65 * smooth + .35 * rng.uniform(-1, 1)
        tone = math.sin(phase) + .2 * math.sin(phase * 2)
        tone += smooth * .3 * math.exp(-t / .006)
        buffer[offset + i] += gain * envelope(t, duration, .024) * tone


def sweep(buffer, start, duration, low, high, gain, airy=False):
    offset = round(start * SAMPLE_RATE)
    phase = 0.0
    smooth = 0.0
    rng = random.Random(710 + offset)
    for i in range(min(round(duration * SAMPLE_RATE), len(buffer) - offset)):
        t = i / SAMPLE_RATE
        progress = t / duration
        frequency = low * (high / low) ** progress
        phase += 2 * math.pi * frequency / SAMPLE_RATE
        smooth = .975 * smooth + .025 * rng.uniform(-1, 1)
        tone = .6 * math.sin(phase) + .12 * math.sin(2 * phase)
        if airy:
            tone = .22 * tone + .85 * smooth
        # The whoosh swells naturally; all sweeps release softly.
        window = math.sin(math.pi * progress) ** .8
        buffer[offset + i] += gain * window * tone


def note(buffer, start, frequency, duration, gain):
    offset = round(start * SAMPLE_RATE)
    for i in range(min(round(duration * SAMPLE_RATE), len(buffer) - offset)):
        t = i / SAMPLE_RATE
        phase = 2 * math.pi * frequency * t
        tone = math.sin(phase) + .19 * math.sin(2 * phase) * math.exp(-t / .04)
        tone += .07 * math.sin(3 * phase) * math.exp(-t / .023)
        buffer[offset + i] += gain * envelope(t, duration, duration / 3) * tone


def save(name, buffer, peak=.68):
    maximum = max(abs(value) for value in buffer) or 1
    pcm = [round(value / maximum * peak * 32767) for value in buffer]
    pcm[0] = pcm[-1] = 0
    with wave.open(str(OUTPUT / f"pin_{name}.wav"), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(struct.pack(f"<{len(pcm)}h", *pcm))


def generate():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    durations = {
        "flipper": .075, "launch": .240, "bumper": .120, "sling": .095,
        "target": .085, "ramp": .330, "skill": .420, "combo": .260,
        "mission": .620, "multiball": .760, "jackpot": .700,
        "save": .320, "drain": .400, "nudge": .090, "game_over": .780,
    }
    clips = {name: [0.0] * round(duration * SAMPLE_RATE) for name, duration in durations.items()}
    impact(clips["flipper"], 0, .075, .9, 105, 240)
    metal(clips["flipper"], .007, 920, .052, .32)
    impact(clips["launch"], 0, .075, .7, 95, 350)
    sweep(clips["launch"], .015, .205, 230, 990, .5)
    metal(clips["launch"], .120, 1480, .100, .13)
    impact(clips["bumper"], 0, .095, .82, 145, 420)
    metal(clips["bumper"], .004, 610, .110, .5)
    impact(clips["sling"], 0, .060, .8, 125, 260)
    sweep(clips["sling"], .003, .085, 840, 180, .56)
    metal(clips["target"], 0, 1250, .085, .75)
    impact(clips["target"], 0, .045, .23, 290, 520)
    sweep(clips["ramp"], 0, .310, 360, 1780, .9, airy=True)
    metal(clips["ramp"], .180, 1110, .145, .16)
    impact(clips["nudge"], 0, .090, .9, 90, 160)

    for name, notes, spacing in [
        ("skill", [493.88, 659.25, 830.61, 987.77], .050),
        ("combo", [440.00, 659.25, 880.00], .033),
        ("mission", [329.63, 415.30, 493.88, 659.25, 830.61], .066),
    ]:
        impact(clips[name], 0, .085, .38, 120, 240)
        for index, frequency in enumerate(notes):
            start = index * spacing
            note(clips[name], start, frequency, durations[name] - start, .42)
        if name == "mission":
            note(clips[name], .210, 164.81, .370, .25)

    # Rising reactor pulses resolve into a warm chord when nova starts.
    for index in range(6):
        start = index * .066
        note(clips["multiball"], start, 220 * 2 ** (index / 6), .170, .34 + index * .025)
    sweep(clips["multiball"], .015, .395, 95, 260, .28, airy=True)
    impact(clips["multiball"], .405, .100, .48, 82, 220)
    for frequency in [220, 440, 554.37, 659.25]:
        note(clips["multiball"], .400, frequency, .350, .29)

    impact(clips["jackpot"], 0, .120, .55, 100, 360)
    for index, frequency in enumerate([164.81, 329.63, 415.30, 493.88, 659.25, 987.77]):
        note(clips["jackpot"], index * .024, frequency, .700 - index * .024, .30)
    metal(clips["jackpot"], .170, 1318.51, .280, .12)
    sweep(clips["save"], 0, .180, 250, 740, .50)
    note(clips["save"], .085, 659.25, .220, .32)
    note(clips["save"], .125, 987.77, .180, .25)
    metal(clips["drain"], 0, 460, .120, .28)
    sweep(clips["drain"], .015, .360, 330, 85, .43)
    for index, frequency in enumerate([392.00, 329.63, 246.94, 196.00]):
        start = index * .095
        note(clips["game_over"], start, frequency, .780 - start, .37)
    for name, buffer in clips.items():
        save(name, buffer)
    return len(clips)


if __name__ == "__main__":
    print(f"Generated {generate()} original pinball effects in {OUTPUT}")
