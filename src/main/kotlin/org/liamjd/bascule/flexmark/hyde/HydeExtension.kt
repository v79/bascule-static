package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.DataKey
import com.vladsch.flexmark.util.options.MutableDataHolder

class HydeExtension : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
	override fun extend(parserBuilder: Parser.Builder?) {
		parserBuilder?.customBlockParserFactory(HydeTagBlockParser.Factory)
	}

	override fun extend(rendererBuilder: HtmlRenderer.Builder?, rendererType: String?) {
		rendererBuilder?.nodeRendererFactory(HydeTagNodeRenderer.Factory)
	}

	override fun parserOptions(options: MutableDataHolder?) {
	}

	override fun rendererOptions(options: MutableDataHolder?) {
	}

	companion object {
		fun create() = HydeExtension()
		val SOURCE_FOLDER = DataKey<String>("SOURCE_FOLDER","/")
	}
}
