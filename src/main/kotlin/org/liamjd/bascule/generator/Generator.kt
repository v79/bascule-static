package org.liamjd.bascule.generator

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.parameter.ParameterList
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.Constants
import org.liamjd.bascule.assets.AssetsProcessor
import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.random
import org.liamjd.bascule.render.MarkdownToHTMLRenderer
import org.liamjd.bascule.scanner.MarkdownScanner
import picocli.CommandLine
import println.debug
import println.info
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf

val DEFAULT_PROCESSORS = arrayOf("org.liamjd.bascule.pipeline.IndexPageGenerator", "org.liamjd.bascule.pipeline.PostNavigationGenerator", "org.liamjd.bascule.pipeline.TaxonomyNavigationGenerator")


/**
 * Starts the post and page generation process. Must be run from inside the project folder
 */
@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable, KoinComponent {

	private val logger = KotlinLogging.logger {}

	@CommandLine.Option(names = ["-c", "--clean"], description = ["do not use caching; clears generation directory for a clean build"])
	var clean: Boolean = false

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })
	private val currentDirectory = System.getProperty("user.dir")!!
	private val yamlConfig: String
	private val parentFolder: File

	val loader = this.javaClass.classLoader

	init {
		parentFolder = File(currentDirectory)
		yamlConfig = "${parentFolder.name}.yaml"
	}

	override fun run() {
		// build the basic project from the default configuration file
		val configText = File(parentFolder.absolutePath, yamlConfig).readText()
		val project = Project(configText)

		// configure the markdown processor
		project.markdownOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create(), TablesExtension.create(), HydeExtension.create()))
		project.markdownOptions.set(HtmlRenderer.GENERATE_HEADER_ID, true).set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
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
		if(clean) {
			pageList.forEachIndexed { index, cacheAndPost ->
				cacheAndPost.post?.let {
					it.rawContent = fileHandler.readFileAsString(cacheAndPost.post.sourceFileName) // TODO: this still contains the yaml front matter :(
					markdownRenderer.renderHTML(cacheAndPost.post, index)
					generated++
				}
			}
		} else {

			pageList.filter { item -> item.mdCacheItem.rerender }.forEachIndexed { index, cacheAndPost ->
				cacheAndPost.post?.let {
					it.rawContent = fileHandler.readFileAsString(cacheAndPost.post.sourceFileName) // TODO: this still contains the yaml front matter :(
					markdownRenderer.renderHTML(cacheAndPost.post, index)
				}
				generated++
			}
		}

		logger.info {"${generated} HTML files rendered"}
		info("${generated} HTML files rendered")

		val additionalGenerators = mutableListOf<String>()

		if (project.generators.isNullOrEmpty()) {
			additionalGenerators.addAll(DEFAULT_PROCESSORS)
		} else {
			additionalGenerators.addAll(project.generators!!)
		}

		val pluginLoader = loadPlugins(project.generators, File(project.parentFolder,"plugins"))
		val processorPipeline = ArrayList<KClass<*>>()
		val renderer by inject<TemplatePageRenderer> { parametersOf(project) }

		for (className in additionalGenerators) {
			try {
				val kClass = if (pluginLoader != null) {
					pluginLoader.loadClass(className).kotlin
				} else {
					Class.forName(className).kotlin
				}
				if (kClass.isSubclassOf(GeneratorPipeline::class)) {
					debug("Adding $kClass to the generator pipeline")
					processorPipeline.add(kClass)
				} else {
					logger.error {"Pipeline class ${kClass.simpleName} is not an instance of GeneratorPipeline!"}
					println.error("Pipeline class ${kClass.simpleName} is not an instance of GeneratorPipeline!")
				}
			} catch (cnfe: java.lang.ClassNotFoundException) {
				logger.error {"Unable to load class '$className' - is it defined in the classpath or provided in a jar? The full package name must be provided."}
				println.error("Unable to load class '$className' - is it defined in the classpath or provided in a jar? The full package name must be provided.")
			}
		}
		if (processorPipeline.size == 0) {
			logger.error {"No generators found in the pipeline. Aborting execution!"}
			error("No generators found in the pipeline. Aborting execution!")
		}

		val myArray = ArrayList<KClass<GeneratorPipeline>>()
		@Suppress("UNCHECKED_CAST")
		processorPipeline.forEach { kClass ->
			myArray.add(kClass as KClass<GeneratorPipeline>)
		}

		// TODO: this still doesn't work with the CACHE!
		getPostsFromCacheAndPost(pageList).process(myArray, project, renderer, fileHandler)


		//TODO: come up with a better asset copying pipeline stage thingy
		assetsProcessor.copyStatics()
	}

	private fun getPostsFromCacheAndPost(cacheSet: Set<CacheAndPost>): List<Post> {
		val postList = mutableListOf<Post>()
		cacheSet.forEach { if(it.post != null) postList.add(it.post)}
		return postList
	}

	private fun loadPlugins(plugins: ArrayList<String>?, pluginFolder: File): ClassLoader? {
		if (plugins != null) {
			val jars = ArrayList<URL>()
			pluginFolder.walk().forEach {
				if (it.extension.equals("jar")) {
					jars.add(it.toURI().toURL())
					logger.debug {it.toURI().toURL().toString() }
				}
			}

			for (generator in plugins) {
				return URLClassLoader.newInstance(jars.toTypedArray(), loader)
			}
		}
		return null
	}

}

