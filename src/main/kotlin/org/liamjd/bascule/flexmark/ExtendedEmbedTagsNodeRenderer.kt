package org.liamjd.bascule.flexmark

import com.vladsch.flexmark.html.CustomNodeRenderer
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.util.options.DataHolder
import java.io.File

class ExtendedEmbedTagsNodeRenderer(options: DataHolder) : NodeRenderer {

	val opts = options

	override fun getNodeRenderingHandlers(): MutableSet<NodeRenderingHandler<*>> {
		val self = this
		val set = HashSet<NodeRenderingHandler<*>>()

		set.add(NodeRenderingHandler(ExtendedEmbedLink::class.java, CustomNodeRenderer { node, context, html -> self.renderExtendedEmbedLink(node, context, html) }))

		return set
	}


	class Factory : NodeRendererFactory {
		override fun create(options: DataHolder): NodeRenderer {
			return ExtendedEmbedTagsNodeRenderer(options)
		}
	}

	private fun renderExtendedEmbedLink(node: ExtendedEmbedLink, context: NodeRendererContext, html: HtmlWriter) {
		html.line()
		val fileUrl = node.url

		if(fileUrl.isNotNull) {
			val sourceFile = File(File(opts.get(ExtendedEmbedExtension.SOURCE_FOLDER)),fileUrl.toString())
			val stream = sourceFile.inputStream()
			html.indent().append(stream.bufferedReader().readText())
		}

		html.line()
	}
}
