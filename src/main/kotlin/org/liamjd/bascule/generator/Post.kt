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

	companion object Builder {

		fun createPostFromYaml(fileName: String, document: Document, project: ProjectStructure): PostGeneration {
			val yamlVisitor: AbstractYamlFrontMatterVisitor = AbstractYamlFrontMatterVisitor()
			val post = Post()

			yamlVisitor.visit(document)
			yamlVisitor.data.forEach {

				val metaData: PostMetaData?
				if (PostMetaData.contains(it.key)) {
					metaData = PostMetaData.valueOf(it.key)

					// validate the metadata
					if (it.value.isEmpty()) {
						if (metaData.required) {
							return PostGenError("Missing required field '${metaData.name}' in source file '${fileName}'", fileName, metaData.name)
						}
					} else {
						val valueList = splitArray(it.value[0]) // we'll have bailed by now if this is empty?
						val value = it.value[0]

						if (!metaData.multipleAllowed && valueList != null && valueList.size > 1) {
							return PostGenError("Field '${metaData.name}' is only allowed a single value; found '${it.value[0]}' in source file '$fileName", fileName, metaData.name)
						}

						when (metaData) {
							PostMetaData.title -> {
								post.title = value
								println("\t\t title -> ${post.title}")
							}
							PostMetaData.layout -> {
								post.layout = value
								println("\t\t layout -> ${post.layout}")
							}
							PostMetaData.author -> {
								post.author = value
								println("\t\t author -> ${post.author}")
							}
							PostMetaData.slug -> {
								post.slug = value
								println("\t\t slug -> ${post.slug}")
							}
							PostMetaData.date -> {
								val dateFormat = project.model["dateFormat"] as String? ?: "dd/MM/yyyy"
								val formatter = DateTimeFormatter.ofPattern(dateFormat)
								post.date = LocalDate.parse(value, formatter)
								println("\t\t date -> ${post.date}")
							}
							PostMetaData.tags -> {
								post.tags = value.drop(1).dropLast(1).split(",")
								println("\t\t tags -> ${post.tags}")
							}
						}
					}


				} else {
					val valueList = splitArray(it.value[0])
					val value = it.value[0]
					if(valueList != null) {
						post.attributes.put(it.key,valueList)
					} else {
						post.attributes.put(it.key,value)
					}

					/*if (value.startsWith("[") && value.endsWith("]")) {
						// split into an array
						val array = value.drop(1).dropLast(1).split(",")
						post.attributes.put(it.key, array)
					} else {
						post.attributes.put(it.key, value)
					}*/
					println("\t\t attributes -> ${it.key} -> ${post.attributes[it.key]}")

				}
			} // else do nothing? depends on the key I guess...
			return post
		}

		fun splitArray(value: String): List<String>? {
			if (value.startsWith("[").and(value.endsWith("]"))) {
				return value.drop(1).dropLast(1).split(",")
			} else {
				return null
			}
		}
	}
}

enum class PostMetaData(val required: Boolean, val multipleAllowed: Boolean) {
	title(true, false),
	layout(true, false),
	author(false, false),
	slug(false, false),
	date(false, false),
	tags(false, true),
	custom(false, true);

	companion object {
		fun contains(key: String): Boolean {
			for (md in values()) {
				if (md.name == key) return true
			}
			return false
		}
	}
}