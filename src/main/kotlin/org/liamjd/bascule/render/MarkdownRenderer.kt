package org.liamjd.bascule.render

import com.vladsch.flexmark.util.ast.Document
import org.liamjd.bascule.model.BasculePost

interface MarkdownRenderer {

	fun renderHTML(post: BasculePost, itemCount: Int): Boolean

	fun renderMarkdown(document: Document): String
}
