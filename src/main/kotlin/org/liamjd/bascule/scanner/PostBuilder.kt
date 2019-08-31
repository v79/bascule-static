package org.liamjd.bascule.scanner

import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import org.koin.core.parameter.ParameterList
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostStatus
import org.liamjd.bascule.slug
import java.io.File

/**
 * Utility class to construct a [BasculePost] from a markdown source file
 * Most of the work is delegated to the [BasculePost.createPostFromYaml] method, after first parsing the markdown document.
 */
class PostBuilder(val project: Project) : KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })

	val mdParser: Parser = Parser.builder(project.markdownOptions).build()
	private val cacheFileName: String = "${project.name.slug()}.cache.json"

	/**
	 * Build a [PostStatus] object from the markdown source file
	 * @return either a [BasculePost] or a [PostGenError]
	 */
	fun buildPost(mdFile: File): PostStatus {
		val document = parseMarkdown(mdFile)
		val post = BasculePost.createPostFromYaml(mdFile, document, project)

		return post
	}

	private fun parseMarkdown(file: File): Document {
		val text = fileHandler.readFileAsString(file.parentFile, file.name)
		return mdParser.parse(text)
	}
}
