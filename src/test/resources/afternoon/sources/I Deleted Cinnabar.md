---
title: I Deleted Cinnabar
author: Liam Davison
layout: post
date: 15/03/2014
tags: [software development, cms, web, groovy]
slug: deleted-cinnabar
---
A couple of days ago I deliberately deleted all the code for my CMS, Cinnabar. And this morning I resurrected it all, restoring all files from SVN. So why did delete it, and why did I bring it back?

In the first few days of the project, I knew at the back of my mind I was going to have to deal with a tricky database setup involving a many-to-many join table with additional properties. I wasn't sure how I was going to get it to work in Grails, so after I had built the basic scaffold of the site, I concentrated on that setup. It took a while, and there is some ugly database access code I need to revisit. Once I had that working, and had done some UI work, I needed to go back and deal with the part I had missed.

The basic structure of a Cinnabar project will be: a _Site_ is comprised of a number of _Page Templates_. A Page Template will have zero or more _Input Components_ (think text boxes, pictures, etc). A Page Template will also have zero or more _Section Templates_, and those Section Templates will themselves comprise of many Input Components. The data model for these Input Components (I must come up with a better name for these...) is the tricky thing with the join tables, so I skipped the Section Templates part of the project.

When I started to add in the Section Templates, I realised that I had missed some key thinking and I couldn't just engineer in the Section Templates. Since then I've done a lot more thinking, and I eventually decided the whole project was built on shaky grounds. Trying to unpick my broken Section Templates from the rest of the project looked like pretty tricky stuff, so I branched the code in SVN, and pressed delete.

Now I'm not so sure - delete and start again is a pretty dramatic approach. And I got scared and reverted the changes. So now I need to methodically work through the code base, and work out what should stay and what should go. It might be a fraught weekend...
