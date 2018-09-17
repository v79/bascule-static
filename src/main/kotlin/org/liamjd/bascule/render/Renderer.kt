package org.liamjd.bascule.render

interface Renderer {
	fun render(model: Map<String, Any?>, templateName: String) : String
}
