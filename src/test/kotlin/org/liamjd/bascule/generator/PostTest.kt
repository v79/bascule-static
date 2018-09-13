package org.liamjd.bascule.generator

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.loadKoinModules
import org.liamjd.bascule.assets.ProjectStructure
import java.io.File
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertEquals
import kotlin.test.assertTrue


private val TITLE: String = "Four Weeks Sick Leave"
private val AUTHOR = "Nessie the Monster"
private val LAYOUT_POST = "post"
private val SLUG = "four-weeks"
private val POST_DATE = "01/01/2018"

class PostTest : Spek( {

	describe("Can build a Post object with various valid yaml frontispieces") {

		val mYamlVistor = mockk<AbstractYamlFrontMatterVisitor>()
		var data = mutableMapOf<String,MutableList<String>>()
		every { mYamlVistor.visit(any<Document>()) } just Runs


		val koinModule = module {
			factory {
				mYamlVistor as AbstractYamlFrontMatterVisitor
			}
		}

		loadKoinModules(koinModule)

		val mRoot = mockk<File>()
		val mOutputDir = mockk<File>()
		val mSourceDir = mockk<File>()
		val mAssetsDir = mockk<File>()
		val mTemplatesDir = mockk<File>()

		it("builds a simple Post") {

			data = buildYamlData(data)
			every { mYamlVistor.data}.returns(data)

			val fileName = "simple-file.md"
			val mDocument = mockk<Document>()
			val yamlString = ""
			val project = ProjectStructure("simpleDoc",mRoot,mSourceDir,mOutputDir,mAssetsDir,mTemplatesDir,yamlString,"simple-theme")

			val result = Post.Builder.createPostFromYaml(fileName,mDocument,project)
			val isPost = result is Post
			assertTrue(isPost)
			val post = result as Post
			assertEquals(TITLE, result.title)
			assertEquals(AUTHOR, result.author)
			assertEquals(LAYOUT_POST, result.layout)
			assertEquals(SLUG, result.slug)
			assertEquals(LocalDate.of(2018,Month.JANUARY,1),result.date)
			assertEquals(1,result.tags.size)
			assertEquals("aTag",result.tags[0])
		}

		it("builds a Post with two tags") {

			data = buildYamlData(data)
			data.put("tags", arrayListOf("[tagA,tagB]"))
			every { mYamlVistor.data}.returns(data)

			val fileName = "simple-file.md"
			val mDocument = mockk<Document>()
			val yamlString = ""
			val project = ProjectStructure("simpleDoc",mRoot,mSourceDir,mOutputDir,mAssetsDir,mTemplatesDir,yamlString,"simple-theme")

			val result = Post.Builder.createPostFromYaml(fileName,mDocument,project)
			val isPost = result is Post
			assertTrue(isPost)
			val post = result as Post
			assertEquals(2,post.tags.size)
			assertEquals("tagA",post.tags[0])
			assertEquals("tagB",post.tags[1])
		}
	}
})



private fun buildYamlData(data: MutableMap<String, MutableList<String>>): MutableMap<String, MutableList<String>> {
	var data1 = data
	data1 = mutableMapOf()
	data1.put("title", mutableListOf(TITLE))
	data1.put("author", mutableListOf(AUTHOR))
	data1.put("layout", mutableListOf(LAYOUT_POST))
	data1.put("date", mutableListOf(POST_DATE))
	data1.put("tags", mutableListOf("[aTag]"))
	data1.put("slug", mutableListOf(SLUG))
	return data1
}