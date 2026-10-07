# Reviewed skill VFX atlas

The two VFX references shown in chat are contact sheets with headings, portraits,
unequal frame sizes, and overlapping examples. They are **not a regular sprite
grid**. The actual PNG files are currently absent from the workspace and attachment
directory. No source frames have been guessed or marked reviewed. Runtime now
uses separate procedural VFX when an action, travel or impact track is unavailable.
Reviewed loaded tracks take precedence over those fallbacks.

`skill_effects.properties` lists all 25 requested skills and their existing save IDs.
Every track starts with `reviewed=false` and empty rectangles. This deliberately
prevents a title, portrait, another skill, or an unverified crop entering combat.

## Import actual sources

1. Place the original PNGs here as `skill_effect_reference_1.png` and
   `skill_effect_reference_2.png`, or change the two `sheet.*.path` values to their
   actual resource filenames. Keep both originals intact.
2. Check the originals' dimensions and set `sheet.*.width` / `height` exactly.
   The 1536 x 1024 values describe the displayed references; the loader rejects any
   size mismatch instead of scaling crop coordinates silently.
3. Inspect each skill separately and enter explicit frame rectangles in playback
   order. Exclude headings, portraits, neighboring effects, and unintended opaque
   backgrounds. A rectangle alone cannot separate overlapping artwork: use a
   correctly isolated transparent frame exported from the source, or request the
   original isolated frames. Never pretend a contact-sheet overlap is a clean frame.
4. Set consistent pivots and a common `canvasWidth` for each track. Review each
   sequence in both directions with the tool below. Only then set `reviewed=true`.
5. Verify the action/impact clock against `AbilityAnimationTiming` and the actual
   gameplay event. Preserve existing stable skill IDs when importing. Rebuild and
   restart the game after an import; the manifest and frames are cached once.

Opaque presentation backgrounds cannot safely be removed by indiscriminately
discarding black pixels: black also occurs in weapons, shadows, and silhouettes.
The loader requires alpha and both visible and transparent pixels in every crop.
If a source is opaque, acquire or author an accurately isolated transparent export
from that original before marking its frames reviewed.

## Manifest schema (version 1)

Each track has a `skill.<stable-id>.<track>.` prefix. Available track names are
`buildup`, `action`, `travel`, `impact`, `passive`, and `barrier_block`; the metadata
declares which ones make sense for each skill. Add a track to `skill.<id>.tracks`
only when the source really contains artwork for it. A missing phase returns null
and never reuses another skill's artwork.

| Field | Meaning |
| --- | --- |
| `reviewed` | Must be `true` to load any frames. |
| `sheet` | Exact `sheet.<name>` source, such as `reference1`. |
| `frames` | Semicolon-separated `x,y,width,height,durationSeconds,pivotX,pivotY` entries. |
| `canvasWidth` | Logical source canvas width shared by all frames; must be positive. |
| `startDelaySeconds` | Nonnegative delay measured from this track's activation event. |
| `scale` | Positive uniform multiplier, default 1. |
| `rotationDegrees` | Authored orientation correction, default 0. |
| `offsetX`, `offsetY` | Local source-pixel offset from the live attachment, default 0. |
| `directional` | Whether the caller should supply weapon/projectile aim, default false. |
| `anchor` | `WEAPON`, `SHIELD`, `CASTER`, `PROJECTILE`, `TARGET`, `TARGET_ATTACHED`, `AREA`, `BETWEEN`, or `TRAVEL`. |
| `loop`, `playOnce` | Exactly one true; loop only genuine sustained/passive sequences. |
| `hitFrame`, `impactFrame` | Zero-based event indices, or -1 when this track has no such event. |
| `hitFrames` | Optional strictly increasing comma-separated hit indices for multi-hit action sequences; empty uses `hitFrame`. |

Pivots range from 0 to 1 inside **each** crop. They identify the same focal point
(weapon grip/blade origin, feet, projectile center, or ground contact) across unequal
crop sizes. Frame images are rendered at `requestedWidth * scale / canvasWidth`;
using a shared canvas preserves their relative size and aspect ratio. There is no
crossfade, resampling blur, invented intermediate artwork, or frame-dependent scale.

Durations are explicit, positive seconds for each source frame. The renderer draws
one frame at a time, with nearest-neighbor scaling. It does not mutate the parent
graphics transform/composite or advance any animation clocks while rendering.
Impact clocks start at actual contact; a staff buildup and a projectile travel clock
start at their own events. Match declared event times to the gameplay timing before
approving a track. For Whirlwind or Spiral Cut, use `hitFrames` to identify each
actual contact frame rather than repeating damage every update. If both `hitFrame`
and `hitFrames` are present, `hitFrame` must be -1 or match the first list entry.
The array returned by `getHitTimesSeconds()` includes the track's start delay;
action tracks add their release event time to obtain gameplay contact times.
Projectile damage still follows collision at the live target, independently of a
travel animation's frame indices. Optional event indices expose timing metadata
and never apply damage themselves.

## Validate and review

Production action tracks require one hit event for a single-contact skill, two
for Spiral Cut, and four for Whirlwind. Their frame-start times are measured from
action activation (the shared release time). Combat and physical weapon poses
use these approved frame events. Mismatched event counts and events after 84%
of the attack duration disable that action track and appear in the review report.
Fire Storm and Ice Spear action frames form the shot at the staff; travel tracks
loop on the actual projectile, and collision starts impact and applies damage.

Barriers and marks play their formation sequence once, then retain the final
frame with a subtle pulse until expiration. Shadow Strike and Blink Step travel
tracks create brief afterimages along actual movement. Missing action, travel and
impact tracks use procedural feedback attached to the corresponding runtime
actor. This does not admit an unreviewed sprite crop or borrow another skill's
artwork. Passing parser fixtures does not verify the source artwork.

After compiling with `./build.ps1`, run:

```powershell
java '-Djava.awt.headless=true' -cp out/classes SkillEffectAtlasReview src/main/resources/effects/skill_effects.properties out/skill-atlas-review
java '-Djava.awt.headless=true' -cp out/classes SkillEffectAtlasSmokeTest
java '-Djava.awt.headless=true' -cp out/classes SkillVfxSmokeTest
java '-Djava.awt.headless=true' -cp out/classes SkillVfxRenderCheck out/skill-vfx-preview
```

The reviewer prints rejected/missing tracks and exports one contact sheet per
loaded track with every frame in right/left order over a checker background. It
exits with code 2 while the manifest has unresolved tracks. Crop approval still
requires visual inspection of the actual source: bounds and alpha checks cannot
identify another skill's artwork automatically.

The atlas smoke test generates tiny colored pixel fixtures only in a temporary
test directory, checks timing/pivots/mirroring/transparency/nearest-neighbor
sampling and invalid-manifest rejection, then removes the fixtures.
`SkillVfxSmokeTest` also loads temporary reviewed tracks in an isolated runtime to
verify source art takes precedence over fallback art. These are not game assets.
The VFX render check exports contact sheets in both directions and looping GIFs
of all 20 active skills using their actual gameplay actors.
