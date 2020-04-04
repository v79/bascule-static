package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.MutableDataHolder

class HydeExtension : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
	override fun extend(parserBuilder: Parser.Builder?) {
		parserBuilder?.customBlockParserFactory(HydeTagBlockParser.Factory)
	}

	override fun parserOptions(options: MutableDataHolder?) {

	}

	override fun rendererOptions(options: MutableDataHolder) {

	}

	override fun extend(htmlRendererBuilder: HtmlRenderer.Builder, rendererType: String) {
		htmlRendererBuilder.nodeRendererFactory(HydeTagNodeRenderer.Factory)
	}

	companion object {
		fun create() = HydeExtension()
		val SOURCE_FOLDER = DataKey<String>("SOURCE_FOLDER", "/")
	}
}
