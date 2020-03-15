package org.liamjd.bascule.pipeline

import mu.KotlinLogging
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.generators.AbstractPostListGenerator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.model.BasculePost
import println.info
import kotlin.math.ceil
import kotlin.math.roundToInt

@Deprecated("doesn't work",ReplaceWith("MultiTaxonomyNavigationGenerator"))
class TaxonomyNavigationGenerator(posts: List<BasculePost>, numPosts: Int = 1, postsPerPage: Int) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	private val logger = KotlinLogging.logger {}

	val FOLDER_NAME = "tags"
	override val TEMPLATE = "tag"

	override suspend fun process(project: Project, renderer: TemplatePageRenderer, fileHandler: FileHandler, clean: Boolean) {
		info("Building tag navigation pages")

		val tagsFolder = fileHandler.createDirectory(project.dirs.output.absolutePath, FOLDER_NAME)

		// filter out unwanted items
		val filteredPosts = posts.filter { it.layout.equals("post") }.sortedByDescending { it.date }

		val tagSet = getAllTagsFromPosts(filteredPosts)

		tagSet.forEach { tag ->
			val taggedPosts = getPostsWithTag(filteredPosts, tag)
			val numPosts = tag.postCount
			if(numPosts != taggedPosts.size) {
				logger.error("${tag} has a mismatch between tag.postCount (${tag.postCount}) and the count of posts with that tag (${taggedPosts.size})")
			}
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
		logger.info("Tag model now $tagSet")

		val renderedContent = renderer.render(model, "taglist")

		fileHandler.writeFile(tagsFolder, "$FOLDER_NAME.html", renderedContent)
	}

	/**
	 * Get a list of posts which have the given tag
	 */
	private fun getPostsWithTag(posts: List<Post>, tag: Tag): List<Post> {
		val taggedPosts = mutableListOf<Post>()
		posts.forEach { post ->
//			post.tags.forEach { t -> if (t.label.equals(tag.label)) {
//				taggedPosts.add(post)
//			} }
		}


	/*	posts.forEach { post ->
			logger.info { "Checking post ${post.title} for its tags"}
			if(post.title=="Upgraded to Grails 2.3.7") {
				logger.warn{"*** Found 'Upgraded to Grails 2.3.7'"}
				logger.warn{"Tags are:"}
				post.tags.forEach { t ->
					logger.warn{"\t$t" }
				}
			*//*post.tags.forEach { t -> if (t.label.equals(tag.label)) {
				logger.info("Found post ${post.title} with tag ${tag.label}")
			} }*//*
		}}*/
		return taggedPosts.toList()
	}

	/**
	 * Get a set of all the unique tags across the site
	 */
	private fun getAllTagsFromPosts(posts: List<Post>): Set<Tag> {

		logger.info { "Taxonomy generation tag status:"}

		// this is failing because a Tag has label, count, boolean values
		// the addAll isn't always picking the "correct" Tag, as this is derived from pages, not a separate set

		val tagSet = mutableSetOf<Tag>()
		posts.forEach { post ->
			post.tags.forEach { tag ->
//				if(tagSet.contains(tag)) {
//					tagSet.elementAt(tagSet.indexOf(tag)).let {
//						if(it.postCount < tag.postCount) {
//							logger.info("${it.label}: resetting count from ${it.postCount} to ${tag.postCount}")
//							it.postCount = tag.postCount
//						}
//						it.hasPosts = true
//					}
//				} else {
//					tagSet.add(tag)
//				}
			}
		}


/*		posts.forEach {
			tagSet.addAll(it.tags)
			logger.info{"${it.tags}"}
		}*/
		logger.info { "Tag set is now..."}
		tagSet.forEach {
			logger.info{ it }
		}
		return tagSet.toSet()
	}

}
