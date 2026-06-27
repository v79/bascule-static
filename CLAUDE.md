# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Bascule is a Kotlin CLI static site generator (v0.5.4). It reads Markdown files with YAML front matter from a `sources/` directory, renders them to HTML via Handlebars templates, and writes output to a `site/` directory. Run as a fat JAR built with shadow.

## Build & Run

```bash
# Build fat JAR
./gradlew shadowJar

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "org.liamjd.bascule.model.BasculePostTest"

# Publish to GitHub Packages
./gradlew publish
```

The build requires `githubUsername` and `githubToken` in `~/.gradle/gradle.properties` to resolve the private `bascule-lib` dependency from GitHub Packages (`https://maven.pkg.github.com/v79/bascule-lib`).

The `shadowJar` task uses `minimize()` to strip unused classes. Mordant is explicitly excluded from minimization (`minimize { exclude(dependency("com.github.ajalt.mordant:.*")) }`) because it uses `ServiceLoader` for terminal detection and minimize strips the provider class otherwise.

## CLI Commands (as built JAR)

```bash
java -jar bascule.jar -n <siteName> [--theme <themeName>]        # initialise new project
java -jar bascule.jar generate [-c] [-p <projectName>] [-v]      # generate site
java -jar bascule.jar themes                                      # list built-in themes
```

- `-c` / `--clean` — delete cache and clear output before generating
- `-p` / `--project` — use a named YAML config instead of the folder name
- `-v` / `--verbose` — enable debug output (gates all `debug()` calls)

The generator must be run from inside the project folder. It looks for `<foldername>.yaml` by default (or `<projectName>.yaml` if `-p` is set).

## Architecture

### Entry Point & DI

`main.kt` -> `Bascule` (Picocli `@Command`) -> subcommands `Generator` and `Themes`.

Dependency injection is via **Koin**, configured in `koinModules.kt`. Two modules:
- `generationModule` -- factories for `BasculeCache`, `HandlebarsRenderer`, `ChangeSetCalculator`, `PostBuilder`, `AbstractYamlFrontMatterVisitor`
- `fileModule` -- singleton `BasculeFileHandler`

Koin is started with `EmptyLogger()` — its internal DI logs are suppressed entirely. Koin initialises before Picocli parses arguments so it cannot be gated on `--verbose`.

### Console Output — the `Reporter` seam

All user-facing terminal output goes through the `Reporter` interface (`src/main/kotlin/println/`). The global instance is `println.reporter`, backed by `MordantReporter` using the [Mordant](https://ajalt.github.io/mordant/) library.

**Rule:** `logger.*` = diagnostic/file logging only; `Reporter` = console UX only. Never duplicate a message to both.

Free functions in `println.kt` delegate to `reporter`:

```kotlin
info("msg")       // bright yellow
warn("msg")       // bright magenta
error("msg")      // bright red
debug("msg")      // cyan — only printed when reporter.verbose = true
progress(label, current, message)  // animated spinner; no-op on non-interactive terminals
clearProgress()   // erase the progress lines
```

`reporter.verbose` is set from the `-v` flag at the start of `Generator.run()`. Swapping the reporter (e.g. for a test no-op) is done by assigning `println.reporter = SomeOtherReporter()`.

`MordantReporter.progress()` / `clearProgress()` guard on `terminal.terminalInfo.supportsAnsiCursor` and become no-ops in CI / piped environments so cursor codes don't pollute logs.

### stderr / log file split

Early in `Generator.run()`, `System.setErr` is redirected to `<projectname>.log`. Because `slf4j-simple` writes all `logger.*` output to `System.err`, this routes all diagnostic logging to the log file and keeps the terminal clean for `Reporter` output. This is the mechanism that enforces the log/UX split.

### Generation Pipeline

`Generator.run()` orchestrates the full build:

1. Reads `<name>.yaml` -> constructs `Project` (from `bascule-lib`) with all directory paths and config
2. Configures Flexmark with extensions (YAML front matter, tables, attributes, YouTube, custom **HydeExtension**)
3. `MarkdownScanner.calculateRenderSet()` determines which posts need re-rendering by comparing file mtimes/sizes against a JSON cache
4. `MarkdownToHTMLRenderer` renders each flagged post from Markdown -> HTML string stored on the `BasculePost`
5. `AssetsProcessor.copyStatics()` copies images, CSS, JS from the project's `assets/` directory into the output
6. `ProcessPipeline.kt` (extension function `List<Post>.process(...)`) runs the generator pipeline in parallel via coroutines. Default pipeline:
   - `IndexPageGenerator` -- writes `index.html`
   - `PostNavigationGenerator` -- wires `newer`/`older` links
   - `TaxonomyNavigationGenerator` -- writes per-tag pages

### Key Types

- **`BasculePost`** (`model/BasculePost.kt`) -- implements `Post` (from `bascule-lib`). Built from Markdown via `BasculePost.Builder.createPostFromYaml()`. Returns sealed `PostStatus`: either a valid `BasculePost` or a `PostGenError`.
- **`MDCacheItem`** -- serialisable cache record per source file (path, size, mtime, layout, PostLink).
- **`CacheAndPost`** -- pairs an `MDCacheItem` with its `BasculePost?` (null when loaded from cache without re-parsing).
- **`Project`** (from `bascule-lib`) -- holds all config: directory paths, markdown options (`MutableDataSet`), model map, postsPerPage, etc.

### Post YAML Front Matter

Required fields: `title`, `layout`. Optional: `author`, `slug`, `date`, `tags`. Any unknown key is stored in `post.attributes`. The `PostMetaData` enum enforces these rules.

### Caching

`BasculeCacheImpl` serialises a `Set<MDCacheItem>` to `<projectname>.cache.json` in the sources directory using `kotlinx.serialization`. A clean build (`-c`) deletes this file. Draft files (names starting with `.` or `__`) are skipped by `ChangeSetCalculator.walkFolder()`.

### Templating

`HandlebarsRenderer` loads `.hbs` files from the project's templates directory. Registered helpers: `forEach`, `paginate`, `localDate`, `capitalize`, `upper`, `slugify`.

### Plugins

External Flexmark extensions and custom `GeneratorPipeline` implementations can be dropped as JARs into the project's `plugins/` folder. `PluginLoader` / `HandlebarPluginLoader` / `GeneratorPluginLoader` load them via `URLClassLoader`.

### Hyde Extension

A custom Flexmark extension (`flexmark/hyde/`) that implements transclusion – embedding the content of another source file inline using a special block syntax.

## External Dependency: bascule-lib

Core interfaces (`Post`, `Project`, `GeneratorPipeline`, `AbstractPostListGenerator`, `TemplatePageRenderer`, `FileHandler`, `Theme`, `PostLink`, `Tag`) live in the separate `bascule-lib` library (also owned by v79, published to GitHub Packages). Changes to these interfaces require a new `bascule-lib` release first.

## Testing Notes

The app is difficult to unit test because `Project` construction requires real filesystem state and Koin DI is hard to mock in this setup. Existing tests (`BasculePostTest`, `IndexPageGeneratorTest`) exercise the model and a single pipeline stage using test resource fixtures in `src/test/resources/afternoon/`.

The intention is to refactor the project to improve testability. The `Reporter` seam helps: tests can swap `println.reporter` for a no-op or capturing implementation without touching call sites.
