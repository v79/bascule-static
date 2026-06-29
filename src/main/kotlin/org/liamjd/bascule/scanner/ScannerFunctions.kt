package org.liamjd.bascule.scanner

import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.cache.MDCacheItem
import println.debug
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
 * Both `\` and `/` separators are normalised to forward slashes, and any leading/trailing separators are
 * stripped so the result is always a clean root-relative path (no leading slash). This matters because the
 * sub-path arrives with a leading OS separator (`\2025` on Windows, `/2025` on Linux); a surviving leading
 * slash would combine with the template's own `/` prefix to produce a protocol-relative `//2025/...` URL,
 * where the year folder is wrongly parsed as the host.
 */
fun calculateUrl(slug: String, sourcePath: String): String {
	val normalised = sourcePath.replace("\\", "/").trim('/')
	return if (normalised.isEmpty()) "$slug.html" else "$normalised/$slug.html"
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
	debug("Sorting all posts by date")
	debug("Building next and previous links")
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
