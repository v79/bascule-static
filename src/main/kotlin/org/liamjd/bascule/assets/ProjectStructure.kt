package org.liamjd.bascule.assets

import org.liamjd.bascule.Constants
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStream

typealias Theme = String

// TODO: probably too much in this constructor... allow some vars!
// TODO: there's stuff in the model which shouldn't be there, like the directory names
/**
 * Class representing the overall structure of the project, mostly the directory locations for source files, templates, etc
 */
class ProjectStructure(val name: String, val root: File, val sourceDir: File, val outputDir: File, val assetsDir: File, val templatesDir: File, val yamlConfigString: String, val theme: Theme, val model: Map<String, Any>) {

	var postsPerPage = 5
	var yamlMap: Map<String,Any> = mutableMapOf()

	constructor(name: String, root: File, source: String, output: String, assets: String, templates: String, yaml: String, themeName: String, configMap: Map<String, Any>) : this(name = name,
			root = root,
			sourceDir = File(root, source),
			outputDir = File(root, output),
			assetsDir = File(root, assets),
			templatesDir = File(root, templates),
			yamlConfigString = yaml,
			theme = themeName,
			model = configMap)

	constructor(name: String, root: File, sourceDir: File, outputDir: File, assetsDir: File, templatesDir: File, yamlConfigString: String, theme: Theme) : this(name = name,
			root = root,
			sourceDir = sourceDir,
			outputDir = outputDir,
			assetsDir = assetsDir,
			templatesDir = templatesDir,
			yamlConfigString = yamlConfigString,
			theme = theme,
			model = mapOf())


	override fun toString(): String {
		return "ProjectStructure: name: $name, root: $root\n" +
				"source: $sourceDir, output: $outputDir, assets: $assetsDir, templates: $templatesDir\n" +
				"theme: $theme\n" +
				"model: $model"

	}

	/**
	 * Builds the project structure by parsing the root yaml file
	 */
	object Configurator {

		fun buildProjectFromYamlConfig(configStream: InputStream) : ProjectStructure {
			return buildProjectFromYamlConfig(configStream.bufferedReader().readText())
		}

		fun buildProjectFromYamlConfig(yamlConfigString: String): ProjectStructure {
			val yaml = Yaml()
			val configMap: Map<String, Any> = yaml.load(yamlConfigString)

			val parentFolder = File(System.getProperty("user.dir"))
			val sourceDir: String
			val assetsDir: String
			val outputDir: String
			val templatesDir: String

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

			val project =  ProjectStructure(
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
			if(postsPerPage != null && (postsPerPage as Int > 0)) {
				project.postsPerPage = configMap["postsPerPage"] as Int
			}

			project.yamlMap = configMap

			return project
		}
	}

}

