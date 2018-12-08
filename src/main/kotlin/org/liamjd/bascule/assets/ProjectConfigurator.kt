package org.liamjd.bascule.assets

import org.liamjd.bascule.Constants
import org.liamjd.bascule.lib.model.Project
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStream

/**
 * Builds the project structure by parsing the root yaml file
 */
// TODO: this should be in the library, not here...
object Configurator {

	val DEFAULT_PROCESSORS = arrayOf("IndexPageGenerator","PostNavigationGenerator","TaxonomyNavigationGenerator")

	fun buildProjectFromYamlConfig(configStream: InputStream): Project {
		return buildProjectFromYamlConfig(configStream.bufferedReader().readText())
	}

	private fun buildProjectFromYamlConfig(yamlConfigString: String): Project {
		val yaml = Yaml()
		val configMap: Map<String, Any> = yaml.load(yamlConfigString)

		val parentFolder = File(System.getProperty("user.dir"))
		val sourceDir: String
		val assetsDir: String
		val outputDir: String
		val templatesDir: String
		val plugins: Array<String>
		val processors: Array<String>

		val theme = if (configMap["theme"] == null) Constants.DEFAULT_THEME else configMap["theme"] as String

		if (configMap["directories"] != null) {
			@Suppress("UNCHECKED_CAST")
			val directories = configMap["directories"] as Map<String, String?>
			sourceDir = directories["source"] ?: Constants.SOURCE_DIR
			assetsDir = directories["assets"] ?: Constants.ASSETS_DIR
			outputDir = directories["output"] ?: Constants.OUTPUT_DIR
			templatesDir = "$theme/" + (directories["templates"] ?: Constants.TEMPLATES_DIR)
		} else {
			sourceDir = Constants.SOURCE_DIR
			assetsDir = Constants.ASSETS_DIR
			outputDir = Constants.OUTPUT_DIR
			templatesDir = "$theme/" + Constants.TEMPLATES_DIR
		}

		if(configMap["processors"] != null) {
//			processors = arrayOf(configMap["processors"] as String)
		}

		val project = Project(
				name = parentFolder.name,
				root = parentFolder,
				source = sourceDir,
				output = outputDir,
				assets = assetsDir,
				templates = templatesDir,
				yaml = yamlConfigString,
				themeName = theme,
				configMap = configMap
		)

		val postsPerPage: Any? = configMap["postsPerPage"]
		if (postsPerPage != null && (postsPerPage as Int > 0)) {
			project.postsPerPage = configMap["postsPerPage"] as Int
		}

		project.yamlMap = configMap

		return project
	}
}

