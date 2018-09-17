package org.liamjd.bascule

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.koin.dsl.module.module
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.render.HandlebarsRenderer
import org.liamjd.bascule.render.Renderer

/**
 * Declares modules for Dependency Injection via Koin
 */
val generationModule = module {
	factory { AbstractYamlFrontMatterVisitor() }
	factory { (project : ProjectStructure) -> HandlebarsRenderer(project) as Renderer}
}

val fileModule = module {
	single { FileHandler() }
}
