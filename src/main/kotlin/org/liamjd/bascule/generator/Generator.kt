package org.liamjd.bascule.generator

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.ext.youtube.embedded.YouTubeLinkExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.misc.Extension
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.Constants
import org.liamjd.bascule.assets.AssetsProcessor
import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.plugins.GeneratorPluginLoader
import org.liamjd.bascule.plugins.HandlebarPluginLoader
import org.liamjd.bascule.random
import org.liamjd.bascule.render.MarkdownToHTMLRenderer
import org.liamjd.bascule.scanner.MarkdownScanner
import org.liamjd.bascule.scanner.stripYamlFrontMatter
import org.liamjd.bascule.slug
import picocli.CommandLine
import println.debug
import println.info
import println.reporter
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.reflect.full.createInstance
import kotlin.system.measureTimeMillis

val DEFAULT_PROCESSORS = arrayOf(
    "org.liamjd.bascule.pipeline.IndexPageGenerator",
    "org.liamjd.bascule.pipeline.PostNavigationGenerator",
    "org.liamjd.bascule.pipeline.TaxonomyNavigationGenerator"
)


/**
 * Starts the post and page generation process. Must be run from inside the project folder
 */
@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable, KoinComponent {

    private val logger = KotlinLogging.logger {}

    @CommandLine.Option(
        names = ["-c", "--clean"],
        description = ["do not use caching; clears generation directory and deletes cache for a clean build"]
    )
    var clean: Boolean = false

    @CommandLine.Option(
        names = ["-p", "--project"],
        description = ["YAML configuration file name (without extension) to use instead of the default. Useful if the project yaml name is different from the folder name."],
    )
    var projectName: String? = null

    @CommandLine.Option(
        names = ["-v", "--verbose"],
        description = ["Enable verbose/debug output"]
    )
    var verbose: Boolean = false

    private val fileHandler: BasculeFileHandler by inject { parametersOf() }
    private val currentDirectory = System.getProperty("user.dir")!!
    private lateinit var yamlConfig: String
    private val parentFolder: File = File(currentDirectory)

    override fun run() {
        reporter.verbose = verbose

        val startTime = System.currentTimeMillis()

        yamlConfig = if (!projectName.isNullOrBlank()) {
            "${projectName}.yaml"
        } else {
            "${parentFolder.name}.yaml"
        }

        // build the basic project from the default configuration file
        debug("Opening config file ${parentFolder.absolutePath}/$yamlConfig")
        val configText = File(parentFolder.absolutePath, yamlConfig).readText()
        val project = Project(configText)

        // configure the Markdown processor
        // TODO: load extensions from separate package as a plugin so that I don't need to include every possible Markdown extension in this executable

        val handlebarExtensions = mutableListOf<Extension>()
        handlebarExtensions.add(AttributesExtension.create())
        handlebarExtensions.add(YamlFrontMatterExtension.create())
        handlebarExtensions.add(TablesExtension.create())
        handlebarExtensions.add(HydeExtension.create())
        handlebarExtensions.add(YouTubeLinkExtension.create())

        debug("Constructing Handlebars extensions")
        val handlebarPluginLoader =
            HandlebarPluginLoader(this.javaClass.classLoader, Extension::class, project.config.parentDir)
        if (project.config.extensions != null) {
            val extensions = handlebarPluginLoader.getExtensions(project.config.extensions!!)
            for (ext in extensions) {
                debug("Checking extension ${ext.simpleName}")
                handlebarExtensions.add(ext.createInstance())
            }
        }

        project.config.markdownOptions.set(Parser.EXTENSIONS, handlebarExtensions)
        project.config.markdownOptions.set(HtmlRenderer.GENERATE_HEADER_ID, true)
            .set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
        project.config.markdownOptions.set(HtmlRenderer.INDENT_SIZE, 2) // prettier HTML
        project.config.markdownOptions.set(HydeExtension.SOURCE_FOLDER, project.config.directories.sources.toString())

        val assetsProcessor = AssetsProcessor(project, fileHandler)

        // Redirect System.err to a project log file. slf4j-simple writes all logger.* output
        // to stderr, so this routes all diagnostic logging to <projectname>.log instead of
        // the console — keeping the terminal clean for Reporter output only.
        // The original motivation was silencing Apache FOP noise, but the redirect captures
        // all slf4j traffic. A cleaner alternative would be a simplelogger.properties on the
        // classpath, but that requires embedding configuration in the fat JAR.
        val errDumpFile = File(parentFolder, parentFolder.name + ".log")
        System.setErr(PrintStream(FileOutputStream(errDumpFile)))


        project.clean = clean
        info(Constants.logos[Constants.logos.indices.random()])
        info("Generating your website [$yamlConfig]")
        if (clean) {
            info("Cleaning the output directory before generation and deleting the cache")
        }

        val walker = get<MarkdownScanner> { parametersOf(project) }

        val pageList = walker.calculateRenderSet(!clean)
        debug("walker.calculateRenderSet() has returned ${pageList.size} CacheAndPost items")

        val markdownRenderer = MarkdownToHTMLRenderer(project, fileHandler, get { parametersOf(project) })

        var generated = 0
        val renderMs = measureTimeMillis {
            if (clean) {
                fileHandler.deleteFile(project.config.directories.sources, "${project.name.slug()}.cache.json")
                pageList.forEachIndexed { index, cacheAndPost ->
                    cacheAndPost.post?.let {
                        it.rawContent =
                            fileHandler.readFileAsString(cacheAndPost.post.sourceFileName) // TODO: this still contains the yaml front matter :(
                        markdownRenderer.renderHTML(cacheAndPost.post, index)

                        // if(renderMarkdown) {
                        writeMarkdown(project, it, fileHandler)
                        // }
                        generated++
                    }
                }
            } else {
                pageList.filter { item -> item.mdCacheItem.rerender }.forEachIndexed { index, cacheAndPost ->
                    cacheAndPost.post?.let {
                        it.rawContent =
                            fileHandler.readFileAsString(cacheAndPost.post.sourceFileName) // TODO: this still contains the yaml front matter :(
                        markdownRenderer.renderHTML(cacheAndPost.post, index)

                        // if(renderMarkdown) {
                        writeMarkdown(project, it, fileHandler)
                        // }
                    }
                    generated++
                }
            }
        }

        val cachedCount = pageList.size - generated
        if (!clean && cachedCount > 0) {
            info("Rendered $generated HTML files in ${renderMs}ms ($cachedCount cached)")
        } else {
            info("Rendered $generated HTML files in ${renderMs}ms")
        }

        //TODO: come up with a better asset copying pipeline stage thingy
        assetsProcessor.copyStatics()

        val additionalGenerators = mutableListOf<String>()
        if (project.config.generators.isNullOrEmpty()) {
            additionalGenerators.addAll(DEFAULT_PROCESSORS)
        } else {
            additionalGenerators.addAll(project.config.generators!!)
        }

        val generatorPluginLoader =
            GeneratorPluginLoader(this.javaClass.classLoader, GeneratorPipeline::class, project.config.parentDir)
        val generators = generatorPluginLoader.getGenerators(additionalGenerators)

        if (generators.isEmpty()) {
            error("No generators found in the pipeline. Aborting execution!")
        }

        // TODO: this still doesn't work with the CACHE!
        val renderer by inject<TemplatePageRenderer> { parametersOf(project) }
        getPostsFromCacheAndPost(pageList).process(generators, project, renderer, fileHandler)

        val totalMs = System.currentTimeMillis() - startTime
        info("Generation complete in ${totalMs}ms — site at ${project.config.directories.output}")
    }

    private fun getPostsFromCacheAndPost(cacheSet: Set<CacheAndPost>): List<Post> {
        val postList = mutableListOf<Post>()
        cacheSet.forEach { if (it.post != null) postList.add(it.post) }
        return postList
    }

    /**
     * Write the raw Markdown file to the output directory. This function strips the YAML frontmatter,
     * and prepends the title to the Markdown content as a # block
     * @param project the project configuration
     * @param post the post to write out as Markdown
     * @param fileHandler the file handler to use
     * */
    private fun writeMarkdown(project: Project, post: BasculePost, fileHandler: BasculeFileHandler) {
        // strip YAML first? The raw Markdown does not contain the title
        val stripped = post.rawContent.stripYamlFrontMatter()
        val mdContent = "#${post.title}\n\n$stripped"
        // url ends in .html by default, switch it to .md
        val mdUrl = post.url.replace(".html", ".md", ignoreCase = true)
        fileHandler.writeFile(project.config.directories.output.absoluteFile, mdUrl, mdContent)
    }
}


