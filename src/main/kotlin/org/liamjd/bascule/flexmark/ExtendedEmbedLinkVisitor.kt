package org.liamjd.bascule.flexmark

import com.vladsch.flexmark.util.ast.VisitHandler
import com.vladsch.flexmark.util.ast.Visitor

interface ExtendedEmbedLinkVisitor {
	fun visit(node: ExtendedEmbedLink)
}

object EmbedLinkVisitorExt {
	fun <V : ExtendedEmbedLinkVisitor> VISIT_HANDLERS(visitor: V): Array<VisitHandler<*>> {
		return arrayOf(VisitHandler(ExtendedEmbedLink::class.java, Visitor { node -> visitor.visit(node) }))
	}
}
