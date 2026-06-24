package org.liamjd.bascule.plugins

import com.vladsch.flexmark.util.misc.Extension
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Base class for resolving plugin classes from the application classpath and from JAR files dropped
 * into the project's `plugins/` folder.
 *
 * A plugin is any class — identified by its fully qualified name — that implements a particular
 * interface, for example, a Flexmark [Extension] (see [HandlebarPluginLoader]) or a
 * [GeneratorPipeline] (see [GeneratorPluginLoader]). Subclasses turn those names into Kotlin [KClass]
 * references, discarding any class that does not implement the expected interface.
 *
 * JARs found under `plugins/` are wrapped in a [URLClassLoader] that delegates to [classLoader], so
 * both bundled (classpath) plugins and externally supplied (JAR) plugins can be resolved through a
 * single loader.
 *
 * Extend this class once per kind of plugin you want to load.
 *
 * @property classLoader parent class loader used to resolve classpath plugins and as the parent of
 * the JAR [URLClassLoader]
 * @property pluginFolder the project root folder; JARs are read from its [PLUGIN_FOLDER] subfolder
 */
abstract class PluginLoader(var classLoader: ClassLoader, var pluginFolder: File) {
    private val JAR = "jar"

    /** Name of the subfolder, relative to [pluginFolder], that is scanned for plugin JARs. */
    val PLUGIN_FOLDER = "plugins"

    /** Accumulated URLs of every JAR discovered by [addJars]. Grows across calls; never cleared. */
    val jars = ArrayList<URL>()

    /**
     * Build a class loader capable of resolving the supplied plugin [classNames].
     *
     * Every JAR under the [PLUGIN_FOLDER] subfolder of [pluginFolder] is collected (see [addJars])
     * and wrapped in a [URLClassLoader] parented to [classLoader]. Returns `null` when [classNames]
     * is `null` or empty — in that case callers fall back to resolving names directly against the
     * application classpath via [Class.forName].
     *
     * Note that the JARs gathered cover the whole `plugins/` folder, not just the classes named in
     * [classNames]; the names are only used to decide whether any loader is needed at all.
     *
     * @param classNames fully qualified plugin class names that need to be resolvable or `null`
     * @return a [URLClassLoader] spanning the project's plugin JARs, or `null` if there is nothing to load
     */
    fun loadPlugins(classNames: List<String>?): ClassLoader? {
        if (classNames != null) {
            addJars(File(pluginFolder, PLUGIN_FOLDER))
            for (generator in classNames) {
                return URLClassLoader.newInstance(jars.toTypedArray(), classLoader)
            }
        }
        return null
    }

    /**
     * Recursively walk [folder] and append every `.jar` file found to [jars] as a [URL]. Non-JAR
     * files are ignored, and a folder that does not exist simply contributes nothing.
     *
     * @param folder the directory tree to scan for JAR files
     */
    fun addJars(folder: File) {
        folder.walk().forEach {
            if (it.extension == JAR) {
                jars.add(it.toURI().toURL())
            }
        }
    }
}

/**
 * Loads Flexmark [Extension] plugins (the Markdown/Handlebars extensions configured under
 * `extensions:` in the project YAML) by fully qualified class name.
 *
 * @param classLoader parent class loader used to resolve classpath extensions and JAR extensions
 * @property requiredInterface interface that every extension must implement, normally [Extension];
 * classes that do not implement it are logged and skipped
 * @param parentFolder project root whose [PLUGIN_FOLDER] subfolder is searched for plugin JARs
 */
class HandlebarPluginLoader(classLoader: ClassLoader, val requiredInterface: KClass<out Any>, parentFolder: File) :
    PluginLoader(classLoader = classLoader, pluginFolder = parentFolder) {
    /**
     * Resolve the given [extensions] class names to [Extension] [KClass] references.
     *
     * Each name is loaded from the project's plugin JARs when one is present, otherwise from the
     * application classpath. Any name that cannot be found ([ClassNotFoundException]) or whose class
     * does not implement [requiredInterface] is logged and omitted from the result rather than
     * failing the whole load, so one bad entry never blocks the others.
     *
     * @param extensions fully qualified class names of the extensions to load
     * @return the successfully resolved extension classes, excluding any that could not be loaded or
     * were of the wrong type
     */
    fun getExtensions(extensions: java.util.ArrayList<String>): List<KClass<Extension>> {

        val pluginClassLoader = loadPlugins(extensions)
        val extensionList = ArrayList<KClass<Extension>>()

        if (extensions != null) {
            for (className in extensions) {
                try {
                    val kClass = if (pluginClassLoader != null) {
                        pluginClassLoader.loadClass(className).kotlin
                    } else {
                        Class.forName(className).kotlin
                    }

                    // confirm that we have the right type
                    if (kClass.isSubclassOf(requiredInterface)) {
                        println("Adding handlebars extension $className")
                        @Suppress("UNCHECKED_CAST")
                        extensionList.add(kClass as KClass<Extension>)
                    } else {
                        println.error("Extension class ${kClass.simpleName} is not an instance of Extension!")
                    }
                } catch (cnfe: ClassNotFoundException) {
                    println.error("Unable to load class '$className' - is it defined in the classpath or provided in a jar? The full package name must be provided.")
                }
            }
        }
        return extensionList
    }

}

/**
 * Loads [GeneratorPipeline] plugins (the pipeline stages configured under `generators:` in the
 * project YAML) by fully qualified class name.
 *
 * @param classLoader parent class loader used to resolve classpath generators and JAR generators
 * @property requiredInterface interface that every generator must implement, normally
 * [GeneratorPipeline]; classes that do not implement it are logged and skipped
 * @param parentFolder project root whose [PLUGIN_FOLDER] subfolder is searched for plugin JARs
 */
class GeneratorPluginLoader(classLoader: ClassLoader, val requiredInterface: KClass<out Any>, parentFolder: File) :
    PluginLoader(classLoader = classLoader, pluginFolder = parentFolder) {

    /**
     * Resolve the given [generatorNames] class names to [GeneratorPipeline] [KClass] references.
     *
     * Each name is loaded from the project's plugin JARs when one is present, otherwise from the
     * application classpath. Any name that cannot be found ([ClassNotFoundException]) or whose class
     * does not implement [requiredInterface] is logged and omitted from the result rather than
     * failing the whole load. A `null` [generatorNames] yields an empty list.
     *
     * @param generatorNames fully qualified class names of the generators to load, or `null`
     * @return the successfully resolved generator classes, excluding any that could not be loaded or
     * were of the wrong type
     */
    fun getGenerators(generatorNames: List<String>?): ArrayList<KClass<GeneratorPipeline>> {
        val generatorList = ArrayList<KClass<GeneratorPipeline>>()

        val pluginClassLoader: ClassLoader? = loadPlugins(generatorNames)

        if (generatorNames != null) {
            for (className in generatorNames) {
                try {
                    val kClass = if (pluginClassLoader != null) {
                        pluginClassLoader.loadClass(className).kotlin
                    } else {
                        Class.forName(className).kotlin
                    }

                    // confirm that we have the right type
                    if (kClass.isSubclassOf(requiredInterface)) {
                        @Suppress("UNCHECKED_CAST")
                        generatorList.add(kClass as KClass<GeneratorPipeline>)
                    } else {
                        println.error("Pipeline class ${kClass.simpleName} is not an instance of GeneratorPipeline!")
                    }
                } catch (cnfe: ClassNotFoundException) {
                    println.error("Unable to load class '$className' - is it defined in the classpath or provided in a jar? The full package name must be provided.")
                }

            }
        }

        return generatorList
    }

}
