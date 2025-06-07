package org.liamjd.bascule.generator

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import println.debug
import println.info
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions

/**
 * Extension function on a List of Posts. Uses co-routines to run the given array of GeneratorPipeline classes in parallel
 * @param[pipeline] Array of class names to process in parallel
 * @param[project] the bascule project model
 * @param[renderer] the renderer which converts model map into a string
 * @param[fileHandler] file handler for writing output to disc
 */
internal fun List<Post>.process(pipeline: ArrayList<KClass<GeneratorPipeline>>, project: Project, renderer: TemplatePageRenderer, fileHandler: BasculeFileHandler) {

	val logger = KotlinLogging.logger { "listPost.process" }

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
				logger.debug { "Calling function ${func.name} for pipeline ${clazz.key.simpleName}" }
				debug("Calling function ${func.name} for pipeline ${clazz.key.simpleName}")

				@Suppress("UNCHECKED_CAST")
				val processPipeline = constructPipeline(clazz.key as KClass<out GeneratorPipeline>, project,this@process)

				try {
					@Suppress("UNCHECKED_CAST")
					func.callSuspend(processPipeline, project, renderer, fileHandler, project.clean)
				} catch ( iae: IllegalArgumentException) {
					error("Unable to construct pipeline for ${processPipeline.javaClass.name} with exception: ${iae.message}")
				}
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
