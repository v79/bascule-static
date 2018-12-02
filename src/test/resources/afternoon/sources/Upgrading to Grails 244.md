---
title: Upgrading to Grails 2.4.4
author: Liam Davison
layout: basculePost
date: 02/02/2015
tags: [software development, cms, web, groovy]
slug: upgrading-to-grails-2.4.4
---
A few months ago I upgraded my **Cinnabar** application to Grails v2.3.7, and ever since then things have not gone well. I have made no development progress recently. Instead, I've been trying to understand the changes to my code needed to make Cinnabar just run. After the dust from that move settled, Cinnabar wasn't really working and I tried to debug it - but Eclipse, or Spring Tool Suite, could not launch the application in debug mode, and couldn't run any unit tests at all.

There were lots of unhelpful error messages. I'm not sure if they are worth repeating here, as the errors were due to a combination of Spring Tool Suite, Grails 2.3.7, my own code, the installation directory of STS, and a bunch of other things. Individually they were manageable, but together it was all a mess.

I decided to try a radical solution and upgrade again - to Grails 2.4.4. You'd think I'd be stung after the pain of moving to 2.3.7, but I reasoned that it was better to be on the most up-to-date codebase, particularly as 2.4.4 was designed to bridge the gap to the upcoming Grails 3 release. Rather than trying to upgrade in-place, I decided to create a new Grails application, then copy over the code file-by-file. I started with the data model, then the controllers, services and views. It went fairly well, and the new version launched with no obvious failures. Layouts and images were broken, but I expected that - Grails 2.4.4 replaces the `resources` plugin with a new one called `asset-pipeline`, with corresponding changes to the GSP tags required to import CSS and Javascript.

More frustratingly were the unexpected changes to GORM. Suddenly I had strange errors about object IDs already being in use when the application tried to create a new Page Template or Input Component. I have had to add additional `.save(flush:true)` and `.merge()` calls in my controllers, particularly when several new objects need to be persisted at once.

I think it's all working now. It's been a frustrating few months, with no real progress at all. It makes me worry for the future of the Grails platform - especially as its main corporate sponsor, Pivotal, has withdrawn from funding Grails. I'm almost tempted to start Cinnabar all over, using Java and JSF2 instead.
