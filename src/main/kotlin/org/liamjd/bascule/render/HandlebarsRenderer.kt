package org.liamjd.bascule.render

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.context.FieldValueResolver
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.context.MethodValueResolver
import org.liamjd.bascule.assets.ProjectStructure

class HandlebarsRenderer(val project: ProjectStructure) : Renderer {

	val TEMPLATE_SUFFIX = ".hbt"
	val hbRenderer: Handlebars
	val dateFormat: String

	init {
		hbRenderer = Handlebars()
		hbRenderer.registerHelper("forEach", ForEachHelper())
		hbRenderer.registerHelper("paginate", Paginate())
		dateFormat = project.yamlMap["dateFormat"] as String? ?: "dd/MMM/yyyy"
		hbRenderer.registerHelper("localDate", LocalDateFormatter(dateFormat))
	}

	override fun render(model: Map<String, Any?>, templateName: String): String {

		val hbContext = Context.newBuilder(model).resolver(MethodValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, MapValueResolver.INSTANCE, FieldValueResolver.INSTANCE).build()
		val template = hbRenderer.compileInline(getTemplate(project, templateName))

		return template.apply(hbContext)
	}

	private fun getTemplate(project: ProjectStructure, templateName: String): String {
		val matches = project.templatesDir.listFiles { dir, name -> name == templateName + TEMPLATE_SUFFIX }

		if (matches.isNotEmpty() && matches.size == 1) {
			val found = matches[0]
			return found.readText()
		}
		println.error("ERROR - template file '$templateName' not found - unable to generate content.")
		return ""
	}
}
