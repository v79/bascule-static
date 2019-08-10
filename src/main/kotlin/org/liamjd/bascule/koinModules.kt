package org.liamjd.bascule

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.koin.dsl.module.module
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.Renderer
import org.liamjd.bascule.render.HandlebarsRenderer
import org.liamjd.bascule.scanner.BasculeCache
import org.liamjd.bascule.scanner.BasculeCacheImpl
import org.liamjd.bascule.scanner.ChangeSetCalculator

/**
 * Declares modules for Dependency Injection via Koin
 */
val generationModule = module {
	factory { AbstractYamlFrontMatterVisitor() }
	factory { (project : Project, fileHandler: FileHandler) -> BasculeCacheImpl(project, fileHandler) as BasculeCache }
	factory { (project : Project) -> HandlebarsRenderer(project) as Renderer }
	factory { (project: Project) -> ChangeSetCalculator(project)}
}

val fileModule = module {
	single { BasculeFileHandler() }
}
