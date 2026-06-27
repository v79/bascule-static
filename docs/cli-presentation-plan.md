# Plan: Clean up logging & introduce Mordant

_Implementation plan derived from [cli-presentation-analysis.md](./cli-presentation-analysis.md). Goal: replace the ad-hoc `println` package + hand-rolled `ProgressBar` with a single, swappable reporting seam, then back that seam with [Mordant](https://ajalt.github.io/mordant/guide/) — while making the console-UX vs diagnostic-logging split deliberate rather than accidental._

## Goals

1. **One reporting seam.** All user-facing console output goes through a single `Reporter` abstraction, not scattered free functions and raw `println`.
2. **Deliberate log/UX split.** `logger.*` = diagnostic/file logging; `Reporter` = console UX. No more `info("X") + logger.info{"X"}` duplication.
3. **Mordant backend.** A `MordantReporter` implements the seam; Jansi and the global `AnsiConsole.systemInstall()` are removed.
4. **Verbosity control.** Replace the hardcoded `val debug = true` with a real `-v/--verbose` flag.

Non-goal: changing *what* is reported or the generation logic itself. This is a presentation/plumbing refactor. Keep behaviour observable-equivalent until Phase 3.

## Guiding principles

- **Preserve call-site ergonomics.** There are ~131 call sites across 20 files, many in non-Koin contexts (e.g. `ScannerFunctions`, `BasculePost` companion, top-level functions). Full constructor injection everywhere is disproportionate. Keep the free-function ergonomics by having `info/error/debug` **delegate to a single swappable `Reporter` instance**. This matches the analysis insight: reimplement those ~4 things on a new backend and the whole app changes without touching call sites.
- **Land it in small, compiling, test-green steps.** Each phase below should build and pass tests on its own.
- **Backend swap is the last, smallest step.** Most of the value (and risk) is in the seam and the log/UX cleanup, which are library-agnostic.

## Phase 0 — Decisions & prep

- [X] Confirm Mordant major version and API surface to target (2.x `progressAnimation` vs 3.x `progressBarLayout`/`animation`). Check latest on Maven Central; artifact `com.github.ajalt.mordant:mordant`. **Decided: 3.0.2.**
- [X] Decide injection strategy: **global swappable instance** (`var reporter: Reporter = MordantReporter()` in `println.kt`) behind the existing free functions. Koin-managed classes that need testability can swap the instance in tests. No Koin `single` registration needed for now.
- [ ] Decide verbosity model: simple `-v/--verbose` boolean, or a level (`-v`, `-vv`). Recommend a boolean to start (`quiet` default + `verbose`), expandable later.

## Phase 1 — Introduce the `Reporter` seam (no behaviour change, still Jansi-backed)

Goal: a seam exists and is wired through, but output looks identical. Pure refactor.

- [x] Define `interface Reporter` with the minimal surface the codebase actually uses:
  - `info(msg: String)`, `error(msg: String)`, `warn(msg: String)`, `debug(msg: String)`
  - `progress(label: String, current: Int, message: String?)` / `clearProgress()`
- [x] Implement `JansiReporter : Reporter` that contains **exactly the current behaviour** (move the bodies of `println.info/error/debug` and `ProgressBar` into it verbatim).
- [x] Provide a single swappable instance: `var reporter: Reporter = MordantReporter()` in `println.kt`. Free functions `info/error/warn/debug/progress/clearProgress` delegate to `reporter.*` so **call sites don't change**.
- [ ] Optionally register `Reporter` as a Koin `single` in `fileModule` for classes that should receive it by injection later. _(skipped for now — global instance sufficient)_
- [X] Build + run a full `generate` on a sample site; confirm output is visually unchanged.

_Result: one place now owns console output, even though it's still Jansi underneath._

## Phase 2 — Make the log/UX split deliberate & add verbosity

Goal: stop the accidental coupling and the duplication; control noise.

- [X] Add `-v/--verbose` (and/or `-q/--quiet`) options to the Picocli `Generator` command (and `Bascule` root if appropriate). Thread the resulting verbosity into the `Reporter` instance.
- [X] Replace the hardcoded `val debug = true` constant; `debug(...)` output is now gated by verbosity on the `Reporter`, not a compile-time constant. _(Note: the constant was removed from `println.kt` but `debug()` still always prints in `MordantReporter` — gating is not yet implemented.)_
- [X] Sweep the ~20 files and **de-duplicate**: where a message is emitted both via `Reporter` (`info/error`) and `logger.*`, decide its home:
  - User-facing progress/status → `Reporter` only.
  - Diagnostic detail / errors-for-debugging → `logger.*` only.
- [X] Revisit the `System.setErr(...)` redirect in `Generator.run()`. It currently sends *all* slf4j-simple output to the `.log` file as a side effect of silencing Apache FOP. Prefer configuring FOP's own logger (or the slf4j config) to suppress that noise, rather than hijacking stderr globally. At minimum, document the intent explicitly in code.
- [X] Re-point Koin's `PrintLogger()` (DI logs) behind verbosity too, or switch to `EmptyLogger` by default and `PrintLogger` only under `--verbose`.

_Result: console shows curated UX; the log file holds diagnostics; verbosity is a real flag._

## Phase 3 — Add Mordant & implement `MordantReporter`

Goal: swap the backend. Small, because the seam already exists.

- [X] Add Mordant to `gradle/libs.versions.toml` (`mordant = "3.0.2"`) and `build.gradle.kts` (`implementation(libs.mordant)`).
- [X] Add `minimize { exclude(dependency("com.github.ajalt.mordant:.*")) }` in `shadowJar` — Mordant uses `ServiceLoader` for terminal detection; `minimize()` strips the provider class otherwise.
- [X] Create one shared `Terminal` instance (Mordant auto-detects ANSI/Windows support). Lives in `MordantReporter`.
- [X] Implement `MordantReporter : Reporter` using Mordant styled text (`TextColors`, `terminal.println(...)`). Colour scheme: info→brightYellow, error→brightRed, warn→brightMagenta, debug→cyan, progress→brightCyan/brightMagenta.
- [X] Fixed cursor column drift in `progress()`: Mordant's `up(n)`/`down(n)` preserve the column (unlike Jansi's `cursorUpLine/DownLine` which reset to col 0). Fix: call `startOfLine()` before `down(1)` and after `up(1)`.
- [X] Switch the default `reporter` instance from `JansiReporter` to `MordantReporter`.
- [ ] Delete `JansiReporter` once Mordant output is confirmed good on all platforms.

