package org.liamjd.bascule

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Testing ground for setting up Bascule.
 */
@Deprecated("Just for testing. Don't use this", level = DeprecationLevel.HIDDEN)
class BasculeTester {

	val USERHOME = System.getProperty("user.home")!!
	val NUM_FILES = 10

	val logger = LoggerFactory.getLogger(this.javaClass)

	init {
		println("Welcome to BasculeTester")
		println("Enter the name of your project")
		val projectName = "wibble"

		println("Building $projectName...")
		println("USERHOME: $USERHOME")

		val tmpDir = createTempDir("bascule-", "-$projectName", File(USERHOME))
		println("Directory created at $tmpDir")



		println("Adding files...")
		val configFile = File(tmpDir.absolutePath, "bascule.cfg").bufferedWriter().use { out ->
			out.write(content.configString)
		}

		println("Faking a bunch of source md files")
		val sourceDir = File(tmpDir.absolutePath + "/sources")
		sourceDir.mkdirs()
		println("\tsources: $sourceDir")

		for (mdFile in 1..NUM_FILES) {
			val randomAuthor = (0..3).random()
			File(sourceDir.absolutePath, "source-$mdFile.md").bufferedWriter().use { out ->
				out.write(content.mdFileContent.format(content.nameArray[randomAuthor], mdFile))
			}
		}
		println("\t$NUM_FILES files created")
		println()

		println("Generating content...")
		val outputDir = File(tmpDir.absolutePath + "/output")
		outputDir.mkdir()

		val hbRenderer = Handlebars()

		val timeTaken = measureTimeMillis {
			sourceDir.walk().forEachIndexed { i, f ->
				if (!f.isDirectory) {

					/*f.forEachLine {line ->
						println(line)
						if(line.equals(content.SEPARATOR)) {
							println("separator found")
						}
					}*/

					val fileText = f.readText()
					val sourceFileName = f.nameWithoutExtension

//					println("Converting markdown to html")

					val options = MutableDataSet()
					options.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create()))
					val mdParser = Parser.builder(options).build()
					val mdRender = HtmlRenderer.builder(options).build()

//					 You can re-use parser and renderer instances
					val document = mdParser.parse(fileText)
					val yamlVisitor = AbstractYamlFrontMatterVisitor()
					yamlVisitor.visit(document)
					val url = yamlVisitor.data["url"]?.get(0)
					val html = mdRender.render(document)
//					println("yaml url -> $url")


					val random = (1..3).random()
					val model = mapOf(Pair("animal", content.animalArray.get(random)),
							Pair<String, String>("content", html), Pair<String, String>("uuu", url!!))

					val hbContext = Context.newBuilder(model).build()
					val template = hbRenderer.compileInline(content.htmlTemplate)
					val result = template.apply(hbContext)


					File(outputDir.absolutePath, "$sourceFileName-$url.html").bufferedWriter().use { out ->
						out.write(result)
//						println("\t\tGenerated $i -> ${model.get("animal")}; ")
						println("--> $sourceFileName-$url.html")
//						println(result)
					}
				}
			}
		}
		println("Time taken ${timeTaken}ms")

		println()
		println("Press Q to exit")
		readLine()
		tmpDir.run {
			deleteRecursively()
			deleteOnExit()
		}
		println("bye!")

	}


	object content {
		const val configString = "bascule-config: config\r\nauthor=liam"
		val mdFileContent = """
			---
			url: %s
			---
			## Once upon a time there were %d little animals

			The animals are very *cute* {.red}
		""".trimIndent()
		val htmlTemplate = """
			<html>
			<header><title>{{ animal }}</title></header>
			<body>
			<h1>This is a story about {{ animal }}</h1>
			<div>
				{{{ content }}}
			</div>
			<footer>
			This was page {{ uuu }}
			</footer>
			</body>
			</html>
		""".trimIndent()
		val animalArray = arrayListOf<String>("bears", "sheep", "pigs", "ducks")
		val nameArray = arrayListOf("liam", "susan", "bob", "angela")
		val SEPARATOR = "~~~~~"
	}
}
