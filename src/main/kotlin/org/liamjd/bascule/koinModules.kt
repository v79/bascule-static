package org.liamjd.bascule

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.koin.dsl.module.module
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.Renderer
import org.liamjd.bascule.render.HandlebarsRenderer

/**
 * Declares modules for Dependency Injection via Koin
 */
val generationModule = module {
	factory { AbstractYamlFrontMatterVisitor() }
	factory { (project : Project) -> HandlebarsRenderer(project) as Renderer }
}

val fileModule = module {
	single { BasculeFileHandler() }
}
