package org.liamjd.bascule.generator

import org.liamjd.bascule.Constants
import org.liamjd.bascule.scanner.FolderWalker
import org.yaml.snakeyaml.Yaml
import picocli.CommandLine
import java.io.File
import java.nio.file.FileSystems


@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable {

	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().getSeparator()
	val yamlConfig: String
	val parentFolder: File

	init  {
		println("Time to generate...")
		parentFolder = File(currentDirectory)
		yamlConfig = "${parentFolder.name}.yaml"
	}

	override fun run() {
		println("Generating your website")
		println("Reading yaml configuration file $yamlConfig")

		val yaml = Yaml()
		val configStream = File(parentFolder.absolutePath,yamlConfig).inputStream()
		val configMap: Map<String,Any> = yaml.load(configStream)
		println(configMap)

		val sourceDir: String
		val assetsDir: String
		val outputDir: String
		val templatesDir: String

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

		val walker = FolderWalker(parentFolder, sources = sourceDir, assets = assetsDir, templates = templatesDir, output = outputDir)

		walker.generate()

	}
}