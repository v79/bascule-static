# Coroutines / Parallelism Analysis

_Analysis of where kotlinx.coroutines / parallelism could help the Bascule generation pipeline, and whether it's worth it._

## TL;DR

This is a lot of change for little gain. The generation work is **CPU-bound** and Bascule is a **short-lived CLI**, so JVM startup/JIT warmup dominate small builds and parallelism only pays off on large sites. A prior benchmark already showed roughly flat results. Upgrade the (ancient) coroutines dependency regardless, but **measure on a genuinely large site before investing** in parallelism.

## What's actually there today

Coroutines are already a dependency and already used — but **nothing currently runs in parallel.**

- `kotlinx-coroutines-core` is pinned at **1.0.1** (October 2018). Ancient — missing structured-concurrency ergonomics (`coroutineScope`, `awaitAll`, limited-parallelism dispatchers).
- The only usage is `ProcessPipeline.kt`. Despite the docstring ("run … in parallel via coroutines") it's `runBlocking { launch { for (clazz in processors) { … } } }` — a **single** `launch` wrapping a **sequential** `for` loop. The three generators run one after another on one coroutine. Coroutines are involved only because `GeneratorPipeline.process` is declared `suspend` in `bascule-lib`, so `callSuspend` needs a coroutine context. The "parallel" claim in the code comments and CLAUDE.md is aspirational, not real.
- The benchmark notes in `src/test/resources/Coroutine Testing Times.txt` confirm this: coroutine vs sequential came out roughly even (1140ms → 1011ms for 44 files; the 3 generators 221ms → ~230ms), and async file-writing was actually *slower*. The dominant cost is markdown rendering (~660ms of ~895ms), with file I/O a distant second.

## Crucial framing

Two things shape everything:

1. **The work is CPU-bound, not I/O-bound.** Flexmark parse → HTML render → Handlebars compile/apply is all CPU. Coroutines give nothing unless you dispatch onto `Dispatchers.Default` (or a custom pool) to spread across cores. For purely CPU-bound fan-out with no suspension points, a `parallelStream()` or an executor achieves the same thing — coroutines are justified here mainly because the `suspend` signatures already exist and structured concurrency reads cleanly, not because there's async I/O to exploit.

2. **This is a short-lived CLI.** JVM startup + class loading + JIT warmup dominate small builds. Parallelism only pays back on large sites (hundreds/thousands of posts) where the render loop runs long enough to amortize thread setup and reach JIT steady state. The flat benchmark numbers are exactly what you'd expect for 44 files. **Step zero: measure on a large site** before investing — otherwise you're adding concurrency bugs for no wall-clock win.

## Genuinely parallelizable (independent work)

**1. The markdown→HTML render loop — best ROI.** `Generator.run()` lines ~148–166: a sequential `forEachIndexed` over posts, each fully independent. `MarkdownToHTMLRenderer.render()` builds a *fresh* `HtmlRenderer` per call, and the document is already parsed (parsing happened earlier in `PostBuilder`), so there's no shared parser in the hot path. Natural `coroutineScope { posts.map { async(Dispatchers.Default) { … } }.awaitAll() }` candidate, ideally with bounded parallelism to avoid oversubscription.

**2. Per-file markdown parsing during the scan.** `PostBuilder.buildPost` (Flexmark parse + YAML visit) is CPU-heavy and independent per file. Parallelize *just the parsing* while keeping aggregation sequential (parallel-map → sequential-fold). This speeds up the scan without fighting `ChangeSetCalculator`'s stateful nature.

**3. The three pipeline generators (Index / PostNav / Taxonomy).** They produce three disjoint output sets and mostly *read* the post list. Could become genuinely concurrent (one `async`/`launch` each, then join), fixing the current fake-parallel loop. Speedup caps at ~3x and they're light, so modest, but it's the lowest-risk place for truly concurrent work.

## Genuinely sequential / stateful — leave alone or split carefully

- **`ChangeSetCalculator.walkFolder`** — recursive, mutating shared `allSources`, `markdownSourceCount`, `errorMap`, and a progress bar as it goes. The orchestration is sequential; only the inner per-file *parse* (point 2) is safely parallelizable.
- **Tag aggregation** (the `allTags` loop, ~lines 86–101) — order-dependent `postCount++` on shared `Tag` objects. Must stay single-threaded.
- **Cache comparison + `writeCacheFile`** — needs the complete set; runs once at the end.
- **newer/older post linking** (`sortAndLinkPosts`) — inherently ordered.

## Hazards to fix *first* (correctness, not performance)

If/when the render loop is parallelized, these shared-state races need addressing:

- **`fileHandler.createDirectories` / `writeFile`** — different posts can share a destination folder, so concurrent `mkdirs` on the same path can race. Pre-create directories, or guard with a mutex.
- **Counters** — `generated++` and `markdownSourceCount1++` need to be atomic.
- **`ProgressBar`** — writes ANSI to stdout; concurrent updates garble the terminal. Synchronize, or drop per-item progress in parallel mode.
- **Shared `HandlebarsRenderer`** — `render()` builds a fresh model/context each call and looks reentrant, but it reuses one `hbRenderer` and re-reads/`compileInline`s the template every call. jknack Handlebars is generally thread-safe for compile+apply, but **verify** before trusting it under concurrency (and caching the template instead of re-reading the file per render is worth doing anyway as a serial win).

## Recommendation

1. **Upgrade coroutines** (1.0.1 → a current 1.8/1.9.x line; fine with Kotlin 1.9.22) regardless — the old version is a liability.
2. **Measure on a large site first.** If generation isn't actually slow for real content, this is churn.
3. If it *is* slow, target the **render loop** with bounded `Dispatchers.Default` fan-out, *after* fixing the dir/counter/progress-bar/renderer hazards.
4. Then optionally **parallel-parse in the scan** (parse concurrent, aggregate sequential) and **make the 3 generators truly concurrent**.
5. Leave change-set calc, tag aggregation, linking, and cache write sequential.
6. Update CLAUDE.md + the `ProcessPipeline` docstring, which currently overstate today's parallelism.