/**
 * Extension function on a List of Posts. Uses co-routines to run the given array of GeneratorPipeline classes in parallel
 * @param[pipeline] Array of class names to process in parallel
 * @param[project] the bascule project model
 * @param[renderer] the renderer which converts model map into a string
 * @param[fileHandler] file handler for writing output to disc
 */
private fun List<Post>.process(pipeline: ArrayList<KClass<GeneratorPipeline>>, project: Project, renderer: TemplatePageRenderer, fileHandler: BasculeFileHandler) {

	val logger = KotlinLogging.logger { "listPost.process"}

	val processors = mutableMapOf<KClass<*>, KFunction<*>>()

	for (p in pipeline) {
		val processorFunc = p.declaredFunctions.find { it.name.equals("process") }
		if (processorFunc != null) {
			processors.put(p, processorFunc)
		}
	}

	val progress = runBlocking {
		launch {
			for (clazz in processors) {
				val func = clazz.value
				logger.debug {"Calling function ${func.name} for pipeline ${clazz.key.simpleName}"}
				debug("Calling function ${func.name} for pipeline ${clazz.key.simpleName}")
				@Suppress("UNCHECKED_CAST")
				func.callSuspend(constructPipeline(
						clazz.key as KClass<out GeneratorPipeline>,
						project,
						this@process
				), project, renderer, fileHandler, project.clean)
			}
		}
	}
	if (progress.isCompleted) {
		logger.info { "Generation complete. HTML files are stored in folder ${project.dirs.output}" }
		info("Generation complete. HTML files are stored in folder ${project.dirs.output}")
	}

}

/**
 * Construct a GeneratorPipeline object from the given class
 * @param[pipelineClazz] A class implementing the GeneratorPipeline interface
 * @param[project] the bascule project model
 * @param[posts] the list of posts in the project
 * @return An instantiated GeneratorPipeline object
 */
private fun constructPipeline(pipelineClazz: KClass<out GeneratorPipeline>, project: Project, posts: List<Post>): GeneratorPipeline {
	val primaryConstructor = pipelineClazz.constructors.first()
	val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()
	val constructorKParams: List<KParameter> = primaryConstructor.parameters
	constructorKParams.forEach { kparam ->
		when (kparam.name) {
			"posts" -> {
				constructorParams.put(kparam, posts)
			}
			"numPosts" -> {
				constructorParams.put(kparam, posts.size)
			}
			"postsPerPage" -> {
				constructorParams.put(kparam, project.postsPerPage)
			}
		}
	}
	return primaryConstructor.callBy(constructorParams)
}
