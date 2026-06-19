# HullCheck

**HullCheck** is a fully offline, native Android application for marine surveyors. It lets a single professional create and manage **Boat Projects** (one project = one vessel survey) and work through structured, check-off inspection checklists spanning engine/mechanical systems and the whole vessel (hull, deck, rigging, electrical, plumbing, safety, electronics, documentation, and more). For every item the surveyor can set a condition rating, add notes, attach photos, and flag defects with a severity — then export a clean, professional **PDF report** and a portable **JSON** backup.

The app is built for docks and decks with **no reliable internet**: everything works 100% offline, all data lives on-device, and no backend server is required. The UI is dark-first, high-contrast, glanceable in sunlight, and operable with large touch targets for wet/gloved fingers.

---

## Tech stack

| Concern | Choice |
| --- | --- |
| Language | Kotlin 2.0.21 (JVM target 17) |
| UI | Jetpack Compose + Material 3 (Material You / dynamic color optional) |
| Architecture | MVVM, single-activity, `ViewModel` + `StateFlow`, repository pattern |
| DI | Hilt |
| Persistence | Room (SQLite); photos as files in app-internal storage |
| Async | Coroutines + Flow |
| Images | Coil (thumbnails) + CameraX (in-app capture) + system Photo Picker (gallery) |
| PDF | Android `PdfDocument` + `Canvas` (manual layout, no paid libs) |
| Serialization | `kotlinx.serialization` (JSON import/export + bundled template) |
| Date/time | `java.time` with core-library desugaring |
| Prefs | Jetpack DataStore (Preferences) |
| Fonts | **Inter** bundled as a variable font (offline, no downloadable-font provider) |
| Min / Target SDK | 26 / 34 (compileSdk 35) |
| Network | **No `INTERNET` permission** — fully offline |

## Building

Requires JDK 17 and the Android SDK (platform 34/35, build-tools 34+).

```bash
./gradlew :app:assembleDebug      # build the debug APK
./gradlew installDebug            # build + install to a connected device
./gradlew test                    # JVM unit tests
```

The Gradle wrapper targets Gradle 8.9 / AGP 8.7.3 / KSP. Output APK:
`app/build/outputs/apk/debug/app-debug.apk`.

## Architecture & package layout

```
com.hullcheck
├── HullCheckApplication        @HiltAndroidApp; runs first-launch init (seed template + orphan-photo cleanup)
├── MainActivity                single activity; edge-to-edge; hosts the NavHost; applies theme from settings
├── MainViewModel               exposes theme-relevant settings to the activity
├── di/                         Hilt modules (DatabaseModule, RepositoryModule), qualifiers, AppInitializer
├── data/
│   ├── db/                     Room: entities, DAOs (with SQL progress aggregation), @Relation POJOs, converters
│   ├── model/                  decoupled domain models + enums + value objects (ProjectProgress, …)
│   ├── repository/             ProjectRepository, ChecklistRepository, PhotoRepository, ReportRepository,
│   │                           SettingsRepository, + entity↔domain mappers
│   └── seed/                   bundled checklist template (assets/checklist_default.json) + loader
├── pdf/                        ReportPdfGenerator (A4, paginated, cover/summary/defects/checklist/photo appendix)
├── export/                     ProjectJsonExporter (+ DTOs): Base64-embedded, atomic import with fresh UUIDs
├── ui/
│   ├── theme/                  Color/Type/Theme (dark-first palette, Inter typography)
│   ├── navigation/             Routes + HullCheckNavHost (animated transitions)
│   ├── components/             reusable Composables (ProgressRing, status/condition/severity controls, …)
│   ├── camera/                 in-app CameraX capture + permission handling
│   ├── projects / projecteditor / projectdetail / checklist / itemdetail / defects / report / settings
│                               each: Screen + ViewModel (StateFlow), no business logic in Composables
└── util/                       FileStorage, ImageScaler (EXIF + downscale), DateFormatters, Result, haptics, share
```

