---
type: spike
status: designed
tags: [music-analysis]
updated: 2026-11-22
---
# Spike: pretrained audio embeddings for genre and mood

## Goal

Find out whether a pretrained audio embedding run through ONNX Runtime (e.g. Essentia Discogs-EffNet / MusiCNN with genre, mood, arousal/valence and danceability heads) is affordable live, licensable, and useful — in particular whether a genre-informed tempo prior closes BeatTracker's double/half-tempo gap.

## Dependencies

_None._

## Related

- [1 Music state descriptors and intensity](01-music-state-descriptors-and-intensity.md)
- [3 Key, mode and mood (arousal/valence)](03-key-mode-and-mood-arousal-valence.md)

## Measures

- **Question:** Can a pretrained audio embedding (e.g. Essentia Discogs-EffNet or MusiCNN, via
  ONNX Runtime for Java) give usable genre/style, mood and danceability live, and does a
  genre-informed tempo prior improve `BeatTracker`?
- **Hypothesis:** An embedding every ~3 s costs well under one core; style probabilities smoothed
  over 10–30 s are stable enough to script against; choosing the tempo prior from the style cuts
  the Acc1/Acc2 gap (70 % / 89 % on GTZAN).
- **Measure:** (1) CPU per inference and native-dependency size on Linux x86-64; (2) licence of
  model weights (Essentia models are believed to be CC BY-NC-SA — confirm); (3) genre accuracy on
  GTZAN mapped from Discogs styles; (4) beatEval Acc1 with a per-track prior from the predicted
  style vs the fixed 120 BPM prior; (5) label stability (changes per minute) on whole tracks.
- **Answer criteria:** adopt if inference < 10 % of a core at one per 3 s, the licence permits
  this project's use, and Acc1 improves by ≥ 5 points without Acc2 dropping; otherwise record
  why not.
- **Time-box:** 3 days.

## Findings

_Written when the spike completes: method, results against the criteria, conclusion, and whether the code should be adopted, adapted or abandoned (and where it lives)._

## Open questions

_None._

## Acceptance criteria

_What must be true when this is done._
