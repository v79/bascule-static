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
import org.liamjd.bascule.plugins.GeneratorPluginLoader
import org.liamjd.bascule.plugins.HandlebarPluginLoader
import org.liamjd.bascule.random
import org.liamjd.bascule.render.MarkdownToHTMLRenderer
import org.liamjd.bascule.scanner.MarkdownScanner
import picocli.CommandLine
import println.debug
import println.info
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.reflect.full.createInstance
import kotlin.system.exitProcess

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
        description = ["do not use caching; clears generation directory for a clean build"]
    )
    var clean: Boolean = false

    private val fileHandler: BasculeFileHandler by inject { parametersOf() }
    private val currentDirectory = System.getProperty("user.dir")!!
    private val yamlConfig: String
    private val parentFolder: File

    init {
        parentFolder = File(currentDirectory)
        yamlConfig = "${parentFolder.name}.yaml"
    }

    override fun run() {
        // build the basic project from the default configuration file
        println("Opening config file ${parentFolder.absolutePath}/$yamlConfig")
        val configText = File(parentFolder.absolutePath, yamlConfig).readText()
        val project = Project(configText)

        // configure the markdown processor
        // TODO: load extensions from separate package as a plugin so that I don't need to include every possible markdown extension in this executable

        val handlebarExtensions = mutableListOf<Extension>()
        handlebarExtensions.add(AttributesExtension.create())
        handlebarExtensions.add(YamlFrontMatterExtension.create())
        handlebarExtensions.add(TablesExtension.create())
        handlebarExtensions.add(HydeExtension.create())
        handlebarExtensions.add(YouTubeLinkExtension.create())

        info("Constructing Handlebars extensions")
        val handlebarPluginLoader =
            HandlebarPluginLoader(this.javaClass.classLoader, Extension::class, project.parentFolder)
        if (project.extensions != null) {
            val extensions = handlebarPluginLoader.getExtensions(project.extensions!!)
            for (ext in extensions) {
                debug("Checking extension ${ext.simpleName}")
                handlebarExtensions.add(ext.createInstance())
            }
        }

        project.markdownOptions.set(Parser.EXTENSIONS, handlebarExtensions)
        project.markdownOptions.set(HtmlRenderer.GENERATE_HEADER_ID, true)
            .set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
        project.markdownOptions.set(HtmlRenderer.INDENT_SIZE, 2) // prettier HTML
        project.markdownOptions.set(HydeExtension.SOURCE_FOLDER, project.dirs.sources.toString())

        val assetsProcessor = AssetsProcessor(project)

        // suppress apache FOP logging to a log file
        // TODO: do this we a better logger?
        val errDumpFile = File(parentFolder, parentFolder.name + ".log")
        val errorOutStream = FileOutputStream(errDumpFile)
        val printStream = PrintStream(errorOutStream)
        System.setErr(printStream);


        project.clean = clean
        info(Constants.logos[(0 until Constants.logos.size).random()])
        info("Generating your website")
        info("Reading yaml configuration file $yamlConfig")

        // TODO: be less aggressive with this, use some sort of caching :)
        // if I don't delete, how do I keep track of deleted files?
        // if I do delete, there is no cache
        // unless I cache all content externally
//		fileHandler.emptyFolder(project.dirs.output, OUTPUT_SUFFIX)
//		fileHandler.emptyFolder(File(project.dirs.output, "tags"))
//		val walker = FolderWalker(project)

        val walker = MarkdownScanner(project)

        val pageList = walker.calculateRenderSet()
        println("walker.calculateRenderSet() has returned ${pageList.size} CacheAndPost items")

        val markdownRenderer = MarkdownToHTMLRenderer(project)

        var generated = 0
        if (clean) {
            pageList.forEachIndexed { index, cacheAndPost ->
                cacheAndPost.post?.let {
                    it.rawContent =
                        fileHandler.readFileAsString(cacheAndPost.post.sourceFileName) // TODO: this still contains the yaml front matter :(
                    markdownRenderer.renderHTML(cacheAndPost.post, index)
                    generated++
                }
            }
        } else {

            pageList.filter { item -> item.mdCacheItem.rerender }.forEachIndexed { index, cacheAndPost ->
                cacheAndPost.post?.let {
                    it.rawContent =
                        fileHandler.readFileAsString(cacheAndPost.post.sourceFileName) // TODO: this still contains the yaml front matter :(
                    markdownRenderer.renderHTML(cacheAndPost.post, index)
                }
                generated++
            }
        }

        logger.info { "${generated} HTML files rendered" }
        info("${generated} HTML files rendered")

        //TODO: come up with a better asset copying pipeline stage thingy
        assetsProcessor.copyStatics()

        val additionalGenerators = mutableListOf<String>()
        if (project.generators.isNullOrEmpty()) {
            additionalGenerators.addAll(DEFAULT_PROCESSORS)
        } else {
            additionalGenerators.addAll(project.generators!!)
        }

        val generatorPluginLoader =
            GeneratorPluginLoader(this.javaClass.classLoader, GeneratorPipeline::class, project.parentFolder)
        val generators = generatorPluginLoader.getGenerators(additionalGenerators)

        if (generators.size == 0) {
            logger.error { "No generators found in the pipeline. Aborting execution!" }
            error("No generators found in the pipeline. Aborting execution!")
            exitProcess(-1)
        }

        // TODO: this still doesn't work with the CACHE!
        val renderer by inject<TemplatePageRenderer> { parametersOf(project) }
        getPostsFromCacheAndPost(pageList).process(generators, project, renderer, fileHandler)


    }

    private fun getPostsFromCacheAndPost(cacheSet: Set<CacheAndPost>): List<Post> {
        val postList = mutableListOf<Post>()
        cacheSet.forEach { if (it.post != null) postList.add(it.post) }
        return postList
    }
}


