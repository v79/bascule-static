package org.liamjd.bascule.initializer

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
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

class Initializer(val siteName: String, val themeName: Theme?) {

	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().getSeparator()

	init {
		info("Initializing new site $currentDirectory$pathSeparator$siteName")

		val siteRoot = File("$currentDirectory$pathSeparator$siteName")
		val theme = themeName ?: Constants.DEFAULT_THEME
		if (siteRoot.mkdirs() == false) {
			println.error("Could not create folder $siteRoot")
			exitProcess(-1)
		}

		// copy configuration yaml file from resources
		val yamlConfigString = buildConfiguration(theme, siteRoot)
		info("Building directory structure")


//		copyFileFromResources(fileName = CONFIG_YAML, destination = siteRoot, destFileName = yamlConfigString)
		val sourceDir = File(siteRoot.absolutePath + "/${SOURCE_DIR}")
		val outputDir = File(siteRoot.absolutePath + "/${OUTPUT_DIR}")
		val assetsDir = File(siteRoot.absolutePath + "/${ASSETS_DIR}")
		sourceDir.mkdir()
		outputDir.mkdir()
		assetsDir.mkdir()
		val templatesDir = File(siteRoot.absolutePath + "/${TEMPLATES_DIR}")
		// TODO: allow theme to be set during creation? List of themes?
		templatesDir.mkdir()

		info("Copying theme '${theme}' templates")
		copyThemeToTemplates(theme, templatesDir)

		val projectStructure = ProjectStructure(name = siteName,
				root = siteRoot,
				sourceDir = sourceDir,
				outputDir = outputDir,
				assetsDir = assetsDir,
				templatesDir = templatesDir,
				yamlConfigString = yamlConfigString,
				theme = theme)

		info("Site generated. Start writing your pages and posts inside the ${sourceDir.absolutePath} folder. Store images and CSS files in ${assetsDir.absolutePath} and Handlebars templates in ${templatesDir.absolutePath}.")

	}

	// TODO: this will get much more complicated in the future
	private fun buildConfiguration(themeName: Theme, root: File): String {
		val yamlConfigString = "${siteName}.yaml"
		info("Writing $siteName.yaml configuration file")
		val yamlTemplate = FileHandler.readFileFromResources("", CONFIG_YAML)
		val model = mutableMapOf<String, String>()
		model.put("themeName", themeName)
		val projectConfig = render(model, yamlTemplate)

		FileHandler.writeFile(root, yamlConfigString, projectConfig)

		return yamlConfigString

	}

	private fun render(model: Map<String, Any>, templateString: String): String {
		val hbRenderer = Handlebars()
		val hbContext = Context.newBuilder(model).build()
		val template = hbRenderer.compileInline(templateString)

		return template.apply(hbContext)
	}

	private fun copyThemeToTemplates(themeName: Theme, templatesDir: File) {
		val themeTemplateDirName = "${Constants.THEME_FOLDER}${themeName}/templates"
		val filesToCopy = arrayOf("post.html")
		for (f in filesToCopy) {
			FileHandler.copyFileFromResources(fileName = f, destination = templatesDir, sourceDir = themeTemplateDirName + "/")
		}
	}

	object FileHandler {
		/**
		 * Reads a file from /src/main/resources/_sourceDir_ and writes it to _destination_.
		 * You can override the filename by supplying a value for _destFileName_
		 * @param[fileName] Name of the file to copy
		 * @param[destination] Folder the file is to be copied to
		 * @param[destFileName] The final name of the file once copied. If left out, the destination file will have the same name as the source
		 * @param[sourceDir] the folder within /src/main/resources to copy from. Optional.
		 */
		internal fun copyFileFromResources(fileName: String, destination: File, destFileName: String? = null, sourceDir: String = "") {
			val data = readFileFromResources(sourceDir, fileName)
			val finalFileName = if (destFileName == null) fileName else destFileName

			writeFile(destination, finalFileName, data)
		}

		/**
		 * Write the specified data to the file finalFileName in the directory destination
		 * @param[data] The string to write
		 * @param[finalFileName] The file name
		 * @param[destination] The destination directory
		 */
		internal fun writeFile(destination: File, finalFileName: String, data: String) {
			File(destination, finalFileName).bufferedWriter().use { out ->
				out.write(data)
			}
		}

		internal fun readFileFromResources(sourceDir: String, fileName: String): String {
			val data = this.javaClass.getResource(sourceDir + fileName).readText()
			return data
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