package org.liamjd.bascule.initializer

import org.liamjd.bascule.Constants
import org.liamjd.bascule.random
import org.yaml.snakeyaml.Yaml
import picocli.CommandLine
import println.info

@CommandLine.Command(name = "themes", description = ["List available themes"])
class Themes : Runnable {
	override fun run() {
		info(Constants.logos[(0 until Constants.logos.size).random()])
		info("List of predefined themes")
		val yaml = Yaml()
		val data = this.javaClass.getResource(Constants.THEMES_LIST).openStream()

		val yamlData: Map<String, ArrayList<HashMap<String, String>>> = yaml.load(data)
		val yamlList = yamlData.get("themes")!!
		val themeList = mutableListOf<ThemeDetails>()
		yamlList.forEach {
			themeList.add(ThemeDetails(it["theme"]!!, it["description"]!!, it["folder"]!!))
		}

		for (theme in themeList) {
			info(theme.toString())
		}
		println()
		info("To choose a theme when generating a new website, use the --theme <themename> command")
		info("To change the theme in an existing project, edit the <projectname>.yaml configuration file.")
	}
}

data class ThemeDetails(val name: String, val description: String, val folder: String) {
	override fun toString(): String {
		return "\tTheme:\t$name\t $description (in $folder)"
	}
}