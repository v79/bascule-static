package org.liamjd.bascule.pipeline

import org.liamjd.bascule.generator.Post

abstract class AbstractPostListGenerator(val posts: List<Post>, val numPosts: Int = 1, val postsPerPage: Int) {

	/**
	 * Construct pagination model for the current page in a list of posts
	 */
	fun buildPaginationModel(projectModel: Map<String, Any>, currentPage: Int, totalPages: Int, posts: List<Post>, totalPosts: Int, tagLabel: String? = null): Map<String, Any> {
		val model = mutableMapOf<String, Any>()
		model.putAll(projectModel)
		model.put("currentPage", currentPage)
		model.put("totalPages", totalPages)
		model.put("isFirst", currentPage == 1)
		model.put("isLast", currentPage >= totalPages)
		model.put("previousPage", currentPage - 1)
		model.put("nextPage", currentPage + 1)
		model.put("nextIsLast", currentPage == totalPages)
		model.put("prevIsFirst", (currentPage - 1) == 1)
		model.put("totalPosts", totalPosts)
		model.put("posts", posts)
		model.put("pagination", buildPaginationList(currentPage, totalPages))
		if (tagLabel != null) {
			model.put("tag", tagLabel)
			model.put("title", "Posts tagged '$tagLabel'")
		} else {
			model.put("title", "All posts")
		}

		return model
	}

	/**
	 * This is an awful hard-coded algorithm to produce a pagination list.
	 * @param[currentPage] The current page in the list of pages
	 * @param[totalPages] Total number of pages in the list
	 * @return A list of strings representing the pagination buttons, with a specific format:
	 * _an integer_ represents a page number, e.g. 1, 2, 12...
	 * _. a period_ represents an elipsis, i.e. numbers which have been skipped, e.g 1, 2, ..., 5.
	 * _* an asterisk_, representing the current page.
	 */
	fun buildPaginationList(currentPage: Int, totalPages: Int): List<String> {
		val paginationList = mutableListOf<String>()
		val prev = currentPage - 1
		val next = currentPage + 1
		val isFirst = (currentPage == 1)
		val isLast = (currentPage == totalPages)
		val prevIsFirst = (currentPage - 1 == 1)
		val nextIsLast = (currentPage + 1 == totalPages)

		if (totalPages == 1) {
			paginationList.add("*")
		} else if (isFirst) {
			paginationList.add("*")
			if (nextIsLast) {
				paginationList.add("$next")
			} else {
				paginationList.add("$next")
				paginationList.add(".")
				paginationList.add("$totalPages")
			}
		} else if (prevIsFirst) {
			paginationList.add("1")
			paginationList.add("*")
			if (!isLast) {
				paginationList.add(".")
				paginationList.add("$totalPages")
			}
		} else if (!isLast) {
			paginationList.add("1")
			paginationList.add(".")
			paginationList.add("$prev")
			paginationList.add("*")
			if (!nextIsLast) {
				paginationList.add("$next")
				paginationList.add(".")
				paginationList.add("$totalPages")
			} else {
				paginationList.add("$totalPages")
			}
		} else if (isLast) {
			paginationList.add("1")
			paginationList.add(".")
			paginationList.add("$prev")
			paginationList.add("*")
		}
		return paginationList
	}
}
