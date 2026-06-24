package org.liamjd.bascule.scanner

import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.cache.MDCacheItem
import println.info
import java.io.File
import java.util.Locale

/**
 * Pure, side-effect-free helpers extracted from [ChangeSetCalculator] and MarkdownScanner so that the
 * trickiest logic (URL building, cache comparison, file filtering, post ordering) can be unit tested
 * without a filesystem or Koin.
 */

const val MARKDOWN_EXTENSION = "md"

/**
 * A draft file or folder is one whose name starts with "." or "__". Drafts are skipped during scanning.
 */
fun isDraftName(name: String): Boolean = name.startsWith(".") || name.startsWith("__")

/** True if the file has a (case-insensitive) `.md` extension. Pure: only inspects the path string. */
fun isMarkdownFile(file: File): Boolean =
	file.extension.lowercase(Locale.getDefault()) == MARKDOWN_EXTENSION

/**
 * Build the output URL for a post from its slug and the source sub-path (relative to the sources root).
 * A leading backslash is stripped and all backslashes are normalised to forward slashes.
 */
fun calculateUrl(slug: String, sourcePath: String): String =
	if (sourcePath.isEmpty()) {
		"$slug.html"
	} else {
		"${sourcePath.removePrefix("\\")}\\$slug.html".replace("\\", "/")
	}

/**
 * True if [cachedSet] already contains an item matching [mdItem] on path, modification date AND size,
 * i.e. a genuine cache hit where the source file has not changed since the cache was written.
 */
fun cacheContainsItem(mdItem: MDCacheItem, cachedSet: Set<MDCacheItem>): Boolean =
	cachedSet.any {
		it.sourceFilePath == mdItem.sourceFilePath &&
			it.sourceModificationDate == mdItem.sourceModificationDate &&
			it.sourceFileSize == mdItem.sourceFileSize
	}

/**
 * Sort [posts] by post date then url, and wire up the `previous`/`next` links (and the corresponding
 * `older`/`newer` links on any attached post) across all post layouts.
 * Mutates the [MDCacheItem]s (and posts) in place and returns the sorted set.
 */
fun sortAndLinkPosts(posts: Set<CacheAndPost>): Set<CacheAndPost> {
	info("Sorting all posts by date")
	info("Building next and previous links")
	val sortedSet = posts.toSortedSet(
		compareBy(
			{ cacheAndPost -> cacheAndPost.mdCacheItem.link.date },
			{ cacheAndPost -> cacheAndPost.mdCacheItem.link.url })
	)

	// 	val filteredList = sortedSet.filter { it.mdCacheItem.layout == blogPostLayout }.toList()
	val filteredList = sortedSet.toList()
	filteredList.forEachIndexed { index, cacheAndPost ->
		if (index != 0) {
			val olderPost = filteredList[index - 1].mdCacheItem
			cacheAndPost.mdCacheItem.previous = olderPost.link
			cacheAndPost.post?.older = olderPost.link
		}
		if (index != filteredList.size - 1) {
			val newerPost = filteredList[index + 1].mdCacheItem
			cacheAndPost.mdCacheItem.next = newerPost.link
			cacheAndPost.post?.newer = newerPost.link
		}
	}

	return sortedSet
}
