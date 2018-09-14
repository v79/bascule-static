package org.liamjd.bascule.generator

import org.liamjd.bascule.Constants
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.random
import org.liamjd.bascule.scanner.FolderWalker
import picocli.CommandLine
import println.info
import java.io.File
import java.nio.file.FileSystems

/**
 * Starts the post and page generation process. Must be run from inside the project folder
 */
@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable {

	private val currentDirectory = System.getProperty("user.dir")!!
	private val pathSeparator = FileSystems.getDefault().separator
	private val yamlConfig: String
	private val parentFolder: File

	init  {
		parentFolder = File(currentDirectory)
		yamlConfig = "${parentFolder.name}.yaml"
	}

	override fun run() {
		info(Constants.logos[(0 until Constants.logos.size).random()])
		info("Generating your website")
		info("Reading yaml configuration file $yamlConfig")

		val configStream = File(parentFolder.absolutePath,yamlConfig).inputStream()
		val projectStructure = ProjectStructure.Configurator.buildProjectFromYamlConfig(configStream)

		println(projectStructure)

		val walker = FolderWalker(projectStructure)

		walker.generate()
	}

}