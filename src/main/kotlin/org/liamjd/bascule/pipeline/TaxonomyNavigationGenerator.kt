package org.liamjd.bascule.pipeline

import org.liamjd.bascule.FileHandler
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.generator.Post
import org.liamjd.bascule.generator.Tag
import org.liamjd.bascule.render.Renderer
import println.info
import kotlin.math.ceil
import kotlin.math.roundToInt

class TaxonomyNavigationGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	val FOLDER_NAME = "tags"
	override val TEMPLATE = "tag"

	override fun process(project: ProjectStructure, renderer: Renderer, fileHandler: FileHandler) {
		info("Building tag navigation pages")
		val tagsFolder = fileHandler.createDirectory(project.outputDir.absolutePath, FOLDER_NAME)
		val tagSet = getAllTags(posts)

		tagSet.forEachIndexed { index, tag ->
			val taggedPosts = getPostsWithTag(posts, tag)
			val numPosts = tag.postCount
			val totalPages = ceil(numPosts.toDouble() / postsPerPage).roundToInt()

			// only create tagged index pages if there's more than one page with the tag
			if (taggedPosts.size > 1) {
				val thisTagFolder = fileHandler.createDirectory(tagsFolder.absolutePath, tag.url)
				for (page in 1..totalPages) {
					val startPos = postsPerPage * (page - 1)
					val endPos = (postsPerPage * page)
					val finalEndPos = if (endPos > taggedPosts.size) taggedPosts.size else endPos
					val model = buildPaginationModel(projectModel = project.model, currentPage = page, totalPages = totalPages, posts = taggedPosts.subList(startPos, finalEndPos), totalPosts = numPosts, tagLabel = tag.url)

					val renderedContent = renderer.render(model, TEMPLATE)
					fileHandler.writeFile(thisTagFolder, "${tag.url}$page.html", renderedContent)
				}
			}
		}

		info("Building tag list page")

		val model = mutableMapOf<String, Any>()
		model.putAll(project.model)
		model.put("title", "List of tags")
		model.put("tags", tagSet.filter { it.postCount > 1 }.sortedBy { it.postCount }.reversed())

		val renderedContent = renderer.render(model, "taglist")

		fileHandler.writeFile(tagsFolder, "$FOLDER_NAME.html", renderedContent)
	}

	/**
	 * Get a list of posts which have the given tag
	 */
	private fun getPostsWithTag(posts: List<Post>, tag: Tag): List<Post> {
		val taggedPosts = mutableListOf<Post>()
		posts.forEach {
			it.tags.forEach { t -> if (t.label.equals(tag.label)) taggedPosts.add(it) }
		}
		return taggedPosts.toList()
	}

	/**
	 * Get a set of all the unique tags across the site
	 */
	private fun getAllTags(posts: List<Post>): Set<Tag> {
		val tagSet = mutableSetOf<Tag>()
		posts.forEach {
			tagSet.addAll(it.tags)
		}
		return tagSet.toSet()
	}

}
