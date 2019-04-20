package org.liamjd.bascule.flexmark

import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.ext.media.tags.internal.AbstractMediaLink
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.NodeTracker
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence

class ExtendedEmbedTagsNodePostProcessor(options: DataHolder) : NodePostProcessor() {
	override fun process(state: NodeTracker?, node: Node?) {
		if (node is Link) {
			val previous = node.previous

			if (previous is Text) {
				val chars = previous.chars
				if (chars.isContinuedBy(node.chars)) {
					var mediaLink: AbstractMediaLink
					if (chars.endsWith(ExtendedEmbedLink.PREFIX) && !isEscaped(chars, ExtendedEmbedLink.PREFIX)) {
						mediaLink = ExtendedEmbedLink(node)
					} else {
						// none of the above; abort
						return
					}

					mediaLink.takeChildren(node)
					node.unlink()
					state?.nodeRemoved(node)
					previous.insertAfter(mediaLink)
					state?.nodeAddedWithChildren(mediaLink)
					previous.setChars(chars.subSequence(0, chars.length - mediaLink.getPrefix().length))
					if (previous.getChars().length == 0) {
						previous.unlink()
						state?.nodeRemoved(previous)
					}
				}
			}
		}
	}

	private fun isEscaped(chars: BasedSequence, prefix: String): Boolean {
		val backslashCount = chars.subSequence(0, chars.length - prefix.length).countTrailing('\\')
		return backslashCount and 1 != 0
	}

	class Factory(options: DataHolder) : NodePostProcessorFactory(false) {
		init {
			addNodes(Link::class.java)
		}

		override fun create(document: Document): NodePostProcessor {
			return ExtendedEmbedTagsNodePostProcessor(document)
		}
	}

}
