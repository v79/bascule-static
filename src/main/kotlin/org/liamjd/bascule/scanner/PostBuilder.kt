package org.liamjd.bascule.scanner

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostStatus
import org.liamjd.bascule.slug
import java.io.File

/**
 * Utility class to construct a [BasculePost] from a Markdown source file
 * Most of the work is delegated to the [BasculePost.createPostFromYaml] method, after first parsing the markdown document.
 */
class PostBuilder(val project: Project, private val fileHandler: FileHandler) {

	val mdParser: Parser = Parser.builder(project.markdownOptions).build()
	private val cacheFileName: String = "${project.name.slug()}.cache.json"

	/**
	 * Build a [PostStatus] object from the Markdown source file
	 * @return either a [BasculePost] or a [org.liamjd.bascule.model.PostGenError]
	 */
	fun buildPost(mdFile: File): PostStatus {
		val document = parseMarkdown(mdFile)
		// a fresh visitor per post; the visitor accumulates YAML data and must not be shared across files
		val post = BasculePost.createPostFromYaml(mdFile, document, project, AbstractYamlFrontMatterVisitor())

		return post
	}

	private fun parseMarkdown(file: File): Document {
		val text = fileHandler.readFileAsString(file.parentFile, file.name)
		return mdParser.parse(text)
	}
}
