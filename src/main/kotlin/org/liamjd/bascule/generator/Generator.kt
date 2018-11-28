package org.liamjd.bascule.generator

import org.koin.core.parameter.ParameterList
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.Constants
import org.liamjd.bascule.FileHandler
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.pipeline.GeneratorPipeline
import org.liamjd.bascule.pipeline.IndexPageGenerator
import org.liamjd.bascule.pipeline.PostNavigationGenerator
import org.liamjd.bascule.pipeline.TaxonomyNavigationGenerator
import org.liamjd.bascule.random
import org.liamjd.bascule.render.Renderer
import org.liamjd.bascule.scanner.FolderWalker
import picocli.CommandLine
import println.info
import java.io.File
import java.io.FileInputStream
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

/**
 * Starts the post and page generation process. Must be run from inside the project folder
 */
@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable, KoinComponent {

	private val fileHandler: FileHandler by inject(parameters = { ParameterList() })
	private val renderer by inject<Renderer> { parametersOf(project) }

	private val currentDirectory = System.getProperty("user.dir")!!
	private val yamlConfig: String
	private val parentFolder: File
	private val configStream: FileInputStream
	private val project: ProjectStructure

	private val OUTPUT_SUFFIX = ".html"

	init {
		parentFolder = File(currentDirectory)
		yamlConfig = "${parentFolder.name}.yaml"

		configStream = File(parentFolder.absolutePath, yamlConfig).inputStream()
		project = ProjectStructure.Configurator.buildProjectFromYamlConfig(configStream)
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
		val numPosts = sortedPosts.size

		// TODO: is this a good idea?
		// TODO: 
		sortedPosts.process(arrayOf(IndexPageGenerator::class, PostNavigationGenerator::class, TaxonomyNavigationGenerator::class), project, renderer, fileHandler)

	/*	val indexPageGenerator = IndexPageGenerator(sortedPosts, numPosts, 2)
		indexPageGenerator.process(project, renderer, fileHandler);

		val postListGenerator = PostNavigationGenerator(sortedPosts, numPosts, project.postsPerPage)
		postListGenerator.process(project, renderer, fileHandler)

		val taxonomyNavigationGenerator = TaxonomyNavigationGenerator(sortedPosts, numPosts, project.postsPerPage)
		taxonomyNavigationGenerator.process(project, renderer, fileHandler)*/

	}

}

fun List<Post>.process(pipeline: Array<KClass<out GeneratorPipeline>>, project: ProjectStructure, renderer: Renderer, fileHandler: FileHandler) {
	for (p in pipeline) {
		println("----- initiating pipeline ${p.simpleName}")

		val primaryConstructor = p.constructors.first()
		val constructorParams: MutableMap<KParameter, Any?> = mutableMapOf()
		val constructorKParams: List<KParameter> = primaryConstructor.parameters
		constructorKParams.forEach { kparam ->
			when (kparam.name) {
				"posts" -> {
					constructorParams.put(kparam, this)
				}
				"numPosts" -> {
					constructorParams.put(kparam, this.size)
				}
				"postsPerPage" -> {
					constructorParams.put(kparam, project.postsPerPage)
				}
			}
		}

		val processor = primaryConstructor.callBy(constructorParams)

		processor.process(project, renderer, fileHandler)
	}
}
