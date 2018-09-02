package org.liamjd.bascule.scanner

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.File
import java.io.InputStream
import kotlin.system.measureTimeMillis

class FolderWalker(val parent: File, val sources: String, val templates: String, val output: String, val assets: String) {

	private val sourcesDir: File
	private val templatesDir: File
	private val outputDir: File
	private val assetsDir: File

	val mdOptions = MutableDataSet()
	val mdParser: Parser

	init {
		println("FolderWalker initialised")
		sourcesDir = File(parent,sources)
		templatesDir = File(parent,templates)
		outputDir = File(parent,output)
		assetsDir = File(parent, assets)

		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create()))
		mdParser = Parser.builder(mdOptions).build()
	}

	// I wonder if coroutines can help with this?
	fun generate() {
		var numFiles = 0;

		println("Scanning ${sourcesDir.absolutePath} for markdown files")

		val timeTaken = measureTimeMillis {

			sourcesDir.walk().forEach {
				if(it.isDirectory) {
					// do something with directories?
				} else {
					numFiles++
					println("Scanning file ${it.name}")
					val model = mutableMapOf<String,Any>()
					val inputStream = it.inputStream()
					val inputFileName = it.nameWithoutExtension
					val inputExtension = it.extension

					val document = parseMarkdown(inputStream)

					val yamlVisitor = AbstractYamlFrontMatterVisitor()
					yamlVisitor.visit(document)
					yamlVisitor.data.forEach {
						model.put(it.key, it.value[0])
					}
					println("\t model is $model")

				}
			}
		}
		println("${timeTaken}ms to generate ${numFiles} files")
	}

	private fun parseMarkdown(inputStream: InputStream): Document {
		val text = inputStream.bufferedReader().readText()
		return mdParser.parse(text)
	}

}