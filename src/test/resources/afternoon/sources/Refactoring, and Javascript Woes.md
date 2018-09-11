---
title: Refactoring, and Javascript Woes
author: Liam Davison
layout: post
date: 09/03/2014
tags: [software development, cms, web, groovy]
slug: refactoring
---
For the past few days I've had a nagging thought at the back of my mind regarding Cinnabar. I had deliberately ignored one of the components I had planned to build, _content areas_, while I worked on getting a rather tricky many-to-many join table working. That table is now working, though there is some ugly HQL database queries that I'll need to revisit later. So this week I started to add my code to support _content areas_. A page template will have zero or more input components (such as text or date fields), and then zero or more content areas. Those content areas can each have 1 or more input components of their own.

Before I started, I decided to integrate my codebase into an SVN repository. I'm still surprised how few SVN tools there are for Windows, so I decided to go online - _SilkSVN_ offers 100Mb of space for a single developer for free, so I've committed and branched my code there. As for my refactoring, the database structure came easily enough, and the UI started fine as well. But as I added more functionality, it became clear that my current layouts and designs where not going to work. I've come up with a nice vertical tab layout, but there's still too much information to fit on to the screen. A popup seemed the obvious solution.

That's where my troubles began.

Grails provides a neat, almost transparent Ajax functionality integrating jQuery or Prototype or some other JavaScript libraries. But once you go beyond basic Ajax, Grails doesn't offer much else in the way of rich web components. At work I've been working with RedHat's RichFaces components, which provide lots of useful tools such as modal popups, menus, accordions, etc. Grails does not. Grails does offer instead a rich plugin mechanism, that in theory would let me integrate another JavaScript UI framework. Sadly, the documentation is very sparse, and I can't find any examples on how to use jQuery-UI, or Twitter Bootstrap, or any other library, to integrate with Grails. The Grails plugin repository links to deprecated code, abandoned code, and poorly documented code. When I finally got Twitter Bootstrap installed, it played havoc with my existing CSS and layouts.

I fear that I'm going to have to learn JavaScript and write it all myself.