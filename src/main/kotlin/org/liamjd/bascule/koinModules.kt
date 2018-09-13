package org.liamjd.bascule

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.koin.dsl.module.module

val generationModule = module {
	factory { AbstractYamlFrontMatterVisitor() as AbstractYamlFrontMatterVisitor }
}