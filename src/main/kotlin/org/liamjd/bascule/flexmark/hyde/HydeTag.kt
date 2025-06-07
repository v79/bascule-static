package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.sequence.BasedSequence
import java.lang.StringBuilder

/**
 * This is inspired by the jekyll-extension from https://github.com/vsch/flexmark-java/tree/master/flexmark-ext-jekyll-tag - pretty much the same thing
 */
class HydeTag : Node {
	var openingMarker = BasedSequence.NULL
	var tag = BasedSequence.NULL
	var parameters = BasedSequence.NULL
	var closingMarker = BasedSequence.NULL

	override fun getSegments(): Array<BasedSequence> {
		//return EMPTY_SEGMENTS;
		return arrayOf(openingMarker, tag, parameters, closingMarker)
	}

	override fun getAstExtra(out: StringBuilder) {
		Node.segmentSpanChars(out, openingMarker, "open")
		Node.segmentSpanChars(out, tag, "tag")
		Node.segmentSpanChars(out, parameters, "parameters")
		Node.segmentSpanChars(out, closingMarker, "close")
	}

	constructor(chars: BasedSequence) : super(chars)

	// can I make this more null safe
	constructor(openingMarker: BasedSequence?, tag: BasedSequence?, parameters: BasedSequence?, closingMarker: BasedSequence) : super(
		openingMarker?.baseSubSequence(openingMarker.startOffset, closingMarker.endOffset)!!
	) {

		this.openingMarker = openingMarker
		this.tag = tag
		this.parameters = parameters
		this.closingMarker = closingMarker
	}


}
