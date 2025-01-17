package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.util.data.DataHolder
import java.io.File
import java.util.*

class HydeTagNodeRenderer(options: DataHolder) : NodeRenderer {

    val opts = options
    override fun getNodeRenderingHandlers(): MutableSet<NodeRenderingHandler<*>> {
        val set = HashSet<NodeRenderingHandler<*>>()

        set.add(
            NodeRenderingHandler(
                HydeTag::class.java
            ) { node, context, html ->
                this.render(
                    node,
                    context,
                    html
                )
            })

        set.add(
            NodeRenderingHandler(
                HydeTagBlock::class.java
            ) { node, context, html ->
                this.render(
                    node,
                    context,
                    html
                )
            })

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

        if (fileUrl.isNotNull) {
            val sourceFile = File(File(opts.get(HydeExtension.SOURCE_FOLDER)), fileUrl.toString())
            if (sourceFile.exists()) {

                when (node.tag.toString()) {
                    "include" -> {
                        val stream = sourceFile.inputStream()
                        html.indent().append(stream.bufferedReader().readText())
                    }

                    "md" -> {
                        html.indent()
                            .append("markdown source file found; attemping to parse... (existing chars: ${node.chars}")
                        val stream = sourceFile.inputStream()

//						node.appendChild(stream.bufferedReader().readText())

                        context.renderChildren(node)
                    }
                }


            } else {
                html.tag("em").text("${sourceFile.name} not found").closeTag("em")
            }
        }
        html.line()
    }

    object Factory : NodeRendererFactory {

        override fun apply(options: DataHolder): NodeRenderer {
            return HydeTagNodeRenderer(options)

        }
    }
}
