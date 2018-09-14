package org.liamjd.bascule.generator

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.assets.ProjectStructure
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern


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
		modelMap.put("sourceFileName.name", sourceFileName)
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
		val REMOMVE_TAGS = Pattern.compile("<.+?>")
		val matcher = REMOMVE_TAGS.matcher(content.take(150))
		return matcher.replaceAll("").plus("...")
	}

	companion object Builder : KoinComponent {

		fun createPostFromYaml(file: File, document: Document, project: ProjectStructure): PostGeneration {
			val yamlVisitor by inject<AbstractYamlFrontMatterVisitor>()
			yamlVisitor.visit(document)
			val data = yamlVisitor.data

			if (data == null || data.isEmpty()) {
				println.error("No YAML frontispiece for file '${file.name}'. Attempting to construct a post from just the file itself.")
				val noYamlPost = buildPostWithoutYaml(file)
				return noYamlPost
			}

			val requiredFields = PostMetaData.values().toSet().filter { it.required }
			requiredFields.forEach {
				if (!data.containsKey(it.name)) {
					// a required field is missing completely!
					return PostGenError("Required field '${it.name} not found", file.name, it.name)
				}
			}

			val post = Post()

			data.forEach { it ->
				val metaData: PostMetaData?
				if (PostMetaData.contains(it.key)) {
					metaData = PostMetaData.valueOf(it.key)

					// validate the metadata
					if (it.value.isEmpty() || (it.value.size == 1 && it.value[0].isNullOrBlank())) {
						if (metaData.required) {
							// a required field exists but has no value
							return PostGenError("Missing required field '${metaData.name}' in source file '${file.name}'", file.name, metaData.name)
						}
					} else {
						val valueList = it.value // we'll have bailed by now if this is empty?

						if (!metaData.multipleAllowed && valueList != null && valueList.size > 1) {
							return PostGenError("Field '${metaData.name}' is only allowed a single value; found '${it.value[0]}' in source file '$file.name'", file.name, metaData.name)
						}
						val value = it.value[0]

						when (metaData) {
							PostMetaData.title -> {
								post.title = value
							}
							PostMetaData.layout -> {
								post.layout = value
							}
							PostMetaData.author -> {
								post.author = value
							}
							PostMetaData.slug -> {
								post.slug = value
							}
							PostMetaData.date -> {
								val dateFormat = project.model["dateFormat"] as String? ?: "dd/MM/yyyy"
								val formatter = DateTimeFormatter.ofPattern(dateFormat)
								post.date = LocalDate.parse(value, formatter)
							}
							PostMetaData.tags -> {
								post.tags = value.drop(1).dropLast(1).split(",")
							}
							else -> {
								println("How did i get here?")
							}
						}
					}


				} else {
					val finalVal = if (it.value.size == 1) it.value[0] else it.value
					post.attributes.put(it.key, finalVal)
				}
			} // else do nothing? depends on the key I guess...
			return post
		}

		/**
		 * Attempt to construct a post without any Yaml metadata. Can only provide a title, layout (always "post"), a slug and a date.
		 * No author, tags or custom attributes
		 */
		fun buildPostWithoutYaml(file: File): PostGeneration {
			val post = Post()
			post.sourceFileName = file.name
			post.title = file.nameWithoutExtension

			// have to make an assumption about the layout - going for "post"
			post.layout = "post"
			// get the date from the file itself
			val filePath = file.toPath()
			val attributes = Files.readAttributes(filePath, BasicFileAttributes::class.java)
			val milliseconds = attributes.creationTime().toInstant()
			post.date = milliseconds.atZone(ZoneId.systemDefault()).toLocalDate()

			// transform the source file name to a cleaner URL
			val slugRegex = Regex("[^a-zA-Z0-9-]")
			post.slug = slugRegex.replace(file.nameWithoutExtension.toLowerCase(), "-")

			// author, tags will all be empty. No custom attributes
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