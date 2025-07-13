package org.liamjd.bascule.model

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.util.ast.Document
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class BasculePostTest {

    private lateinit var mockDocument: Document
    private lateinit var mockMetadata: Map<String, Any>
    private lateinit var mockYamlVisitor: AbstractYamlFrontMatterVisitor
    private lateinit var mockFile: File

    @BeforeTest
    fun setup() {
      /*  // initialise the mock YAML visitor
        mockYamlVisitor = mockk<AbstractYamlFrontMatterVisitor>()
        every { mockYamlVisitor.visit(any<Document>()) } just Runs
        // and inject the mock via Koin
        val koinModule = module {
            factory { mockYamlVisitor }
        }
        loadKoinModules(koinModule)*/
    }

    @Test
    fun `can create a BasculePost from a Document`() {
        // setup

        // execute

        // verify
        assertEquals(false, 1 == 2, "This is a placeholder test to ensure the test framework is working correctly.")
    }

}