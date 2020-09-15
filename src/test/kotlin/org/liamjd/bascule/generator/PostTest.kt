package org.liamjd.bascule.generator

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.util.ast.Document
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.loadKoinModules
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val TITLE_VAL: String = "Four Weeks Sick Leave"
private const val AUTHOR_VAL = "Nessie the Monster"
private const val LAYOUT_POST_VAL = "post"
private const val SLUG_VAL = "four-weeks"
private const val POST_DATE_VAL = "01/01/2018"

class PostTest : Spek( {

	var data: MutableMap<String, MutableList<String>>

	// initialize the mock yaml visitor
	val mYamlVistor = mockk<AbstractYamlFrontMatterVisitor>()
	every { mYamlVistor.visit(any<Document>()) } just Runs
	// and inject the mock via Koin
	val koinModule = module {
		factory {
			mYamlVistor
		}
	}
	loadKoinModules(koinModule)

	// rest of the mocks
	val mFile = mockk<File>()
	val mPath = mockk<Path>()
	val mFileAttributes = mockk<BasicFileAttributes>()
	val mFileCreationTime = mockk<FileTime>()

	every { mFile.name } returns "Simple File (1).md"
	every { mFile.toPath()} returns mPath
	every { Files.readAttributes(mPath, BasicFileAttributes::class.java)} returns mFileAttributes
	every { mFileAttributes.creationTime()} returns mFileCreationTime
	every { mFileCreationTime.toInstant()} returns LocalDateTime.of(2018,Month.SEPTEMBER,14,8,0).toInstant(ZoneOffset.UTC)

	// some constants
	val mDocument = mockk<Document>()
	val project = Project(yaml.simple)

		describe("Can build a BasculePost object with various valid yaml frontispieces") {

		it("builds a simple BasculePost") {
			data = buildYamlData()
			every { mYamlVistor.data}.returns(data)

			val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
			val isPost = result is BasculePost
			assertTrue(isPost)
			val post = result as BasculePost
			assertEquals(TITLE_VAL, result.title)
			assertEquals(AUTHOR_VAL, result.author)
			assertEquals(LAYOUT_POST_VAL, result.layout)
			assertEquals(SLUG_VAL, result.slug)
			assertEquals(LocalDate.of(2018,Month.JANUARY,1),result.date)
			assertEquals(1,result.tags.size)
			assertEquals("aTag",result.tags?.first()?.label)
		}

		it("builds a BasculePost with two tags") {
			data = buildYamlData()
			data.put("tags", arrayListOf("[tagA,tagB]"))
			every { mYamlVistor.data}.returns(data)

			val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
			val isPost = result is BasculePost
			assertTrue(isPost)
			val post = result as BasculePost
			assertEquals(2,post.tags.size)
			assertEquals(1,post.tags?.filter { it.label == "tagA" }?.size)
			assertEquals(1,post.tags?.filter { it.label == "tagB" }?.size)
		}

		it("builds a post with two different custom tags") {
			val multiTagProject = Project(yaml.multiTag)
			data = buildYamlData()
			data.remove("tags")
			data.put("genres", mutableListOf("[classical,jazz]"))
			data.put("composers", mutableListOf("[beethoven,mahler,fitzgerald]"))
			every { mYamlVistor.data}.returns(data)

			val result = BasculePost.createPostFromYaml(mFile,mDocument,multiTagProject)
			val isPost = result is BasculePost
			assertTrue(isPost)
			val post = result as BasculePost
			assertEquals(5,post.tags.size)
			assertEquals(2,post.getTagsForCategory("genres")?.size)
			assertEquals(3,post.getTagsForCategory("composers")?.size)
		}

		it("builds a post with two custom attributes") {
			data = buildYamlData()
			data["wibble"] = mutableListOf("wobble")
			data["greep"] = mutableListOf("grump")
			every { mYamlVistor.data}.returns(data)

			val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
			val isPost = result is BasculePost
			assertTrue(isPost)
			val post = result as BasculePost
			assertEquals("wobble",result.attributes["wibble"])
			assertEquals("grump",result.attributes["greep"])
		}

		it("builds a post with a custom attribute list value") {
			data = buildYamlData()
			data["wibble"] = mutableListOf("wobble", "wumple")
			every { mYamlVistor.data}.returns(data)

			val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
			val isPost = result is BasculePost
			assertTrue(isPost)
			val post = result as BasculePost
			val attributes = result.attributes["wibble"] as List<*>
			assertEquals(2,attributes.size)
			assertEquals(listOf("wobble","wumple"),result.attributes["wibble"])
		}

			it("builds a post with a custom scalar (string) attribute in quotes") {
				data = buildYamlData()
				data["summary"] = mutableListOf("\"This string is quoted\"")
				every { mYamlVistor.data}.returns(data)

				val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
				val isPost = result is BasculePost
				assertTrue(isPost)
				val post = result as BasculePost
				val attributes = result.attributes["summary"] as String
				assertNotNull(attributes)
				assertEquals("This string is quoted",result.attributes["summary"])
			}

			it("builds a post with a custom scalar (string) attribute in quotes with escaped quotes") {
				data = buildYamlData()
				data["summary"] = mutableListOf("""
					"\"This string is quoted and escaped\""
				""".trimIndent())
				every { mYamlVistor.data}.returns(data)

				val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
				val isPost = result is BasculePost
				assertTrue(isPost)
				val post = result as BasculePost
				val attributes = result.attributes["summary"] as String
				assertNotNull(attributes)
				assertEquals("\\\"This string is quoted and escaped\\\"",result.attributes["summary"])
			}
	}

	describe("Returns an Error when a compulsory field is missing") {
		it("Does not have a title at all") {
			data = buildYamlData()
			data.remove("title")
			every { mYamlVistor.data}.returns(data)

			val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
			val isError = result is PostGenError
			assertTrue(isError)
			val error = result as PostGenError
			assertEquals("title",error.field)
		}
		it("Title is blank") {
			data = buildYamlData()
			data["title"] = arrayListOf("")
			every { mYamlVistor.data}.returns(data)

			val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
			val isError = result is PostGenError
			assertTrue(isError)
			val error = result as PostGenError
			assertEquals("title",error.field)
		}
	}

	describe("Returns an Error when a singleton field has multiple answers") {
		it("There are two layouts!") {
			data = buildYamlData()
			data["layout"] = mutableListOf("post","page","index")
			every { mYamlVistor.data}.returns(data)

			val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
			val isError = result is PostGenError
			assertTrue(isError)
			val error = result as PostGenError
			assertEquals("layout",error.field)

		}
	}

	describe("Can construct a post even without any yaml") {
		it("Makes assumptions based on the file name") {
			data = mutableMapOf()
			every { mYamlVistor.data}.returns(data)
			val result = BasculePost.createPostFromYaml(mFile,mDocument,project)
			val isPost = result is BasculePost
			assertTrue { isPost }
			val post = result as BasculePost
			assertEquals("Simple File (1)",post.title)
			assertEquals("",post.author)
			assertEquals("post",post.layout)
			assertNotNull(post.date)
			assertEquals(LocalDate.of(2018,Month.SEPTEMBER,14),post.date)
			assertTrue(post.tags.isEmpty())
			assertEquals("simple-file--1-",post.slug)
		}
	}

})

private fun buildYamlData(): MutableMap<String, MutableList<String>> {
	val data1 = mutableMapOf<String, MutableList<String>>()
	data1.put("title", mutableListOf(TITLE_VAL))
	data1.put("author", mutableListOf(AUTHOR_VAL))
	data1.put("layout", mutableListOf(LAYOUT_POST_VAL))
	data1.put("date", mutableListOf(POST_DATE_VAL))
	data1.put("tags", mutableListOf("[aTag]"))
	data1.put("slug", mutableListOf(SLUG_VAL))
	return data1
}

object yaml {
	val simple = """
		sitename: simpleDoc
		theme: simple-theme
	""".replace("\t","  ")

	val multiTag = """
		sitename: simpleDoc
		theme: simple-theme
		tagging: [composers,genres]
	""".replace("\t","  ")
}
