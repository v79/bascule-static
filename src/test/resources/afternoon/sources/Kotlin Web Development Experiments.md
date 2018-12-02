---
title: Kotlin web development experiments
author: Liam Davison
layout: basculePost
date: 19/07/2017
tags: [software development, kotlin, web]
slug: kotlin-web-developments
---
This past month I've been experimenting with building full-stack web applications with Kotlin, the new JVM programming language _du jour_. I've previously played with Kotlin in Android app development, where it promises reduce some of the boilerplate code that Java is so infamous for. I'm not convinced by Kotlin yet, but I'm enjoying the learning experience and am willing to work through my issues and hopefully become a better programmer for it.

In my day job I primarily write JSF2 portlets running on JBoss Portal, and our backend runs on JBoss Fuse, BRMS, Oracle databases, Hibernate ORM and a bunch of big J2EE technologies. It's not a lot of fun. I've also worked with Spring MVC (again with Portlets) which I think I preferred. JBoss Portal is a dead product now, and will go out of support soon. No wonder; it's awful and it breaks basic conventions of the web like 'distinct URLs for each page'.

So for my own projects, I want get away from this corporate-enterprise monstrosity. I want to try to recreate a full-stack MVC web application in Kotlin, but without using any of the big-hitting frameworks such as Spring, Hibernate, JSF2, etc.

> What app am I building? Nothing, right now. There's some [code running on Heroku](https://kotlin-spark-routes.herokuapp.com/users/), but the app doesn't do anything - a simple form to capture and save some names. That's it. Groundbreaking stuff. Of course any half-decent developer could knock this together in half a hour in any web framework they chose. They could do it with HTML and CGI-Scripts written in _perl_ if they were feeling retro.
> The point isn't the application itself. The point is building a set of components which could be used to create much more interesting web applications in the future.

