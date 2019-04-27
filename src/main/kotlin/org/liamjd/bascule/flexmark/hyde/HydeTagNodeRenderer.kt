package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.html.CustomNodeRenderer
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.util.options.DataHolder
import org.liamjd.bascule.flexmark.ExtendedEmbedExtension
import java.io.File
import java.util.*

class HydeTagNodeRenderer(options: DataHolder) : NodeRenderer {

	val opts = options
	override fun getNodeRenderingHandlers(): MutableSet<NodeRenderingHandler<*>> {
		val set = HashSet<NodeRenderingHandler<*>>()

		set.add(NodeRenderingHandler<HydeTag>(HydeTag::class.java, CustomNodeRenderer<HydeTag>( { node, context, html -> this.render(node, context, html) })))

		set.add(NodeRenderingHandler<HydeTagBlock>(HydeTagBlock::class.java, CustomNodeRenderer<HydeTagBlock>( { node, context, html -> this.render(node, context, html) })))

		return set
	}

	// render block
	private fun render(node: HydeTagBlock, context: NodeRendererContext, html: HtmlWriter) {
		context.renderChildren(node)
	}

	// render tag
	private fun render(node: HydeTag, context: NodeRendererContext, html: HtmlWriter) {
		html.line()
		val fileUrl = node.parameters

		if(fileUrl.isNotNull) {
			val sourceFile = File(File(opts.get(ExtendedEmbedExtension.SOURCE_FOLDER)),fileUrl.toString())
			if(sourceFile.exists()) {
				val stream = sourceFile.inputStream()
				html.indent().append(stream.bufferedReader().readText())
			} else {
				html.tag("em").text("${sourceFile.name} not found").closeTag("em")
			}
		}
		html.line()
	}

	object Factory : NodeRendererFactory {
		override fun create(options: DataHolder): NodeRenderer {
			return HydeTagNodeRenderer(options)
		}
	}
}
