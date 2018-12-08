---
title: Introducing Cinnabar, a work-in-progress CMS
author: Liam Davison
layout: basculePost
date: 01/03/2014
tags: [software development, cms, web, groovy]
slug: introducing-cinnabar
---
I'm a software developer. This is a bit of a surprise for me, as it's a bit of a career change after years of working in book selling and book publishing. Two years ago I took a chance on a vaguely-worded job advert, and became an "ICT Specialist" working for the Scottish Government. It's been a good move for me, and I'm glad I made the change. Working for such a large organisation, and on such a big project, is worlds away from the small Scottish book publishing world I was familiar with.

Though perhaps I'm being a little disingenuous - I did study computer science at University, but when I graduated I vowed that I did not want to be a programmer. Well, things change. I'm not going to go into the details of what I do for SG (our familiar term for the Scottish Government), but briefly: I'm working on integrating a powerful content management system called [FirstSpirit](https://www.e-spirit.com/en/ "FirstSpirit CMS") into the somewhat arcane _JBoss Portal Platform_ as part of a major new system for the _Agriculture, Food and Rural Communities_ directorate. There's not a lot of Java development in my current role, and I've found myself itching to some real programming. Inspired by the FirstSpirit system, I've decided to build a web-based CMS of my own.

I've also decided to switch platform - away from Java, JSF2 and web services, to [Groovy and Grails](http://grails.org/ "Groovy and Grails"). I've found, to my surprise, that I'm really enjoying working with Groovy and Grails. It's a rapid development framework that doesn't bog you down in configuration, XML and setup. It hides the faff around Spring injection. It makes logical choices to map your domain objects to database tables, using Hibernate. It cleanly maps web page parameters to web controllers, without the acres of annotations that Spring MVC relies on. It's fun!

My project is called **Cinnabar**. With it you can create a site, which is comprised of a number of pages based on page templates. Those page templates are split into input components - such as text boxes, images, date fields and so on. I've been focusing on getting the framework and the data model in place, so it doesn't actually do anything yet. I'm trying to do things in small parts, and getting them working correctly, before moving on to the next part. Though I do have a big picture in mind, I don't want to do too much at once.

Why "Cinnabar"? I'm not really sure. I like the word, which is the name of the ore what mercury is extracted from. (There's a tenuous connection there; Mercury, [messenger of the Gods in Roman mythology](http://en.wikipedia.org/wiki/Mercury_(mythology)); a website is a communication tool; my [Cinnabar](http://en.wikipedia.org/wiki/Cinnabar) is the source of  communication... something like that. I'm not too worried about it!). The logo is the Cinnabar moth, chosen mostly because it's a nice red colour and looks a lot nicer than a lump of ore.

My much neglected blog needs updating more regularly, so I intend to discuss the development of Cinnabar and my experiences with Groovy and Grails here. Hopefully doing so will also encourage me to write about the other things this blog promised to discuss, such as books and politics.
