package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.util.ast.Block
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.sequence.BasedSequence

class HydeTagBlock : Block {

	override fun getSegments(): Array<BasedSequence> {
		return Node.EMPTY_SEGMENTS
	}

	constructor()

	constructor(chars: BasedSequence) : super(chars)

	constructor(node: Node) : super() {
		appendChild(node)
		this.setCharsFromContent()
	}
}
