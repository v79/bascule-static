package org.liamjd.bascule.scanner

import org.liamjd.bascule.cache.HandlebarsTemplateCacheItem
import org.liamjd.bascule.cache.MDCacheItem
import java.io.File
import java.time.*
import java.time.temporal.TemporalUnit


object TEST_DATA {
	val post_template_modification_date = LocalDateTime.of(2019,Month.NOVEMBER,28,16,50,0)
	val post_template_modification_date_MILLIS = post_template_modification_date.toInstant(ZoneId.systemDefault().rules.getOffset(LocalDateTime.now())).toEpochMilli()

	val sources_absolute_path: String = "test/sources/path"
	val test_project_name = "test-project"

	val big_bang_title = "Review of Big Bang by Simon Singh"
	val big_bang_url = "2005/review-of-big-bang.html"
	val big_bang_date = LocalDate.of(2005, Month.OCTOBER, 8)
	val big_bang_json = """
	{
		"layouts": [
			{
				"layoutName": "post",
				"layoutFilePath": "D:\\Development\\liamjdavison\\liamjd-theme\\templates\\post.hbs",
				"layoutFileSize": 3464,
				"layoutModificationDate": "2019-11-24T10:25:10.335"
			}
		],
		"items": [
			{
			"sourceFileSize": 1061,
			"sourceFilePath": "D:\\Development\\liamjdavison\\sources\\2005\\Review of Big Bang.md",
			"sourceModificationDate": "2018-09-11T21:32:30.342",
			"link": {
				"title": "Review of Big Bang by Simon Singh",
				"url": "2005/review-of-big-bang.html",
				"date": 1128726000
			},
			"tags": [
			],
			"previous": null,
			"next": {
				"title": "Single Syllable Story",
				"url": "2005/single-syllable-story.html",
				"date": 1128726000
			},
			"layout": "post",
			"rerender": true
		}
		]
	}
	""".trimIndent()

	val bigBangItem = MDCacheItem(12345L, big_bang_url, big_bang_date.atStartOfDay())

	val mdCacheItemSet = setOf(bigBangItem)
	val mdCacheItemEmptySet = emptySet<MDCacheItem>()

	val hbTemplateCacheEmptySet = emptySet<HandlebarsTemplateCacheItem>()
	val hbTemplatePostItem = HandlebarsTemplateCacheItem("post","D:\\post.hbs",234L, post_template_modification_date)

	val singleFileList = arrayOf<File>()
}

object REVIEW_BIG_BANG_CACHE {

	val json = """
		[
    {
        "sourceFileSize": 1061,
        "sourceFilePath": "D:\\Development\\liamjdavison\\sources\\2005\\Review of Big Bang.md",
        "sourceModificationDate": "2018-09-11T21:32:30.342",
        "link": {
            "title": "Review of Big Bang by Simon Singh",
            "url": "2005/review-of-big-bang.html",
            "date": 1128726000
        },
        "tags": [
        ],
        "previous": null,
        "next": {
            "title": "Single Syllable Story",
            "url": "2005/single-syllable-story.html",
            "date": 1128726000
        },
        "layout": "post",
        "rerender": true
    }
	]
	""".trimIndent()
}

fun millsToLocalDateTime(millis: Long): LocalDateTime {
	val instant: Instant = Instant.ofEpochMilli(millis)
	return instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
}

object REVIEW_BIG_BANG {
	val markDownSource = """
		---
title: Review of Big Bang by Simon Singh
author: Liam Davison
layout: post
date: 08/10/2005
tags: [books, science]
slug: review-of-big-bang
isbn: 9781231231234
---

Although I promised myself that I wouldn't read anything that isn't Scottish - my job demands that I improve my knowledge of Scottish literature - I couldn't help but pick up and read Simon Singh's _Big Bang_, a history of how the Big Bang theory of the universe came to be. It's a long book, almost 500 pages, but very easy to read and it only took me a few days to finish it. I've read quite a bit of science books, so much of it was retreading old ground, but Singh writes in an easy, informal way, and he does like to explore the back-stories and personal characters of the scientists and explorers who have lead, from Ptolemy's Earth-centric view, to Copernicus, to Einstein, Hubble and beyond.

It's not rocket science, and it's not going to stretch your mind, but as an introduction to the Big Bang theory - and an introduction to how science works - I recommend it highly.
	""".trimIndent()
}
