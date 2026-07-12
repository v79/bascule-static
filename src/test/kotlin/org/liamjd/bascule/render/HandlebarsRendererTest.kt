package org.liamjd.bascule.render

import org.liamjd.bascule.TestProjectYaml
import org.liamjd.bascule.lib.model.Project
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [HandlebarsRenderer]: template lookup by name, model binding, helper wiring and the
 * missing-template fallback. Templates are read from src/test/resources/handlebars/templates.
 */
class HandlebarsRendererTest {

    private val yamlConfig = TestProjectYaml.load("src/test/resources/handlebars/templates")

    private val project = Project(yamlString = yamlConfig)
    private val renderer = HandlebarsRenderer(project)

    @Test
    fun `renders a template substituting model values`() {
        val result = renderer.render(mapOf("name" to "World"), "greeting")
        assertEquals("Hello World!", result)
    }

    @Test
    fun `returns an empty string when the template is not found`() {
        val result = renderer.render(mapOf("name" to "World"), "does-not-exist")
        assertEquals("", result)
    }

    @Test
    fun `applies a registered string helper`() {
        // "upper" is registered from Handlebars StringHelpers in the renderer's init block
        val result = renderer.render(mapOf("name" to "bascule"), "upper")
        assertEquals("BASCULE", result)
    }

    @Test
    fun `wires the project date format into the localDate helper`() {
        // dateFormat from the YAML ("dd/MM/yyyy") should drive the custom localDate helper
        val result = renderer.render(mapOf("myDate" to LocalDate.of(2023, 1, 2)), "date")
        assertEquals("02/01/2023", result)
    }

    @Test
    fun `missing model value renders as empty`() {
        // Handlebars resolves an absent key to an empty string rather than throwing
        val result = renderer.render(emptyMap(), "greeting")
        assertEquals("Hello !", result)
    }
}
