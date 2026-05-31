# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Bascule is a Kotlin CLI static site generator (v0.4.0). It reads Markdown files with YAML front matter from a `sources/` directory, renders them to HTML via Handlebars templates, and writes output to a `site/` directory. Run as a fat JAR built with shadow.

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

## CLI Commands (as built JAR)

```bash
java -jar bascule.jar -n <siteName> [--theme <themeName>]   # initialise new project
java -jar bascule.jar generate [-c] [-p <projectName>]      # generate site; -c for clean build
java -jar bascule.jar themes                                 # list built-in themes
```

The generator must be run from inside the project folder. It looks for `<foldername>.yaml` by default (or `<projectName>.yaml` if `-p` is set).

## Architecture

### Entry Point & DI

`main.kt` -> `Bascule` (Picocli `@Command`) -> subcommands `Generator` and `Themes`.

Dependency injection is via **Koin**, configured in `koinModules.kt`. Two modules:
- `generationModule` -- factories for `BasculeCache`, `HandlebarsRenderer`, `ChangeSetCalculator`, `PostBuilder`, `AbstractYamlFrontMatterVisitor`
- `fileModule` -- singleton `BasculeFileHandler`

### Generation Pipeline

`Generator.run()` orchestrates the full build:

1. Reads `<name>.yaml` -> constructs `Project` (from `bascule-lib`) with all directory paths and config
2. Configures Flexmark with extensions (YAML front matter, tables, attributes, YouTube, custom **HydeExtension**)
3. `MarkdownScanner.calculateRenderSet()` determines which posts need re-rendering by comparing file mtimes/sizes against a JSON cache
4. `MarkdownToHTMLRenderer` renders each flagged post from Markdown -> HTML string stored on the `BasculePost`
5. `ProcessPipeline.kt` (extension function `List<Post>.process(...)`) runs the generator pipeline in parallel via coroutines. Default pipeline:
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

A custom Flexmark extension (`flexmark/hyde/`) that implements transclusion -- embedding the content of another source file inline using a special block syntax.

## External Dependency: bascule-lib

Core interfaces (`Post`, `Project`, `GeneratorPipeline`, `AbstractPostListGenerator`, `TemplatePageRenderer`, `FileHandler`, `Theme`, `PostLink`, `Tag`) live in the separate `bascule-lib` library (also owned by v79, published to GitHub Packages). Changes to these interfaces require a new `bascule-lib` release first.

## Testing Notes

The app is difficult to unit test because `Project` construction requires real filesystem state and Koin DI is hard to mock in this setup. Existing tests (`BasculePostTest`, `IndexPageGeneratorTest`) exercise the model and a single pipeline stage using test resource fixtures in `src/test/resources/afternoon/`.