## Phase 4 — Progress reporting on Mordant

Goal: replace the fragile cursor `ProgressBar` with Mordant's progress API.

- [X] Replace `println.ProgressBar` usage in `ChangeSetCalculator` with `println.progress/clearProgress` through the `Reporter` seam. Also fixed recursive `walkFolder` bug (return value was discarded, losing subdirectory counts).
- [X] Replace `println.ProgressBar` usage in `AssetsProcessor.copyDirectory()` with `println.progress/clearProgress`. _(Note: the plan only mentioned `ChangeSetCalculator` and the commented-out `MarkdownToHTMLRenderer` — `AssetsProcessor` was an additional site found during implementation.)_
- [X] Removed commented-out dead `ProgressBar` code from `MarkdownToHTMLRenderer`.
- [X] Deleted `ProgressBar.kt` — no longer referenced anywhere.
- [X] **Remove `Thread.sleep()` test scaffolding** before shipping: `Thread.sleep(50)` in `ChangeSetCalculator.walkFolder()` and `Thread.sleep(100)` in `MordantReporter.progress()` — added to make the spinner visible during visual testing.
- [ ] Ensure a **single component owns the progress line** at a time. This is the concurrency-safe pattern the analysis calls out — important if the parallelism work (see `coroutines-analysis.md`) ever lands. The hand-rolled cursor bar garbles under concurrent writes; Mordant's animation driven from one coordinating thread does not.
- [ ] Consider replacing the hand-rolled spinner in `MordantReporter.progress()` with Mordant's native progress animation API (`terminal.progressAnimation { }`) for smoother rendering without needing `Thread.sleep()` to see it.
- [ ] Consider richer output where cheap: a summary table at the end (files scanned / rendered / skipped / errors), styled error blocks.

## Phase 5 — Remove Jansi & final cleanup

- [X] Delete `AnsiConsole.systemInstall()` from `Bascule.init` (Mordant supersedes it for Windows ANSI).
- [X] Remove the `jansi` dependency from `libs.versions.toml` and `build.gradle.kts`.
- [X] Delete `JansiReporter.kt`.
- [ ] Update `CLAUDE.md` to reflect the new reporting model.

## Dependency changes (summary)

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | ✅ added `mordant = "3.0.2"`; later **remove** `jansi` |
| `build.gradle.kts` | ✅ added `implementation(libs.mordant)` + minimize exclusion; later remove `implementation(libs.jansi)` |
| `Bascule.kt` | remove `AnsiConsole.systemInstall()` |

## Risks & gotchas

- **`shadowJar { minimize() }`** ✅ handled — `minimize { exclude(dependency("com.github.ajalt.mordant:.*")) }` added after `ServiceConfigurationError` on first fat JAR run.
- **The `System.setErr` redirect** interacts with everything that writes to stderr (slf4j-simple). Touch it carefully; verify the `.log` file still captures FOP noise and that intended console output is unaffected.
- **Concurrency** — keep progress rendering single-owner (Phase 4). Don't reintroduce the garble problem the analysis describes.
- **Tests** — `IndexPageGeneratorTest`, `ChangeSetCalculatorTest`, etc. exercise classes that emit output. If any assert on stdout/stderr, the seam change may require updating them; injecting a no-op/`Reporter` test double is the clean fix and a side benefit of the seam.
- **Non-Koin call sites** — top-level functions and companion objects can't receive injected dependencies; the global-swappable-instance approach (Phase 1) is what keeps these working without invasive signature changes.

## Definition of done

- All console output routes through `Reporter`; no direct `Ansi.ansi()` / raw `println` for user-facing messages.
- `MordantReporter` is the default; Jansi dependency and `AnsiConsole.systemInstall()` are gone.
- `-v/--verbose` controls debug output; `val debug = true` is removed.
- Console = curated UX, `.log` file = diagnostics; no duplicated info/log lines.
- Fat JAR (post-`minimize()`) produces correct, styled output on Windows and a sample generate run.
- `CLAUDE.md` updated.

## Open questions

- Verbosity: boolean or multi-level (`-v`/`-vv`)?
- Keep the `println.info/error/debug` import path as thin delegators (zero call-site churn) or migrate call sites to an injected `Reporter` over time?
- Should the end-of-run summary table be in scope now, or a follow-up?
