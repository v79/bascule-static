package org.liamjd.bascule.generator

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.parameter.ParameterList
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.Constants
import org.liamjd.bascule.assets.Configurator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.Renderer
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.random
import org.liamjd.bascule.scanner.FolderWalker
import picocli.CommandLine
import println.debug
import println.info
import java.io.File
import java.io.FileInputStream
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.system.measureTimeMillis

/**
 * Starts the post and page generation process. Must be run from inside the project folder
 */
@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable, KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })
	private val renderer by inject<Renderer> { parametersOf(project) }

	private val currentDirectory = System.getProperty("user.dir")!!
	private val yamlConfig: String
	private val parentFolder: File
	private val configStream: FileInputStream
	private val project: Project

	private val OUTPUT_SUFFIX = ".html"

	init {
		parentFolder = File(currentDirectory)
		yamlConfig = "${parentFolder.name}.yaml"

		configStream = File(parentFolder.absolutePath, yamlConfig).inputStream()
		project = Configurator.buildProjectFromYamlConfig(configStream)
	}

	override fun run() {

		info(Constants.logos[(0 until Constants.logos.size).random()])
		info("Generating your website")
		info("Reading yaml configuration file $yamlConfig")

		// TODO: be less aggressive with this, use some sort of caching :)
		fileHandler.emptyFolder(project.outputDir, OUTPUT_SUFFIX)
		fileHandler.emptyFolder(File(project.outputDir, "tags"))
		val walker = FolderWalker(project)

		val postList = walker.generate()
		val sortedPosts = postList.sortedByDescending { it.date }

		// TODO: is this a good idea?
		// TODO: how to move the creation of sortedPosts into this? Rewrite folder walker?
		// TODO: load the list of processors from configuration, such as config.yaml

		// OK, let's just hard-code a string first
		val DEFAULT_PROCESSORS = arrayOf("IndexPageGenerator", "PostNavigationGenerator", "TaxonomyNavigationGenerator")
		val PIPELINE_PACKAGE = "org.liamjd.bascule.pipeline"

		val processorPipeline =  ArrayList<KClass<*>>()
		for (className in DEFAULT_PROCESSORS) {
			val kClass = Class.forName("$PIPELINE_PACKAGE.$className").kotlin
			println(kClass.supertypes)
			if (kClass.isSubclassOf(GeneratorPipeline::class)) {
				println("adding $kClass")
				processorPipeline.add(kClass)
			} else {
				println.error("Pipeline class ${kClass.simpleName} is not an instance of GeneratorPipeline!")
			}
		}
		if (processorPipeline.size == 0) {
			error("No generators found in the pipeline. Aborting execution!")
		}
		println("Forcing SinglePagePDFGenerator into the pipeline... is it even loaded?")
		processorPipeline.add(Class.forName("org.liamjd.bascule.lib.generators.pdf.SinglePagePDFGenerator").kotlin)

		val myArray = ArrayList<KClass<GeneratorPipeline>>()
		processorPipeline.forEachIndexed { index, kClass ->
			println("$index -> $kClass")
			myArray.add(kClass as KClass<GeneratorPipeline>)
		}

		sortedPosts.process(myArray, project, renderer, fileHandler)

//		sortedPosts.process(arrayOf(IndexPageGenerator::class, PostNavigationGenerator::class, TaxonomyNavigationGenerator::class), project, renderer, fileHandler)

	}

}

/**
 * Extension function on a List of Posts. Uses co-routines to run the given array of GeneratorPipeline classes in parallel
 * @param[pipeline] Array of class names to process in parallel
 * @param[project] the bascule project model
 * @param[renderer] the renderer which converts model map into a string
 * @param[fileHandler] file handler for writing output to disc
 */
private fun List<BasculePost>.process(pipeline: ArrayList<KClass<GeneratorPipeline>>, project: Project, renderer: Renderer, fileHandler: BasculeFileHandler) {

	val processors = mutableMapOf<KClass<*>, KFunction<*>>()

	for (p in pipeline) {
		debug("----- expanding pipeline ${p.simpleName}")
		val processorFunc = p.declaredFunctions.find { it.name.equals("process") }
		if (processorFunc != null) {
			processors.put(p, processorFunc)
		}
	}
	val timeTaken = measureTimeMillis {
		runBlocking {
			launch {
				for (clazz in processors) {
					val func = clazz.value
					debug("Calling function ${func.name} for pipeline ${clazz.key.simpleName}")
					@Suppress("UNCHECKED_CAST")
					func.callSuspend(constructPipeline(
							clazz.key as KClass<out GeneratorPipeline>,
							project,
							this@process
					), project, renderer, fileHandler)
				}
			}
		}

	}

	println.error("Time taken: $timeTaken ms")
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
