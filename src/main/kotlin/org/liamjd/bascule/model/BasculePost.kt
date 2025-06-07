package org.liamjd.bascule.model

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.util.ast.Document
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.slug
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class PostGenError(val errorMessage: String, val fileName: String, var field: String?) : PostStatus()

/**
 * A PostStatus is either "BasculePost" (successful) or "PostGenError" (when unable to create a BasculePost).
 */
sealed class PostStatus

/**
 * Class representing an individual BasculePost. This significantly extends the [Post] base class, and its companion is a builder pattern
 */
class BasculePost(val document: Document) : Post, PostStatus() {

    override var sourceFileName: String = ""
    override var url: String = ""
    override var title: String = ""
    override var author: String = ""

    override var layout: String = ""
    override var date: LocalDate = LocalDate.now()

    override var tags = mutableSetOf<Tag>()
    override var slug: String = ""
    override var attributes: MutableMap<String, Any> = mutableMapOf()

    override var content: String = ""
    override var rawContent: String = ""
    override var newer: PostLink? = null
    override var older: PostLink? = null

    var destinationFolder: File? = null

    /**
     * Return a short excerpt from the post, stripping out any HTML and returning just plain text
     */
    override fun getSummary(characterCount: Int): String {
        val REMOMVE_TAGS = Pattern.compile("<.+?>")
        val matcher = REMOMVE_TAGS.matcher(content.take(characterCount))
        return matcher.replaceAll("").plus("...")
    }

    override fun groupTagsByCategory(): Map<out String, Set<Tag>?> {
        println("Grouping tags by category - NOT YET IMPLEMENTED")
        return emptyMap()
    }

    /**
     * Construct a BasculePost from the source markdown document.
     * Ideally the document should contain a yaml front piece describing the post.
     * If the yaml does not exist, it will make some best guesses from the file.
     * Returns a PostStatus.BasculePost if successful, or PostStatus.PostGenError if unable to parse the content.
     *
     */
    companion object Builder : KoinComponent, Comparator<BasculePost> {

        /**
         * Sorting comparator by date order
         */
        override fun compare(o1: BasculePost, o2: BasculePost): Int {
            return if (o1.date.compareTo(o2.date) == 0) {
                o1.url.compareTo(o2.url)
            } else {
                o1.date.compareTo(o2.date)
            }
        }

        fun createPostFromYaml(file: File, document: Document, project: Project): PostStatus {
            val yamlVisitor by inject<AbstractYamlFrontMatterVisitor>()
            yamlVisitor.visit(document)
            val data = yamlVisitor.data

            if (data == null || data.isEmpty()) {
                println.error("No YAML frontispiece for file '${file.name}'. Attempting to construct a post from just the file itself.")
                return buildPostWithoutYaml(file, document)
            }

            val requiredFields = PostMetaData.values().toSet().filter { it.required }
            requiredFields.forEach {
                if (!data.containsKey(it.name)) {
                    // a required field is missing completely!
                    return PostGenError("Required field '${it.name} not found", file.name, it.name)
                }
            }

            val post = BasculePost(document)

            data.forEach { it ->
                val metaData: PostMetaData?
                if (PostMetaData.contains(it.key)) {
                    metaData = PostMetaData.valueOf(it.key)

                    // validate the metadata
                    if (it.value.isEmpty() || (it.value.size == 1 && it.value[0].isNullOrBlank())) {
                        if (metaData.required) {
                            // a required field exists but has no value
                            return PostGenError(
                                "Missing required field '${metaData.name}' in source file '${file.name}'",
                                file.name,
                                metaData.name
                            )
                        }
                    } else {
                        val valueList = it.value // we'll have bailed by now if this is empty?

                        if (!metaData.multipleAllowed && valueList != null && valueList.size > 1) {
                            return PostGenError(
                                "Field '${metaData.name}' is only allowed a single value; found '${it.value[0]}' in source file '$file.name'",
                                file.name,
                                metaData.name
                            )
                        }
                        val value = it.value[0]

                        when (metaData) {
                            PostMetaData.title -> {
                                post.title = value
                            }

                            PostMetaData.layout -> {
                                post.layout = value
                            }

                            PostMetaData.author -> {
                                post.author = value
                            }

                            PostMetaData.slug -> {
                                post.slug = value.trim()
                            }

                            PostMetaData.date -> {
                                val dateFormat = project.model["dateFormat"] as String? ?: "dd/MM/yyyy"
                                val formatter = DateTimeFormatter.ofPattern(dateFormat)
                                post.date = LocalDate.parse(value, formatter)
                            }

                            PostMetaData.tags -> {
                                // split string [tagA, tagB, tagC] into a list of three tags, removing spaces
                                post.tags.addAll(
                                    value.trim().drop(1).dropLast(1).split(",")
                                        .map { label -> Tag(label.trim(), label.trim(), label.trim().slug()) })
                            }

                            else -> {
                                println("How did i get here?")
                            }
                        }
                    }


                } else {
                    val finalVal = if (it.value.size == 1) it.value[0] else it.value
                    post.attributes[it.key] = finalVal
                }
            } // else do nothing? depends on the key I guess...
            return post
        }

        /**
         * Attempt to construct a post without any Yaml metadata. Can only provide a title, layout (always "post"), a slug and a date.
         * No author, tags or custom attributes
         */
        fun buildPostWithoutYaml(file: File, document: Document): PostStatus {
            val post = BasculePost(document)
            post.sourceFileName = file.name
            post.title = file.nameWithoutExtension

            // have to make an assumption about the layout - going for "post"
            post.layout = "post"
            // get the date from the file itself
            val filePath = file.toPath()
            val attributes = Files.readAttributes(filePath, BasicFileAttributes::class.java)
            val milliseconds = attributes.creationTime().toInstant()
            post.date = milliseconds.atZone(ZoneId.systemDefault()).toLocalDate()

            // transform the source file name to a cleaner URL
//			val slugRegex = Regex("[^a-zA-Z0-9-]")
//			post.slug = slugRegex.replace(file.nameWithoutExtension.toLowerCase(), "-")
            post.slug = file.nameWithoutExtension.slug()

            // author, tags will all be empty. No custom attributes
            return post
        }

    }

    override fun toString(): String {
        return "BasculePost: ${this.title}, slug:${this.slug}, layout:${this.layout}"
    }
}

/**
 * Enum representing all the key fields used to construct a BasculePost. Each field has it's own eligibility requirements,
 * 'required' and 'multipleAllowed'. E.g. the _title_ is required, and there must only be a single title.
 */
internal enum class PostMetaData(val required: Boolean, val multipleAllowed: Boolean) {
    title(true, false),
    layout(true, false),
    author(false, false),
    slug(false, false),
    date(false, false),
    tags(false, true),
    custom(false, true);

    companion object {
        fun contains(key: String): Boolean {
            for (md in values()) {
                if (md.name == key) return true
            }
            return false
        }
    }
}
