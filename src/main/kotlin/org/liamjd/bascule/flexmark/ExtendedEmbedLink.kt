package org.liamjd.bascule.flexmark

import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ext.media.tags.internal.AbstractMediaLink
import com.vladsch.flexmark.util.ast.Block

class ExtendedEmbedLink : AbstractMediaLink {

	constructor() : super(PREFIX, TYPE) {}

	constructor(other: Link) : super(PREFIX, TYPE, other) {}

	companion object {

		const val PREFIX = "!§"
		const val TYPE = "ExtendedEmbed"
	}

	// This class leaves room for specialization, should we need it.
	// Additionally, it makes managing different Node types easier for users.
}
