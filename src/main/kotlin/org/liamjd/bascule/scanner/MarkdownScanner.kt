package org.liamjd.bascule.scanner

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.options.MutableDataSet
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.koin.core.parameter.ParameterList
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.cache.LocalDateTimeSerializer
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import org.liamjd.bascule.slug
import println.ProgressBar
import println.info
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.system.measureTimeMillis


@UnstableDefault
class MarkdownScanner(val project: Project) : KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })
	val BLOG_POST = "post"

	val mdOptions = MutableDataSet()
	val mdParser: Parser

	init {
		// TODO: move this into another class? Configure externally?
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create(), TablesExtension.create(), HydeExtension.create()))
		mdOptions.set(HtmlRenderer.GENERATE_HEADER_ID, true).set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
		mdOptions.set(HtmlRenderer.INDENT_SIZE, 2) // prettier HTML
		mdOptions.set(HydeExtension.SOURCE_FOLDER, project.dirs.sources.toString())
		mdParser = Parser.builder(mdOptions).build()
	}


	// this method is called by
	fun calculateRenderSet() : Set<CacheAndPost> {
		val uncachedSet = calculateUncachedSet()
		val sorted = orderPosts(uncachedSet)
		// urgh. can't kotlin do this for me?
		val toBeCached = mutableSetOf<MDCacheItem>()
		sorted.forEach { cacheAndPost ->
			toBeCached.add(cacheAndPost.mdCacheItem)
		}
		writeCache(toBeCached)
		return sorted
	}

	private fun writeCache(setToCache: Set<MDCacheItem>) {
		val json = Json(JsonConfiguration(prettyPrint = true))
		val jsonData = json.stringify(MDCacheItem.serializer().set, setToCache)
		fileHandler.writeFile(project.dirs.sources,"${project.name.slug()}.cache.json",jsonData)
	}

	private fun calculateUncachedSet() : Set<CacheAndPost> {
		info("Scanning ${project.dirs.sources.absolutePath} for markdown files")

		val errorMap = mutableMapOf<String, Any>()
		val allSources = mutableSetOf<CacheAndPost>()

		val timeTaken = measureTimeMillis {
			val markdownScannerProgressBar = ProgressBar("Reading markdown files", animated = true, asPercentage = false)

			project.dirs.sources.walk().forEachIndexed { index, mdFile ->

				if (mdFile.name.startsWith(".") || mdFile.name.startsWith("__")) {
					markdownScannerProgressBar.progress(index, "Skipping draft file/folder '${mdFile.name}'")
					return@forEachIndexed // skip this one
				}

				if (mdFile.isDirectory) {
					return@forEachIndexed
				}

				if (mdFile.extension.toLowerCase() != "md") {
					markdownScannerProgressBar.progress(index, "Skipping file ${mdFile.name} as extension does not match '.md'")
					return@forEachIndexed
				} else {
					info("Processing file ${index} ${mdFile.name}...")

					// construct MDCacheItem for this file, and compare it with the cache file
					val fileLastModifiedDateTimeLong = mdFile.lastModified();
					val fileLastModifiedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fileLastModifiedDateTimeLong), TimeZone
							.getDefault().toZoneId())
					val mdItem = MDCacheItem(mdFile.length(), mdFile.absolutePath, fileLastModifiedDateTime)

					// the site model needs to know the relationship between all the pages
					// specifically the links between each ite
					// and ideally the tags too

					// now need to extract the yaml frontispiece

					val inputStream = mdFile.inputStream()
					val document = parseMarkdown(inputStream)
					val post = BasculePost.createPostFromYaml(mdFile, document, project)

					// check for errors
					when (post) {
						is PostGenError -> {
							errorMap.put(mdFile.name, post.errorMessage)
						}
						is BasculePost -> {
							val sourcePath = mdFile.parentFile.absolutePath.toString().removePrefix(project.dirs.sources.absolutePath.toString())
							mdItem.layout = post.layout
							post.url = calculateUrl(post.slug, sourcePath)
							mdItem.link = PostLink(post.title, post.url, post.date)

							post.sourceFileName = mdFile.canonicalPath
							post.destinationFolder = File(project.dirs.output,sourcePath)
							post.rawContent = mdFile.readText() // TODO: this still contains the yaml front matter :(

							allSources.add(CacheAndPost(mdItem,post))
						}
					}
				}

			}

			markdownScannerProgressBar.progress(1,"Cache items found for all files.")
		}
		info("Time taken to calculate set: ${timeTaken}ms")
		if(errorMap.isNotEmpty()) {
			println.error("Errors found in calculations:")
			errorMap.forEach { t, u ->
				println.error("$t -> $u")
			}
		}
		return allSources
	}

	private fun parseMarkdown(inputStream: InputStream): Document {
		val text = inputStream.bufferedReader().readText()
		return mdParser.parse(text)
	}

	private fun calculateUrl(slug: String, sourcePath: String): String {
		val url: String = if (sourcePath.isEmpty()) {
			"$slug.html"
		} else {
			"${sourcePath.removePrefix("\\")}\\$slug.html".replace("\\", "/")
		}
		return url
	}

	/**
	 * Sorts posts according to the date in the PostLink property (user provided via yaml)
	 * Then creates the navigation links between each post (
	 */
	private fun orderPosts(posts:Set<CacheAndPost>): Set<CacheAndPost> {
		info("sorting")
		val sortedSet = posts.toSortedSet(compareBy({ cacheAndPost -> cacheAndPost.mdCacheItem.link.date}))
		println("sorted set size: ${sortedSet.size}")
		info("building next and previous links")

		val filteredList = sortedSet.filter { cacheAndPost -> cacheAndPost.mdCacheItem.layout.equals(BLOG_POST) }.toList()
		filteredList.forEachIndexed { index, cacheAndPost ->
			if(index != 0) {
				val olderPost = filteredList.get(index -1).mdCacheItem
				cacheAndPost.mdCacheItem.previous = olderPost.link
				cacheAndPost.post.older = olderPost.link
			}
			if(index != filteredList.size-1) {
				val newerPost = filteredList.get(index+1).mdCacheItem
				cacheAndPost.mdCacheItem.next = newerPost.link
				cacheAndPost.post.newer = newerPost.link
			}
		}

		println("filteredList size: ${filteredList.size}")

		return sortedSet
	}
}

