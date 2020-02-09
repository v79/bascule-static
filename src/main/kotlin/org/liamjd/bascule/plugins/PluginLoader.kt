package org.liamjd.bascule.plugins

import org.liamjd.bascule.lib.generators.GeneratorPipeline
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Base class for loading plugins from the Classpath and the project plugins folder.
 * Extend this class for each type of class you wish you add.
 */
abstract class PluginLoader(var classLoader: ClassLoader) {
	private val JAR = "jar"
	val jars = ArrayList<URL>()

	fun loadPlugins(classNames: List<String>?, pluginFolder: File): ClassLoader? {
		if (classNames != null) {
			addJars(pluginFolder)
			for (generator in classNames) {
				return URLClassLoader.newInstance(jars.toTypedArray(), classLoader)
			}
		}
		return null
	}

	fun addJars(folder: File) {
		folder.walk().forEach {
			if (it.extension.equals(JAR)) {
				jars.add(it.toURI().toURL())
			}
		}
	}
}

class GeneratorPluginLoader(classLoader: ClassLoader, val requiredInterface: KClass<out Any>) : PluginLoader(classLoader = classLoader) {
	val PLUGIN_FOLDER = "plugins"

	fun getGenerators(generatorNames: List<String>?, parent: File) : ArrayList<KClass<GeneratorPipeline>> {
		val generatorList = ArrayList<KClass<GeneratorPipeline>>()

		val pluginClassLoader: ClassLoader? = loadPlugins(generatorNames, File(parent, PLUGIN_FOLDER))

		if(generatorNames != null) {
			for (className in generatorNames) {
				try {
					val kClass = if (pluginClassLoader != null) {
						pluginClassLoader.loadClass(className).kotlin
					} else {
						Class.forName(className).kotlin
					}

					// confirm that we have the right type
					if (kClass.isSubclassOf(requiredInterface)) {
						println("Adding $kClass to the generator pipeline")
						@Suppress("UNCHECKED_CAST")
						generatorList.add(kClass as KClass<GeneratorPipeline>)
					} else {
//						logger.error {"Pipeline class ${kClass.simpleName} is not an instance of GeneratorPipeline!"}
						println.error("Pipeline class ${kClass.simpleName} is not an instance of GeneratorPipeline!")
					}
				} catch (cnfe: java.lang.ClassNotFoundException) {
//						logger.error {"Unable to load class '$className' - is it defined in the classpath or provided in a jar? The full package name must be provided."}
						println.error("Unable to load class '$className' - is it defined in the classpath or provided in a jar? The full package name must be provided.")
					}

				}
			}

		return generatorList
	}

}
