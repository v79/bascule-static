# CLI Presentation / Output Reporting Analysis

_Analysis of the current output/reporting mess (mixed `println` + KotlinLogging) and whether the project can adopt a modern terminal UI library (Kotter, Mordant, or Mosaic)._

## TL;DR

The project is **not** structured for the reactive TUI libraries (Kotter, Mosaic) — there's no central UI state and no seam, so their "own the terminal" model fights the procedural pipeline. But it **is** reasonably positioned for **Mordant**, because all console output already funnels through one tiny `println` package. The right sequence is: **(a) extract a `Reporter` seam (library-agnostic, the real work), then (b) implement it on Mordant (almost cosmetic).** Avoid Kotter/Mosaic.

## How output works today — three tangled channels

**1. The `println` package** (`src/main/kotlin/println/`) — top-level free functions:
- `info()` (yellow), `error()` (red), `debug()` (cyan), all hand-rolling Jansi `Ansi.ansi()` escapes and writing to **stdout**.
- `debug` is gated by a **hardcoded `val debug = true`** constant — not a flag.
- Plus a `ProgressBar` class doing manual cursor positioning (`cursorUpLine` / `cursorDownLine` / `eraseLine`) for a two-line animation.
- These are imported directly into **15 files**.

**2. KotlinLogging → slf4j-simple** — `logger.info/debug/warn{}`, configured by `simplelogger.properties`. Crucially, slf4j-simple routes **everything to System.err**.

**3. Raw `println()` / `print()`** scattered in (`Generator` line ~79, etc.), plus Koin's `PrintLogger()` dumping DI logs to stdout.

~131 output-related call sites across 20 files. Inconsistent by any measure.

## The structural fact that decides everything

Two easily-missed details:

- **`Bascule.init` calls `AnsiConsole.systemInstall()`** (Jansi) — a *global* wrap of stdout/stderr to interpret ANSI on Windows.
- **`Generator.run()` calls `System.setErr(...)`** to redirect stderr into a per-project `.log` file (to silence Apache FOP). **Side effect:** because slf4j-simple writes to stderr, *all logger output disappears into that file during generation.* It's not on the console at all.

So at runtime the two concerns are **accidentally separated**: the logger is effectively a file log, and the console UX is driven almost entirely by the tiny `println` package + `ProgressBar`. In the code they're interleaved and often duplicated (e.g. `ChangeSetCalculator` does `info("X")` *and* `logger.info{"X"}` for the same event) — but the console surface you'd actually be restyling is small and funnels through one choke point.

**That choke point is the saving grace.** There's no `Reporter`/`Console` seam — these are free functions, not an injected interface — but they're only ~4 things (`info` / `error` / `debug` / `ProgressBar`). Reimplement those four on a new backend and the whole app's look changes without touching the 15 call sites.

## Library fit

| Library | Model | Fit here |
|---|---|---|
| **Mordant** | "Print nice things" — styled text, tables, progress bars. Does **not** own the main loop. | **Strong.** Drop a `Terminal` in, reimplement the `println` package + `ProgressBar` on it. Coexists with Picocli. Supersedes Jansi (handles Windows ANSI itself → delete `AnsiConsole.systemInstall()`). Minimal structural change. |
| **Kotter** | Reactive `section { }.run { }`; you mutate state vars that trigger re-render. Wants to **own** a terminal region for the duration. | **Poor without restructure.** The pipeline is procedural and spread across many classes that each emit independently. You'd have to invert control so they report into observable state a top-level section renders. A real rewrite. |
| **Mosaic** | Compose-based (Compose compiler plugin + runtime). Same ownership model as Kotter, heavier buy-in. | **Overkill.** Adds the Compose runtime to a batch SSG. Best for genuinely concurrent, continuously-updating UI — not this. |

The suspicion that the project isn't structured for this is right **for Kotter/Mosaic** — no central UI state, no seam, so the reactive model fights the architecture. But **Mordant** slots in precisely because the console output is already a thin layer.

## What's actually worth doing (library-agnostic first)

1. **Introduce a `Reporter` seam** — replace the free `println.*` functions + `ProgressBar` with one injectable interface (via Koin, like everything else). The single highest-value refactor, independent of which library you pick. Once it exists, swapping backends is localized to one implementation class.
2. **Make the log-vs-UX split deliberate**, not a side effect of `setErr`. Decide: logger = diagnostic/file; reporter = console UX. Kill the `info()` + `logger.info{}` duplication.
3. **Fix `val debug = true`** → a real `-v/--verbose` flag / verbosity level on the Picocli command.
4. **If adopting Mordant:** drop Jansi and the global `AnsiConsole.systemInstall()`; reconsider the blunt `System.setErr` redirect (better to configure FOP's own logger than hijack stderr globally).
5. **Concurrency interaction** (see `coroutines-analysis.md`): the hand-rolled cursor `ProgressBar` will garble under parallel rendering. Mordant's progress animations driven from a single coordinating thread are concurrency-safe; rolling your own isn't. The reporter seam is what lets one component own the progress line if generation is ever parallelized.

## Bottom line

Don't reach for Kotter/Mosaic — the architecture genuinely isn't shaped for them. Sequence the work as:

1. Extract a `Reporter` seam (modest, because everything already routes through the `println` package).
2. Implement it on Mordant (then almost cosmetic).
