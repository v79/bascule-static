package org.liamjd.bascule.pipeline

import org.liamjd.bascule.FileHandler
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.render.Renderer

interface GeneratorPipeline {

	val TEMPLATE: String
	fun process(project: ProjectStructure, renderer: Renderer, fileHandler: FileHandler)
}