@Serializable
class MDCacheItem(val sourceFileSize: Long, val sourceFilePath: String, @Serializable(with = LocalDateTimeSerializer::class) val sourceModificationDate: LocalDateTime) {

	@Serializable(with = PostLinkSerializer::class)
	lateinit var link: PostLink

	@Transient // don't know if I need the tags or not
	val tags: Set<String> = mutableSetOf()

	@Serializable(with = PostLinkSerializer::class)
	var previous: PostLink? = null

	@Serializable(with = PostLinkSerializer::class)
	var next: PostLink? = null

	var layout: String? = null

	override fun toString(): String {
		return "$link [older=${previous}] [newer=${next}]"
	}

}

object PostLinkSerializer : KSerializer<PostLink> {
	override fun deserialize(decoder: Decoder): PostLink {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun serialize(encoder: Encoder, obj: PostLink) {
		val compositeOutput = encoder.beginStructure(descriptor)
		compositeOutput.encodeStringElement(descriptor, 0, obj.title)
		compositeOutput.encodeStringElement(descriptor, 1, obj.url)
		compositeOutput.encodeLongElement(descriptor, 2, localDateToLong(obj.date))
		compositeOutput.endStructure(descriptor)
	}

	override val descriptor: SerialDescriptor = object : SerialClassDescImpl("postLink") {
		init {
			addElement("title") // req will have index 0
			addElement("url") // res will have index 1
			addElement("date")
		}
	}

	private fun localDateToLong(date: LocalDate): Long {
		val zoneId = ZoneId.systemDefault() // or: ZoneId.of("Europe/Oslo");
		return date.atStartOfDay(zoneId).toEpochSecond()

	}

}

class CacheAndPost(val mdCacheItem: MDCacheItem,val post:BasculePost)

//public final val date: java.time.LocalDate /* compiled code */

//public final val title: kotlin.String /* compiled code */

//public final val url: kotlin.String /* compiled code */
