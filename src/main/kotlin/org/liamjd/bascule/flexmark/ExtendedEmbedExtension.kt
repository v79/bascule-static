package org.liamjd.bascule.flexmark

import com.vladsch.flexmark.ext.media.tags.internal.MediaTagsNodePostProcessor
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.DataKey
import com.vladsch.flexmark.util.options.MutableDataHolder

class ExtendedEmbedExtension : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

	override fun extend(parserBuilder: Parser.Builder?) {
		parserBuilder?.postProcessorFactory(ExtendedEmbedTagsNodePostProcessor.Factory(parserBuilder))
	}

	override fun extend(rendererBuilder: HtmlRenderer.Builder?, rendererType: String?) {
		rendererBuilder?.nodeRendererFactory(ExtendedEmbedTagsNodeRenderer.Factory())
	}

	override fun parserOptions(options: MutableDataHolder?) {
	}

	override fun rendererOptions(options: MutableDataHolder?) {
	}

	companion object {
		fun create(): ExtendedEmbedExtension { return ExtendedEmbedExtension()}

		val SOURCE_FOLDER = DataKey<String>("SOURCE_FOLDER","/")
	}

}
