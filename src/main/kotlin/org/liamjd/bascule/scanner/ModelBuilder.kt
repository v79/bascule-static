package org.liamjd.bascule.scanner

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor

class ModelBuilder(val document: Document) {

	val yamlVisitor: AbstractYamlFrontMatterVisitor

	init {
		yamlVisitor = AbstractYamlFrontMatterVisitor()
		yamlVisitor.visit(document)
	}

	/**
	 * Get the named attribute. These will be common to all bascule projects, and are distinct
	 * from custom models
	 */
	fun getAttribute(name: String): String {
		val attribute = yamlVisitor.data[name]?.let {
			it.get(0)
		}
		return attribute ?: ""
	}

	/**
	 * Get a named attribute, expecting a list
	 */
	fun getAttributeList(name: String) : List<Any> {
		val list = yamlVisitor.data[name]?.let {
			it
		}
		return list as List<Any>
	}

	fun getModel(): Map<String, Any> {
		val model = mutableMapOf<String, Any>()


		// TODO: make this better
		yamlVisitor.data.forEach {
			val value = it.value.get(0)
			if (value.startsWith("[") && value.endsWith("]")) {
				println("Splitting into an array")
				// split into an array
				val array = value.drop(1).dropLast(1).split(",")
				model[it.key] = array
			} else {
				model[it.key] = value
			}
		}
		println("\t model (sans content) is $model")

		return model
	}
}