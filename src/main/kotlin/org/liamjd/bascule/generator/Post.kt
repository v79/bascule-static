package org.liamjd.bascule.generator

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.liamjd.bascule.assets.ProjectStructure
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Post {

	var sourceFileName: String = ""
	var url: String = ""
	var title: String = ""
	var author: String = ""

	var layout: String = ""
	var date: LocalDate = LocalDate.now()

	var tags: List<String> = mutableListOf()
	var slug: String = ""
	var attributes: MutableMap<String, Any> = mutableMapOf()

	fun toModel(): Map<String, Any> {
		val modelMap = mutableMapOf<String, Any>()
		modelMap.put("sourceFileName", sourceFileName)
		modelMap.put("url", url)
		modelMap.put("title", title)
		modelMap.put("author", author)
		modelMap.put("layout", layout)
		modelMap.put("date", date)
		modelMap.put("tags", tags)
		modelMap.put("slug", slug)
		modelMap.put("attributes", attributes)

		return modelMap
	}

	object Builder {

		val yamlVisitor: AbstractYamlFrontMatterVisitor = AbstractYamlFrontMatterVisitor()

		fun createPostFromYaml(document: Document, project: ProjectStructure): Post {
			val post = Post()

			yamlVisitor.visit(document)
			yamlVisitor.data.forEach {
				val value = it.value.get(0)
				when (it.key) {
					"title" -> {
						post.title = value
					}
					"layout" -> {
						post.layout = value
					}
					"author" -> {
						post.author = value
					}
					"slug" -> {
						post.slug = value
					}
					"date" -> {
						val dateFormat = project.model["dateFormat"] as String? ?: "dd/MM/yyyy"
						val formatter = DateTimeFormatter.ofPattern(dateFormat)
						post.date = LocalDate.parse(value, formatter)
					}
					"tags" -> {
						post.tags = value.drop(1).dropLast(1).split(",")
					}
					else -> {
						if (value.startsWith("[") && value.endsWith("]")) {
							// split into an array
							val array = value.drop(1).dropLast(1).split(",")
							post.attributes.put(it.key, array)
						} else {
							post.attributes.put(it.key, value)
						}
					}
				}
			}
			return post
		}
	}
}