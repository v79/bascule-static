package org.liamjd.bascule.initializer

import org.liamjd.bascule.Constants.ASSETS_DIR
import org.liamjd.bascule.Constants.CONFIG_YAML
import org.liamjd.bascule.Constants.OUTPUT_DIR
import org.liamjd.bascule.Constants.SOURCE_DIR
import org.liamjd.bascule.Constants.TEMPLATES_DIR
import java.io.File
import java.nio.file.FileSystems
import kotlin.system.exitProcess

class Initializer(val siteName: String) {

	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().getSeparator()

	init {
		println("Initializing new site $currentDirectory$pathSeparator$siteName")

		val siteRoot = File("$currentDirectory$pathSeparator$siteName")
		if(siteRoot.mkdirs()== false) {
			println("Could not create folder $siteRoot")
			exitProcess(-1)
		}


		// copy configuration yaml file from resources
		println("Writing $siteName.yaml configuration file")
		val yamlConfigString = this.javaClass.getResource(CONFIG_YAML).readText()
		File(siteRoot,"$siteName.yaml").bufferedWriter().use { out ->
			out.write(yamlConfigString)
		}
		println("Building directory structure")
		val sourceDir = File(siteRoot.absolutePath + "/${SOURCE_DIR}")
		val outputDir = File(siteRoot.absolutePath + "/${OUTPUT_DIR}")
		val assetsDir = File(siteRoot.absolutePath + "/${ASSETS_DIR}")
		val templatesDir = File(siteRoot.absolutePath + "/${TEMPLATES_DIR}")
		sourceDir.mkdir()
		outputDir.mkdir()
		assetsDir.mkdir()
		templatesDir.mkdir()

		println("Site generated. Start writing your pages and posts inside the ${sourceDir.absolutePath} folder. Store images and CSS files in ${assetsDir.absolutePath} and Handlebars templates in ${templatesDir.absolutePath}.")

	}
}


/**
 * Really, really destructive and I'm only using it for testing purposes!
 */
class Destroyer(val siteName: String) {
	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().getSeparator()

	init {
		if(siteName.isNotBlank()) {
			println("Destroying your website $currentDirectory$pathSeparator$siteName!")

			val siteRoot = File("$currentDirectory$pathSeparator$siteName")
			siteRoot.deleteRecursively()
		}
		exitProcess(-1)
	}
}