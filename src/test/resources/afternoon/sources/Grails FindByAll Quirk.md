---
title: Grails FindByAll Quirk with max
author: Liam Davison
layout: post
date: 11/06/2014
tags: [software development, cms, web, groovy]
slug: grails-findbyall
---
So this happened today:

```groovy
println("Page.list: " + Page.list().size())
println("Page.findAllBySite: " + Page.findAllBySite(siteInstance).size())
println("Page.findAllWhere: " + Page.findAllWhere(site:siteInstance).size())
println("Page.findAllBySite (max 4): " + Page.findAllBySite(siteInstance, [max: 4]).size())
println("Page.findAll(from Page...): " + Page.findAll("from Page as p where p.site=?", [siteInstance]).size())
println("Page.findAll(from Page... (max 4): " + Page.findAll("from Page as p where p.site=?", [siteInstance], [max: 4]).size())
```

Displays the following results:

```
Page.list: 7
Page.findAllBySite: 5
Page.findAllWhere: 5
Page.findAllBySite (max 4): 1
Page.findAll(from Page...): 5
Page.findAll(from Page... (max 4): 4
```

For some reason, adding the `[max:4]` metaParam in the `Page.findAllBySite` query returns the wrong list of results. Curiously, the following lines of code:

```groovy
def recentPageTemplates = PageTemplate.findAllBySite(siteInstance, [max: 4, sort: "lastUpdated", order: "desc"])
def recentSectionTemplates = SectionTemplate.findAllBySite(siteInstance, [max: 4, sort: "lastUpdated", order: "desc"])
```

Which do exactly the same thing, but for `PageTemplates` and `SectionTemplates` respectively, all work. This is in Grails 2.3.7.

The `Site` domain class has `static hasMany = [pageTemplates:PageTemplate, sectionTemplates:SectionTemplate, pages:Page]` and each of the three other domains, `Page, PageTemplate and SectionTemplate`, all have `static belongsTo = [site:Site]`. Any thoughts why `def recentPages = Page.findAllBySite(siteInstance, [sort: "lastUpdated", order: "desc", max: 4])` should be any different from the two template queries?