package org.liamjd.bascule.pipeline

import org.liamjd.bascule.lib.generators.SortAndFilter
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project

/**
 * This implements the default sorting routine, sorting by date descending and including all the posts with the matching layout/template
 */
object DefaultSortAndFilter : SortAndFilter {
	override fun sortAndFilter(project: Project, posts:List<Post>) : List<List<Post>> {
		return posts.reversed().asSequence().withIndex()
				.filter { indexedValue: IndexedValue<Post> -> project.postLayouts.contains(indexedValue.value.layout) }
				.sortedByDescending { it.value.date }
				.groupBy { it.index / project.postsPerPage }
				.map { p -> p.value.map { it.value } }.toList()
	}
}
