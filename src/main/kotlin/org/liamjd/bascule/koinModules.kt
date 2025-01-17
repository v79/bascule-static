package org.liamjd.bascule

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.koin.dsl.module
import org.liamjd.bascule.cache.BasculeCache
import org.liamjd.bascule.cache.BasculeCacheImpl
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.render.HandlebarsRenderer
import org.liamjd.bascule.scanner.ChangeSetCalculator
import org.liamjd.bascule.scanner.PostBuilder

/**
 * Declares modules for Dependency Injection via Koin
 */
val generationModule = module {
	factory { AbstractYamlFrontMatterVisitor() }
	factory { (project: Project, fileHandler: FileHandler) -> BasculeCacheImpl(project, fileHandler) as BasculeCache }
	factory { (project: Project) -> HandlebarsRenderer(project) as TemplatePageRenderer }
	factory { (project: Project) -> ChangeSetCalculator(project) }
	factory { (project: Project) -> PostBuilder(project) }
}

val fileModule = module {
	single { BasculeFileHandler() }
}

