package org.liamjd.bascule.pipeline

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

class MultiTaxonomyNavigationGenerator(posts: List<BasculePost>, numPosts: Int = 1, postsPerPage: Int) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {
	override val TEMPLATE = "tag"

	override suspend fun process(project: Project, renderer: TemplatePageRenderer, fileHandler: FileHandler, clean: Boolean) {

		info("Building taxonomy navigation pages...")

		// strip out posts which don't have the basic layout "post" - tagging does not work for non-post pages
		val filteredPosts = posts.filter { it.layout.equals("post") }.sortedByDescending { it.date }

		logMessage("Create folders for each tagKey (size is ${project.tagging.size})")
		project.tagging.forEach {tagKey ->
			logMessage("creating folder for tagKey: $tagKey")
			val tagSlug = tagKey.slug()
			fileHandler.createDirectory(project.dirs.output.absolutePath,tagSlug)

			val allTags = getAllTagsFromPosts(posts,project.tagging.toSet())
			logMessage("Complete map of tags and their values")
			if(allTags.isNotEmpty()) {
				allTags.forEach { t, u ->
					println("Tag: ${t} -> ${u.size} : ${u}")

				}
			}
		}

	}

	private fun getAllTagsFromPosts(posts: List<Post>, taxonomies: Set<String>) : Map<String,Set<Tag>> {
		val tagSet = mutableMapOf<String,Set<Tag>>()
		for(t in taxonomies) {
			val tList = mutableListOf<Tag>()
			posts.forEach {post ->
				if(post.tags[t] != null) {
					post.tags[t]?.toList()?.let { tList.addAll(it) }
				}
			}
			println("tList [$t]: $tList")

			val groupedList = tList.groupBy { it.label }.values.map { it.reduce { acc, item -> Tag(item.label, item.url, acc.postCount + item.postCount, hasPosts = true) } }

			println("groupedList [$t]: $groupedList")
			tagSet.put(t,groupedList.toSet())
		}
		println("FINAL tagSet: $tagSet")

		return tagSet
	}

	private fun logMessage(message: String) {
		println(message)
	}

}
