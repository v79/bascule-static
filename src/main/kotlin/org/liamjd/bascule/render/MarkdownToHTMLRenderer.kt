package org.liamjd.bascule.render

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.model.BasculePost
import println.info

/**
 * Transforms a given [BasculePost] into the final HTML file through the Flexmark HTMLRenderer class,
 * and writes it to disc
 */
class MarkdownToHTMLRenderer(val project: Project) : KoinComponent, MarkdownRenderer {

    private val logger = KotlinLogging.logger {}

    private val fileHandler: BasculeFileHandler by inject { parametersOf() }
    private val renderer by inject<TemplatePageRenderer> { parametersOf(project) }

    val mdParser: Parser

    init {
        mdParser = Parser.builder(project.markdownOptions).build()
    }

    override fun renderHTML(post: BasculePost, itemCount: Int) {
        info("Rendering post ${post.sourceFileName}")
        logger.info { "Rendering post ${post.sourceFileName}" }
        render(project.model, post, itemCount)
    }


    // no performance improvement by making this a suspending function
    private fun render(siteModel: Map<String, Any>, basculePost: BasculePost, count: Int) {

        val model = mutableMapOf<String, Any?>()
        model.putAll(siteModel)
        model.putAll(basculePost.toModel())
        model.put("\$currentPage", basculePost.slug)

        // first, extract the content from the markdown
        val renderedMarkdown = renderMarkdown(basculePost.document)
        model.put("content", renderedMarkdown)

        // then, render the corresponding Handlebars template
        val templateFromYaml
                : String = basculePost.layout
        val renderedContent = renderer.render(model, templateFromYaml)
        basculePost.content = renderedMarkdown

        /*	val renderProgressBar = ProgressBar(label = "Rendering", animated = true, messageLine = basculePost.url)
            renderProgressBar.progress(count)*/

        fileHandler.createDirectories(basculePost.destinationFolder!!)
        fileHandler.writeFile(project.dirs.output.absoluteFile, basculePost.url, renderedContent)

    }

    override fun renderMarkdown(document: Document): String {
        val mdRender = HtmlRenderer.builder(project.markdownOptions).build()
        return mdRender.render(document)
    }
}
