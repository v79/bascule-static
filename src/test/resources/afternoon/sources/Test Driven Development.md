---
title: Test Driven Development and Handling XML
author: Liam Davison
layout: post
date: 18/05/2014
tags: [software development, cms, web, groovy]
slug: test-driven-development-xml
---
The last few days have represented crunch time for Cinnabar. The project is finally in a state to start generating web pages in HTML. The framework of templates, input components and pages was in place. The first job was to validate and parse my templates, which are in a mixture of HTML and my own namespaced CMS tags. Validating the code was pretty straightforward, but parsing the XML has been hard work. I've never worked with XML before. Groovy has lots of utilities to help with manipulating XML, such as `XmlParser` and `XmlSlurper`, but I really struggled to get my head around them. There's not a lot of documentation around, especially when you start adding namespaces into the mix.

I finally decided to use `DOMBuilder`, but even that was a challenge. I took several attempts - when generating my HTML, was I trying to rewrite the existing XML, or creating a new document combining the source XML with values from the page variables, or build a new HTML line by line?

### Writing Unit Tests

I really had no idea how to approach this XML transformation. After a few attempts with Cinnabar running, I decided to try test driven development. I'm used to writing Java tests using TestNG, JUnit and Mockito. Grails seemed to use JUnit but after my recent upgrade to Grails 2.3.7 it now defaults to Spock.

The HTML generation service will depend on at least one more service, currently unwritten. My generation service currently has a private method returning a few values to pass the tests. This needs to be replaced with a service call that I will mock out in the unit test.

I feel like I have too much learn right now - XML parsing using `DOMBuilder`, testing with Spock (or another framework like GMock). This is not helped by my apparent need to use partial mocks.