package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ast.util.Parsing
import com.vladsch.flexmark.parser.block.*
import com.vladsch.flexmark.util.ast.Block
import com.vladsch.flexmark.util.ast.BlockContent
import com.vladsch.flexmark.util.options.DataHolder

class HydeTagBlockParser(options: DataHolder?) : AbstractBlockParser() {

	private var block = HydeTagBlock()
	private var content: BlockContent? = BlockContent()

	override fun tryContinue(state: ParserState?): BlockContinue? {
		return BlockContinue.none()
	}


	override fun getBlock(): Block = block

	override fun closeBlock(state: ParserState?) {
		block.setContent(content)
		content = null
	}

	object Factory : CustomBlockParserFactory {
		override fun getBeforeDependents(): MutableSet<out Class<Any>>? {
			return null
		}

		override fun getAfterDependents(): MutableSet<out Class<Any>>? {
			return null
		}

		override fun affectsGlobalScope() = false

		override fun create(options: DataHolder?): BlockParserFactory {
			return BlockFactory(options)
		}
	}

	class BlockFactory(options: DataHolder?) : AbstractBlockParserFactory(options) {

		private var parsing: HydeTagParsing
		private var opts: DataHolder?

		init {
			this.parsing = HydeTagParsing(Parsing(options))
			opts = options
		}

		override fun tryStart(state: ParserState?, matchedBlockParser: MatchedBlockParser?): BlockStart? {
			val line = state?.getLine()
			val currentIndent = state?.getIndent()
			if (currentIndent == 0 && matchedBlockParser?.getBlockParser()?.block !is Paragraph) {
				val tryLine = line?.subSequence(state.getIndex())
				val matcher = parsing.MACRO_OPEN.matcher(tryLine)

				if (matcher.find()) {
					// see if it closes on the same line, then we create a block and close it
					val tag = tryLine?.subSequence(0, matcher.end())
					val tagName = line?.subSequence(matcher.start(1), matcher.end(1))
					val parameters = tryLine?.subSequence(matcher.end(1), matcher.end() - 2)?.trim()

					when(tagName.toString()) {
						"include" -> {
							val tagNode = tag?.endSequence(2)?.let { HydeTag(tag.subSequence(0, 2), tagName, parameters, it) }
							tagNode?.setCharsFromContent()

							val parser = HydeTagBlockParser(state.getProperties())
							parser.block.appendChild(tagNode)

							return BlockStart.of(parser)
									.atIndex(state.getLineEndIndex())
						}
					}

				}
			}
			return BlockStart.none()
		}

	}
}

