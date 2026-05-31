package org.liamjd.bascule.scanner

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.fileModule
import org.liamjd.bascule.generationModule
import org.liamjd.bascule.lib.model.Project
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Verifies that the Koin factories for the scanner classes still resolve after the move to constructor
 * injection (Step 1 of the testability refactor). Compilation alone does not prove that
 * `get { parametersOf(project) }` wires up correctly at runtime.
 */
class ScannerKoinWiringTest {

	private val yamlConfig = """
        siteName: Liam John Davison
        dateFormat: "dd/MM/yyyy"
        dateTimeFormat: HH:mm:ss dd/MM/yyyy
        author: Liam Davison
        theme: liamjd-theme
        postsPerPage: 10
        directories:
          source: sources
          output: site
          assets: assets
          templates: liamjd-theme/templates
          generators: [IndexPageGenerator, PostNavigationGenerator, TaxonomyNavigationGenerator]
    """.trimIndent()

	private val project = Project(yamlConfig = yamlConfig)
	private lateinit var koin: Koin

	@BeforeEach
	fun setUp() {
		koin = startKoin { modules(fileModule, generationModule) }.koin
	}

	@AfterEach
	fun tearDown() = stopKoin()

	@Test
	fun `PostBuilder resolves from Koin`() {
		assertNotNull(koin.get<PostBuilder> { parametersOf(project) })
	}

	@Test
	fun `ChangeSetCalculator resolves from Koin with its fileHandler and postBuilder`() {
		assertNotNull(koin.get<ChangeSetCalculator> { parametersOf(project) })
	}

	@Test
	fun `MarkdownScanner resolves from Koin with its full dependency chain`() {
		// This is the deepest chain: MarkdownScanner -> BasculeCache + ChangeSetCalculator -> PostBuilder + FileHandler
		assertNotNull(koin.get<MarkdownScanner> { parametersOf(project) })
	}

	@Test
	fun `PostBuilder can also be constructed directly with a FileHandler, no Koin needed`() {
		// the point of the refactor: no global container required to build the object
		val postBuilder = PostBuilder(project, BasculeFileHandler())
		assertNotNull(postBuilder)
	}
}
