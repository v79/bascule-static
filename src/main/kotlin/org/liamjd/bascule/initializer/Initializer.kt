package org.liamjd.bascule.initializer

import org.liamjd.bascule.Constants
import org.liamjd.bascule.Constants.ASSETS_DIR
import org.liamjd.bascule.Constants.CONFIG_YAML
import org.liamjd.bascule.Constants.OUTPUT_DIR
import org.liamjd.bascule.Constants.SOURCE_DIR
import org.liamjd.bascule.Constants.TEMPLATES_DIR
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.assets.Theme
import println.info
import java.io.File
import java.nio.file.FileSystems
import kotlin.system.exitProcess

class Initializer(val siteName: String) {

	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().getSeparator()

	init {
		info("Initializing new site $currentDirectory$pathSeparator$siteName")

		val siteRoot = File("$currentDirectory$pathSeparator$siteName")
		if (siteRoot.mkdirs() == false) {
			println.error("Could not create folder $siteRoot")
			exitProcess(-1)
		}

		// copy configuration yaml file from resources
		info("Building directory structure")

		val yamlConfigString = "${siteName}.yaml"
		info("Writing $siteName.yaml configuration file")
		copyFileFromResources(fileName = CONFIG_YAML, destination = siteRoot, destFileName = yamlConfigString)
		val sourceDir = File(siteRoot.absolutePath + "/${SOURCE_DIR}")
		val outputDir = File(siteRoot.absolutePath + "/${OUTPUT_DIR}")
		val assetsDir = File(siteRoot.absolutePath + "/${ASSETS_DIR}")
		sourceDir.mkdir()
		outputDir.mkdir()
		assetsDir.mkdir()
		val templatesDir = File(siteRoot.absolutePath + "/${TEMPLATES_DIR}")
		// TODO: allow theme to be set during creation? List of themes?
		templatesDir.mkdir()

		info("Copying theme '${Constants.DEFAULT_THEME}' templates")
		copyThemeToTemplates(Constants.DEFAULT_THEME, templatesDir)

		val projectStructure = ProjectStructure(name = siteName,
				root = siteRoot,
				sourceDir = sourceDir,
				outputDir = outputDir,
				assetsDir = assetsDir,
				templatesDir = templatesDir,
				yamlConfigString = yamlConfigString,
				theme = Constants.DEFAULT_THEME)

		info("Site generated. Start writing your pages and posts inside the ${sourceDir.absolutePath} folder. Store images and CSS files in ${assetsDir.absolutePath} and Handlebars templates in ${templatesDir.absolutePath}.")

	}

	private fun copyThemeToTemplates(themeName: Theme, templatesDir: File) {
		val themeTemplateDirName = "${Constants.THEME_FOLDER}${themeName}/templates"
		val filesToCopy = arrayOf("post.html")
		for (f in filesToCopy) {
			copyFileFromResources(fileName = f, destination = templatesDir, sourceDir = themeTemplateDirName + "/")
		}
	}

	/**
	 * Reads a file from /src/main/resources/_sourceDir_ and writes it to _destination_.
	 * You can override the filename by supplying a value for _destFileName_
	 * @param[fileName] Name of the file to copy
	 * @param[destination] Folder the file is to be copied to
	 * @param[destFileName] The final name of the file once copied. If left out, the destination file will have the same name as the source
	 * @param[sourceDir] the folder within /src/main/resources to copy from. Optional.
	 */
	private fun copyFileFromResources(fileName: String, destination: File, destFileName: String? = null, sourceDir: String = "") {
		val data = this.javaClass.getResource(sourceDir + fileName).readText()
		val finalFileName = if (destFileName == null) fileName else destFileName

		File(destination, finalFileName).bufferedWriter().use { out ->
			out.write(data)
		}
	}
}


/**
 * Really, really destructive and I'm only using it for testing purposes!
 */
class Destroyer(val siteName: String) {
	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().getSeparator()

	init {
		if (siteName.isNotBlank()) {
			println("Destroying your website $currentDirectory$pathSeparator$siteName!")

			val siteRoot = File("$currentDirectory$pathSeparator$siteName")
			siteRoot.deleteRecursively()
		}
		exitProcess(-1)
	}
}