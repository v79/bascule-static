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
            when (val resolved = resolveInclude(opts.get(HydeExtension.SOURCE_FOLDER), fileUrl.toString())) {
                is IncludeResolution.NotFound -> html.tag("em").text("${resolved.fileName} not found").closeTag("em")
                is IncludeResolution.Found -> when (node.tag.toString()) {
                    "include" -> {
                        html.indent().append(resolved.content)
                    }

                    "md" -> {
                        html.indent()
                            .append("markdown source file found; attempting to parse... (existing chars: ${node.chars}")
                        context.renderChildren(node)
                    }
                }
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

/**
 * The outcome of resolving a Hyde transclusion tag against the source folder: either the referenced
 * file was [Found] (carrying its content) or it was [NotFound] (carrying the file name for the error
 * message). Kept separate from the renderer so the file-resolution logic can be unit-tested without
 * an [HtmlWriter].
 */
internal sealed class IncludeResolution {
    data class Found(val content: String) : IncludeResolution()
    data class NotFound(val fileName: String) : IncludeResolution()
}

/**
 * Resolve the [fileUrl] referenced by a Hyde tag relative to [sourceFolder], returning its content if
 * the file exists or its name if it does not.
 */
internal fun resolveInclude(sourceFolder: String?, fileUrl: String): IncludeResolution {
    val sourceFile = File(File(sourceFolder ?: "/"), fileUrl)
    return if (sourceFile.exists()) {
        IncludeResolution.Found(sourceFile.readText())
    } else {
        IncludeResolution.NotFound(sourceFile.name)
    }
}
