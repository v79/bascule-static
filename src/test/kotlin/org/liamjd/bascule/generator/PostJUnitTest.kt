package org.liamjd.bascule.generator

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.util.ast.Document
import io.mockk.mockk
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.lib.model.Project
import java.io.File
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TITLE_VAL: String = "Four Weeks Sick Leave"
private const val AUTHOR_VAL = "Nessie the Monster"
private const val LAYOUT_POST_VAL = "post"
private const val SLUG_VAL = "four-weeks"
private const val POST_DATE_VAL = "01/01/2018"

class PostJUnitTest {

	var data: MutableMap<String, MutableList<String>> = buildYamlData()
	val mYamlVistor = mockk<AbstractYamlFrontMatterVisitor>()

	val mFile = mockk<File>()
	val mPath = mockk<Path>()
	val mFileAttributes = mockk<BasicFileAttributes>()
	val mFileCreationTime = mockk<FileTime>()

	// some constants
	val mDocument = mockk<Document>()
	val project = Project(test_yaml.simple)

	@Test
	fun `can build a simple BasculePost with basic yaml frontispiece`() {


		val result = BasculePost.createPostFromYaml(mFile, mDocument, project)
		val isPost = result is BasculePost
		assertTrue(isPost)
		val post = result as BasculePost
		assertEquals(TITLE_VAL, post.title)
		assertEquals(AUTHOR_VAL, result.author)
		assertEquals(LAYOUT_POST_VAL, result.layout)
		assertEquals(SLUG_VAL, result.slug)
		assertEquals(LocalDate.of(2018, Month.JANUARY, 1), result.date)
		assertEquals(1, result.tags.size)
		assertEquals("aTag", result.tags.first()?.label)

	}
}

private fun buildYamlData(): MutableMap<String, MutableList<String>> {
	val data1 = mutableMapOf<String, MutableList<String>>()
	data1["title"] = mutableListOf(TITLE_VAL)
	data1["author"] = mutableListOf(AUTHOR_VAL)
	data1["layout"] = mutableListOf(LAYOUT_POST_VAL)
	data1["date"] = mutableListOf(POST_DATE_VAL)
	data1["tags"] = mutableListOf("[aTag]")
	data1["slug"] = mutableListOf(SLUG_VAL)
	return data1
}

object test_yaml {
	val simple = """
		sitename: simpleDoc
		theme: simple-theme
	""".replace("\t", "  ")

	val multiTag = """
		sitename: simpleDoc
		theme: simple-theme
		tagging: [composers,genres]
	""".replace("\t", "  ")
}
