package org.liamjd.bascule

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.koin.dsl.module.module

/**
 * Declares modules for Dependency Injection via Koin
 */
val generationModule = module {
	factory { AbstractYamlFrontMatterVisitor() }
}

val fileModule = module {
	single { FileHandler() }
}