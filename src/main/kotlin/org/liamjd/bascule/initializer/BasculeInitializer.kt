package org.liamjd.bascule.initializer

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import org.koin.standalone.KoinComponent
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.Constants
import org.liamjd.bascule.Constants.ASSETS_DIR
import org.liamjd.bascule.Constants.CONFIG_YAML
import org.liamjd.bascule.Constants.OUTPUT_DIR
import org.liamjd.bascule.Constants.SOURCE_DIR
import org.liamjd.bascule.Constants.TEMPLATES_DIR
import org.liamjd.bascule.lib.model.Theme
import println.info
import java.io.File
import java.nio.file.FileSystems
import kotlin.system.exitProcess

/**
 * Sets up a new site with the given name and theme.
 */
class BasculeInitializer(val siteName: String, val themeName: Theme?, val fileHandler: BasculeFileHandler) : Initializer, KoinComponent {

	val currentDirectory = System.getProperty("user.dir")!!

	override fun create() {
		info("Initializing new site $currentDirectory/$siteName")

		val siteRoot = File("$currentDirectory/$siteName")
		val theme = themeName ?: Constants.DEFAULT_THEME
		if(!fileHandler.createDirectories(siteRoot)) {
			println.error("Could not create folder $siteRoot")
			return
		}

		// copy configuration yaml file from resources
		val yamlConfigString = buildConfiguration(theme, siteRoot)
		info("Building directory structure")


		val sourceDir = fileHandler.createDirectory(siteRoot.absolutePath,SOURCE_DIR)
		val outputDir = fileHandler.createDirectory(siteRoot.absolutePath,OUTPUT_DIR)
		val assetsDir = fileHandler.createDirectory(siteRoot.absolutePath,ASSETS_DIR)
		val templatesDir = fileHandler.createDirectories("${siteRoot.absolutePath}/$theme",TEMPLATES_DIR)

		info("Copying theme '$theme' templates")
		copyThemeToTemplates(theme, templatesDir)

		info("Site generated. Start writing your pages and posts inside the ${sourceDir.absolutePath} folder. Store images and CSS files in ${assetsDir.absolutePath} and Handlebars templates in ${templatesDir.absolutePath}.")

	}

	// TODO: this will get much more complicated in the future
	private fun buildConfiguration(themeName: Theme, root: File): String {
		val yamlConfigString = "$siteName.yaml"

		info("Writing $siteName.yaml configuration file")
		val yamlTemplate = fileHandler.readFileFromResources("", CONFIG_YAML)
		val model = mutableMapOf<String, String>()
		model.put("themeName", themeName)

		val projectConfig = render(model, yamlTemplate)

		fileHandler.writeFile(root, yamlConfigString, projectConfig)

		return yamlConfigString

	}

	private fun render(model: Map<String, Any>, templateString: String): String {
		val hbRenderer = Handlebars()
		val hbContext = Context.newBuilder(model).build()
		val template = hbRenderer.compileInline(templateString)

		return template.apply(hbContext)
	}

	private fun copyThemeToTemplates(themeName: Theme, templatesDir: File) {
		val themeTemplateDirName = "${Constants.THEME_FOLDER}/$themeName/templates"
		val filesToCopy = arrayOf("post.hbs","index.hbs")
		for (f in filesToCopy) {
			fileHandler.copyFileFromResources(fileName = f, destination = templatesDir, sourceDir = "$themeTemplateDirName/")
		}
	}

}


/**
 * Really, really destructive and I'm only using it for testing purposes!
 */
@Deprecated("Dangerous, deletes all the things!", level = DeprecationLevel.WARNING)
class Destroyer(siteName: String) {
	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().separator
	init {
		if (siteName.isNotBlank()) {
			println("Destroying your website $currentDirectory$pathSeparator$siteName!")

			val siteRoot = File("$currentDirectory$pathSeparator$siteName")
			siteRoot.deleteRecursively()
		}
		exitProcess(-1)
	}
}
