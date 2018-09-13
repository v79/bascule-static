package org.liamjd.bascule.generator

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
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

	companion object Builder : KoinComponent {

		val yamlVisitor by inject<AbstractYamlFrontMatterVisitor>()

		fun createPostFromYaml(fileName: String, document: Document, project: ProjectStructure): PostGeneration {

			val post = Post()

			yamlVisitor.visit(document)
			val data = yamlVisitor.data

			val requiredFields = PostMetaData.values().toSet().filter { it.required }
			requiredFields.forEach {
				if (!data.containsKey(it.name)) {
					// a required field is missing completely!
					return PostGenError("Required field '${it.name} not found", fileName, it.name)
				}
			}

			data.forEach { it ->

				val metaData: PostMetaData?
				if (PostMetaData.contains(it.key)) {
					metaData = PostMetaData.valueOf(it.key)

					// validate the metadata
					if (it.value.isEmpty() || (it.value.size == 1 && it.value[0].isNullOrBlank())) {
						if (metaData.required) {
							// a required field exists but has no value
							return PostGenError("Missing required field '${metaData.name}' in source file '${fileName}'", fileName, metaData.name)
						}
					} else {
						val valueList = it.value // we'll have bailed by now if this is empty?

						if (!metaData.multipleAllowed && valueList != null && valueList.size > 1) {
							return PostGenError("Field '${metaData.name}' is only allowed a single value; found '${it.value[0]}' in source file '$fileName'", fileName, metaData.name)
						}
						val value = it.value[0]

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
					val finalVal  = if(it.value.size == 1) it.value[0] else it.value
					post.attributes.put(it.key, finalVal)

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