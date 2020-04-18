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
import org.liamjd.bascule.slug
import println.info
import java.io.File
import kotlin.math.ceil
import kotlin.math.roundToInt

class MultiTaxonomyNavigationGenerator(posts: List<BasculePost>, numPosts: Int = 1, postsPerPage: Int) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	private val logger = KotlinLogging.logger {}

	override val TEMPLATE = "tag"
	private val MINIMUM_TAGGED_POSTS = 1

	override suspend fun process(project: Project, renderer: TemplatePageRenderer, fileHandler: FileHandler, clean: Boolean) {

		info("Building taxonomy navigation pages...")

		// strip out posts which don't have the basic layout "post" - tagging does not work for non-post pages
		val filteredPosts = posts.filter { it.layout.equals("post") }.sortedByDescending { it.date }
		val allTags: List<Tag> = getAllTagsFromPosts(filteredPosts, project.tagging.toSet())
		val tagKeyFolders = mutableMapOf<String, File>()
		project.tagging.forEach { tagKey ->
			val tagSlug = tagKey.slug()
			val tagKeyFolder = fileHandler.createDirectory(project.dirs.output.absolutePath, tagSlug)
			tagKeyFolders.put(tagSlug, tagKeyFolder)
		}

		// we have a folder for each tagKey at this point, e.g. "genres", "composers"
		// next we need to get all the posts for each tag in each tagKey
		// and then write a folder for each tag
		// and then write 1 or more listing pages for each tag

		if (allTags.size > 0) {
			for (tag in allTags) {
				val category = tag.category
				val tagKeyFolder = tagKeyFolders[category.slug()]!!
				val taggedPosts = getPostsWithTag(tag, filteredPosts)
				val numPosts = tag.postCount-1 // we need it zero-indexed, now
				if (numPosts != taggedPosts.size) {
					logger.error("${tag} has a mismatch between tag.postCount (${tag.postCount}) and the count of posts with that tag (${taggedPosts.size})")
				}
				val totalPages = ceil(numPosts.toDouble() / postsPerPage).roundToInt()

				// only create tagged index pages if there's more than MINIMUM_TAGGED_POSTS page with the tag
				if (taggedPosts.size > MINIMUM_TAGGED_POSTS) {
					val thisTagFolder = fileHandler.createDirectory(tagKeyFolder.absolutePath, tag.url)
					for (page in 1..totalPages) {
						val startPos = postsPerPage * (page - 1)
						val endPos = (postsPerPage * page)
						val finalEndPos = if (endPos > taggedPosts.size) taggedPosts.size else endPos
						val paginationModel = buildPaginationModel(projectModel = project.model, currentPage = page, totalPages = totalPages, posts = taggedPosts.subList(startPos, finalEndPos), totalPosts = numPosts, tagLabel = tag.label)
						val model = mutableMapOf<String, Any>()
						model.putAll(paginationModel)
						model.put("tagKey", category)
						model.put("tagUrl", category.slug())

						val renderedContent = renderer.render(model, TEMPLATE)
						fileHandler.writeFile(thisTagFolder, "${tag.url}$page.html", renderedContent)
					}
				}
			}

			// now build the page which lists each ???
			project.tagging.forEach { category ->
				info("Building tagkey $category list page")
				val tagKeyFolder = tagKeyFolders[category.slug()]!!
				val model = mutableMapOf<String, Any>()
				model.putAll(project.model)
				model.put("title", "List of ${category}")
				model.put("tagKey", category)
				model.put("tagUrl", category.slug())
				model.put("tags", allTags.filter { it.category == category && it.postCount > 1 }.sortedBy { it.postCount }.reversed())


				val renderedContent = renderer.render(model, "taglist")
				fileHandler.writeFile(tagKeyFolder, "${category.slug()}.html", renderedContent)
			}
		}

	}

	private fun getAllTagsFromPosts(posts: List<Post>, taxonomies: Set<String>): List<Tag> {
		val tList = mutableListOf<Tag>()
		posts.forEach { post ->
			tList.addAll(post.tags.toList())
		}
		val groupedList = tList.groupBy { it.label }.values.map { it.reduce { acc, item -> Tag(category = item.category, label = item.label, url = item.url, postCount = item.postCount + 1, hasPosts = item.postCount > 1) } }
		return groupedList
	}

	private fun getPostsWithTag( tag: Tag, posts: List<Post>): List<Post> {
		val taggedPosts = mutableListOf<Post>()
		posts.forEach { post ->
			if (post.tags.contains(tag)) {
				taggedPosts.add(post)
			}
		}
		return taggedPosts.toList()
	}

}

