package org.liamjd.bascule.assets

import java.io.File

typealias Theme = String

class ProjectStructure(val name: String, val root: File, val sourceDir: File, val outputDir: File, val assetsDir: File, val templatesDir: File, val yamlConfigString: String, val theme: Theme) {

	constructor(name: String, root: File, source: String, output: String, assets: String, templates: String, yaml: String, themeName: String) : this(name, root,
			sourceDir = File(root, source),
			outputDir = File(root, output),
			assetsDir = File(root, assets),
			templatesDir = File(root, templates),
			yamlConfigString = yaml,
			theme = themeName)
}

