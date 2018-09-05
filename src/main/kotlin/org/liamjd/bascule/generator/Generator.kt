package org.liamjd.bascule.generator

import org.liamjd.bascule.Constants
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.random
import org.liamjd.bascule.scanner.FolderWalker
import org.yaml.snakeyaml.Yaml
import picocli.CommandLine
import println.info
import java.io.File
import java.nio.file.FileSystems

@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable {

	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().getSeparator()
	val yamlConfig: String
	val parentFolder: File

	init  {
		parentFolder = File(currentDirectory)
		yamlConfig = "${parentFolder.name}.yaml"
	}

	override fun run() {
		info(Constants.logos[(0 until Constants.logos.size).random()])
		info("Generating your website")
		info("Reading yaml configuration file $yamlConfig")

		val yaml = Yaml()
		val configStream = File(parentFolder.absolutePath,yamlConfig).inputStream()
		val configMap: Map<String,Any> = yaml.load(configStream)
		println(configMap)

		val projectStructure = buildProjectFromYamlConfig(yamlConfig, configMap)
		val walker = FolderWalker(projectStructure)

		walker.generate()

	}

	private fun buildProjectFromYamlConfig(yaml: String, configMap: Map<String,Any>): ProjectStructure {
		val sourceDir: String
		val assetsDir: String
		val outputDir: String
		val templatesDir: String

		val theme = if(configMap["theme"] == null) Constants.DEFAULT_THEME else configMap["theme"] as String

		if(configMap["directories"]!= null) {
			val directories = configMap["directories"] as Map<String,String?>
			sourceDir  = directories["source"] ?: Constants.SOURCE_DIR
			assetsDir  = directories["assets"] ?: Constants.ASSETS_DIR
			outputDir  = directories["output"] ?: Constants.OUTPUT_DIR
			templatesDir = directories["templates"] ?: Constants.TEMPLATES_DIR
		} else {
			sourceDir  = Constants.SOURCE_DIR
			assetsDir  = Constants.ASSETS_DIR
			outputDir  = Constants.OUTPUT_DIR
			templatesDir =  Constants.TEMPLATES_DIR
		}

		return ProjectStructure(name = parentFolder.name,
				root = parentFolder,
				source = sourceDir,
				output = outputDir,
				assets = assetsDir,
				templates = templatesDir,
				yaml = yamlConfig,
				themeName = theme)

	}
}