The [source code is on github](https://github.com/v79/kotlin-spark-routes), and there are a few branches as I've explored different technologies, as outlined below.

### Web controller and view

First, I needed a framework that would process HTTP requests and render HTML. I had stumbled upon [SparkJava](http://sparkjava.com/), which is small and easy to read. It's probably mostly used for building REST APIs, but it can handle HTML pages and templates just fine. Even better; there's a Kotlin variant that's easy to write for and read. I've paired it with the [Thymeleaf](http://www.thymeleaf.org/) templating engine, for no other reason than it's supported by SparkJava and is relatively new and modern. I used the [spark thymeleaf engine](https://github.com/perwendel/spark-template-engines/tree/master/spark-template-thymeleaf), but converted the code from Java to Kotlin in Intellij. It just works, though of course I could have just kept in Java.

#### JavaScript and AJAX

I've done some experiments with hand-writing AJAX calls in JavaScript, and it was working fine with SparkJava and Kotlin. I also did the same using jQuery. But I hate JavaScript and hope to minimise my exposure to it. Kotlin can, apparently, compile to JavaScript, but it seems to be early days for that. I'm going to ignore JavaScript and AJAX for now, and return to it later.

#### Dependency Injection

I've never really been convinced by dependency injection, but I've swallowed my pride and accepted that it is required and correct in large scale applications. DI doesn't allow you to hot-swap classes and objects; there's always a redevelopment and rebuild required, so why not just change the code that needs changing?

Anyway, I know I'm wrong, and I accept that DI is useful when testing, so my web project will use DI. But which framework? The big granddaddy is JSR 330 and the `@Inject` annotation, as implemented by Spring. But my application isn't running on a J2EE container (right now, it's running on Jetty). And besides, if I start using Spring for injection, I might as well use Spring for everything. It even works with Kotlin now.

I looked at Google's _Guice_, which seems to be old and out of favour. The cool kids on Android are using [Dagger 2](https://google.github.io/dagger/), but I didn't really like the look of it. The example code, with its CoffeeMakers and Thermosiphons, is meaningless and a bit obtuse to me. After [asking on reddit](https://www.reddit.com/r/Kotlin/comments/6msk74/state_of_dependency_injection_in_kotlin/), I've chosen [Kodein](https://github.com/SalomonBrys/Kodein), a DI framework written entirely in Kotlin. It's been a bit of a learning curve, but I've managed to get some basic injection of services into my controllers, and importantly injecting mock services into my controller JUnit tests.

### Databases and ORMs

Sadly, things haven't gone so well in trying to find a database framework. I want to avoid Hibernate and JPA if possible - the point of this project is to avoid the big frameworks. But I like Hibernate, I'm used to it, and giving up the simplicity of an annotation-driven ORM is difficult to do. In the Android world, there are several alternatives, and I've used [GreenDAO](http://greenrobot.org/greendao/) quite happily on my Android app. But GreenDAO isn't available in Kotlin yet, and, more importantly, it only supports the SQLite database.

I've nothing against SQLite, but it's [not supported on Heroku](https://devcenter.heroku.com/articles/sqlite3), the cloud hosting service I'm using to test my application. I'm most experienced in using MySQL, but would rather use MariaDB, the new fork of MySQL. I'll also consider PostgreSQL, the default database for Heroku, but I've never used it before. PostgreSQL isn't fully supported by [HeidiSQL](https://www.heidisql.com/), the tool I'm most familiar with for managing databases (I tried it out and immediate hit [this bug](https://www.heidisql.com/forum.php?t=22726#p22833)).

So, here are my requirements, roughly in order of priority:

- Is an ORM, so it understands how to do database joins, queries and relations<
- Works with MariaDB, MySQL or PostgreSQL
- Isn't too intrusive on my data model - I want to write simple Kotlin code (like a POJO), or use data classes
- Annotations would be nice
- Is Kotlin-friendly
- Great if it's JPA-compliant
- I can query the database using HeidiSQL

####requery

My first attempt was to use [requery](https://github.com/requery/requery) - it supports Kotlin, it uses annotations (though isn't JPA), it works fine with MariaDB. It's quite easy to understand - create an abstract class, or an interface, annotated with `@Entity`, and the annotation processor will generate concrete implementations of the classes. However, I could never really get Intellij to recognise the generated classes, so the IDE was reporting all sorts of errors and preventing code-completion and other useful tools. Even if gradle could build the entire project, IntelliJ could not.

Lines of code like this were particularly problematic:

```kotlin
val configuration: Configuration = KotlinConfiguration(dataSource = mySQLDataSource, model = uk.co.liamjdavison.kotlinsparkroutes.db.model.Models.DEFAULT)
val data = KotlinEntityDataStore<Persistable>(configuration)
```

Try as I might, I couldn't persuade IntelliJ to recognise the generated `Model` class. Without it, the code would not run.

Then I started running into other problems. If my `User` class is abstract, how to I create a new `User`? I have to create a `UserEntity` instead - using the generated class IntelliJ can't find. In the end, I was disappointed with requery.

#### Exposed

Next up was [Exposed](https://github.com/JetBrains/Exposed), a curious SQL DSL/ORM written by JetBrains, the creators of Kotlin and IntelliJ. It's a strange beast, but I was keen to give it a go as a pure Kotlin solution.

Syntatically it's bizarre. In its DSL variant, it leads to code like this:

```kotlin
Users innerJoin Cities).slice(Users.name, Cities.name).
 select {(Users.id.eq("andrey") or Users.name.eq("Sergey")) and
 Users.id.eq("sergey") and Users.cityId.eq(Cities.id)}.forEach {
 println("${it[Users.name]} lives in ${it[Cities.name]}")
 }
 ```
 
What on earth is `(Users innerJoin Cities)`? Where do these brackets come from? It's not a type cast, that I can see. But it does work, and I'm fairly certain I could manage the code. It's not quite an ORM in this mode though - creating a relationship between objects relies on passing primary keys around (and lots of using the `it` keyword).

```kotlin
val saintPetersburgId = Cities.insert {
 it[name] = "St. Petersburg"
 } get Cities.id

Users.insert {
 it[id] = "andrey"
 it[name] = "Andrey"
 it[cityId] = saintPetersburgId
 }
 ```
The DAO approach, which understands Kotlin classes and the relationships between them, has a simpler query syntax but a bizarre modelling approach which requires both an object and a class, with the object becoming the companion to the class:

```kotlin
object Users : IntIdTable() {
 val name = varchar("name", 50).index()
 val city = reference("city", Cities)
 val age = integer("age")
}

class User(id: EntityID<Int>) : IntEntity(id) {
 companion object : IntEntityClass<User>(Users)
 var name by Users.name
 var city by City referencedOn Users.city
 var age by Users.age
}
```

So there are two bits of code for every entity. Again, I think I could work with it, and probably wrap it in some helper classes to make the code more readable.

However, _Exposed_ does not support _MariaDB_. MariaDB is a fork of MySQL, and I had hoped that they would work much the same. Indeed, Exposed's DSL worked fine with MariaDB. But in DAO mode, the database tables were created, but data was not written and a null pointer exception was thrown. I've [raised a defect with Exposed](https://github.com/JetBrains/Exposed/issues/132) on Github and hope they can find a solution soon.

### Next steps?

I'm torn. I'm still reluctant to revert to _Hibernate_, though it's said to work fine once you apply certain compiler plugins which changes some of Kotlin's default behaviours. I kinda like _Exposed_, but it is bizarre and doesn't currently support my database of choice. I'll try out MySQL, I suppose, though I'd much rather use MariaDB. I dislike _requery_, and I'm ruling it out for now. There's another Kotlin framework called _Squash_, written by a JetBrains developer, but I think it's a variant of Exposed and will probably not offer anything over Exposed.

For my current requirements, I could go for a NoSQL approach. I've never used a NoSQL database though, and given everything else I'm trying to learn, I fear that it's a step too far.

I've stalled for now. I see plenty more googling and reddit-ing ahead.
