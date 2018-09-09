package org.liamjd.bascule.generator

import org.liamjd.bascule.Constants
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.random
import org.liamjd.bascule.scanner.FolderWalker
import picocli.CommandLine
import println.info
import java.io.File
import java.nio.file.FileSystems

@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable {

	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().separator
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

		val configStream = File(parentFolder.absolutePath,yamlConfig).inputStream()
		val projectStructure = ProjectStructure.Configurator.buildProjectFromYamlConfig(configStream)

		println(projectStructure)

		val walker = FolderWalker(projectStructure)

		walker.generate()
	}

}