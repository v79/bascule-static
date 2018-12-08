package org.liamjd.bascule.render

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.context.FieldValueResolver
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.context.MethodValueResolver
import com.github.jknack.handlebars.helper.StringHelpers
import com.github.jknack.handlebars.io.FileTemplateLoader
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.Renderer

class HandlebarsRenderer(val project: Project) : Renderer {

	val TEMPLATE_SUFFIX = ".hbs"
	val hbRenderer: Handlebars
	val dateFormat: String
	val loader: FileTemplateLoader

	init {
		loader = FileTemplateLoader(project.templatesDir)
		dateFormat = project.yamlMap["dateFormat"] as String? ?: "dd/MMM/yyyy"

		hbRenderer = Handlebars(loader)
		hbRenderer.registerHelper("forEach", ForEachHelper())
		hbRenderer.registerHelper("paginate", Paginate())
		hbRenderer.registerHelper("localDate", LocalDateFormatter(dateFormat))
		hbRenderer.registerHelper("capitalize",StringHelpers.capitalize)
		hbRenderer.registerHelper("upper",StringHelpers.upper)
		hbRenderer.registerHelper("slugify", StringHelpers.slugify)

	}

	override fun render(model: Map<String, Any?>, templateName: String): String {
		val hbContext = Context.newBuilder(model).resolver(MethodValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, MapValueResolver.INSTANCE, FieldValueResolver.INSTANCE).build()
		val template = hbRenderer.compileInline(getTemplateText(project, templateName))

		return template.apply(hbContext)
	}

	private fun getTemplateText(project: Project, templateName: String): String {
		val matches = project.templatesDir.listFiles { dir, name -> name == (templateName  + TEMPLATE_SUFFIX) }

		if (matches.isNotEmpty() && matches.size == 1) {
			val found = matches[0]
			return found.readText()
		}
		println.error("ERROR - template file '$templateName' not found - unable to generate content.")
		return ""
	}
}