### Data model

`Project` → many `Category` → many `Item` → many `Photo`. Categories/items are **instantiated per project** from the template, so editing one boat's checklist never affects another. Cascade foreign keys delete child rows when a project/category/item is removed; the repository deletes the corresponding JPEG **files** (Room only deletes rows). Progress (`% non-PENDING`, per-status counts) is computed in SQL and surfaced reactively via `Flow`.

## Notable engineering decisions

- **Known-good toolchain.** Versions mirror a sibling project that already builds in this environment, eliminating the fragile Gradle/AGP/Kotlin/KSP/Compose-compiler version-matrix risk.
- **Offline Inter font.** Bundled as a single variable TTF and exposed at four weights via `FontVariation` (API 26+), so the brand type renders with zero network dependency — instead of the spec's downloadable-font provider, which needs connectivity/Play Services.
- **No storage permission.** Photos live in app-internal storage; exports go to the app's external Documents dir and are shared via `FileProvider`. Gallery import uses the system Photo Picker. Only `CAMERA` is requested, and that optionally.
- **Undoable photo delete.** Delete removes the DB row immediately and finalizes the file only after the 5-second Undo window; a launch-time orphan pass sweeps any file left behind by an interrupted undo or crash.
- **Atomic JSON import across two storage systems.** Photo bytes (Base64) are written to disk first, then all rows insert in a single Room transaction; a failure deletes the just-written files and rolls back, so a failed import leaves nothing behind. UUIDs are regenerated to avoid collisions, and missing photos are skipped and counted.
- **Big touch targets over gestures.** Status setting uses always-visible 52dp OK/DEFECT/N-A buttons (better for gloves) with a haptic tick and animated color crossfade, rather than swipe gestures.

## Additional capabilities (v1.1)

Beyond the original spec, HullCheck adds:

- **Safety-gear expiry tracking** — an optional expiry date per item with `EXPIRED` / `Expiring soon` badges on the checklist, defects view, and report (driven by `ChecklistItem.expiryStatus`).
- **Structured measurements** — an optional value + unit per item (moisture %, hull thickness, engine hours…), surfaced on the report instead of buried in notes.
- **GPS location capture** — one-tap survey coordinates via the platform `LocationManager` (no Play Services, fully offline); shown on the detail header and report.
- **PDF letterhead + certification** — firm name, license number, logo, signature image, and disclaimer (in Settings) render a branded header and a signed certification block on the report.
- **Checklist templates** — choose a template when creating a project, "Save as template" from any project, and manage saved templates (Settings → Checklist templates).
- **Photo annotation** — freehand multi-colour drawing on a photo, composited (view→pixel mapped) and saved as a new photo.
- **Foldable two-pane** — on screens ≥ 700dp (foldable inner displays, tablets, large landscape) the home shows project list + detail side-by-side via a nested NavHost; phones stay single-pane.

**Production hardening:** Room schema exported and bumped to **v2 with a real `MIGRATION_1_2`** (verified on-device over existing data); a signed **R8/minified release build** (22 MB → 3.5 MB) with working ProGuard keep-rules; JVM unit tests for progress/expiry/measurement logic; and the project is under git.

## Definition-of-done coverage

Project list (search / persisted sort / status filter, cover + progress + status + defect badges), full checklist instantiation (13 categories, 120 items), all five statuses + five conditions + four severities, camera + gallery photos (downscaled, EXIF-correct, captioned, zoomable, undo-deletable), autosaving notes, live progress rings, defect aggregation, professional paginated PDF, JSON round-trip, duplicate/archive/export/delete with confirms and file cleanup, DataStore settings (surveyor name, default survey type, theme, units, photo quality, dynamic color), custom categories/items (template items are N/A-only, never deletable), and process-death/rotation safety via `ViewModel`/`rememberSaveable`.

## License / assets

Inter is licensed under the SIL Open Font License (bundled in `app/src/main/res/font/`).
