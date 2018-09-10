package org.liamjd.bascule.generator

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.liamjd.bascule.assets.ProjectStructure
import java.time.LocalDate
import java.time.format.DateTimeFormatter


sealed class PostGeneration
data class PostGenError(val errorMessage: String, val fileName: String, var field: String?) : PostGeneration()

class Post : PostGeneration() {

	var sourceFileName: String = ""
	var url: String = ""
	var title: String = ""
	var author: String = ""

	var layout: String = ""
	var date: LocalDate = LocalDate.now()

	var tags: List<String> = mutableListOf()
	var slug: String = ""
	var attributes: MutableMap<String, Any> = mutableMapOf()

	var content: String = ""

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

	fun getSummary(): String {
		return content.take(100) + "..."
	}

	object Builder {


		fun createPostFromYaml(fileName: String, document: Document, project: ProjectStructure): PostGeneration {
			val yamlVisitor: AbstractYamlFrontMatterVisitor = AbstractYamlFrontMatterVisitor()
			val post = Post()

			yamlVisitor.visit(document)
			yamlVisitor.data.forEach {


				try {
					val metaData: PostMetaData = PostMetaData.valueOf(it.key)
					println("\t\t metaData -> $metaData -> ${it.value}")
					if(metaData.required && it.value.isEmpty()) {
						return PostGenError("Missing required field '${metaData.name}' in source file '${fileName}'",fileName,metaData.name)
					}
				} catch (iae: IllegalArgumentException) {
					println("found ${it.key} instead")
				}





//				println("\t\t it.value = ${it.value}")
				if (it.value != null && it.value.isNotEmpty()) {
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
				} // else do nothing? depends on the key I guess...
			}
			return post
		}
	}
}

enum class PostMetaData( val required: Boolean, val multipleAllowed: Boolean) {
	title(true,false),
	layout(true,false),
	author(false,false),
	slug(false,false),
	date(false,false),
	tags(false,true),
	custom(false,true);
}