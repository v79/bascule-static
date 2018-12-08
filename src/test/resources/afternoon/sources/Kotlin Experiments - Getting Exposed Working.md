---
title: Kotlin Experiments - Getting Exposed Working
author: Liam Davison
layout: basculePost
date: 24/07/2017
tags: [software development, kotlin, exposed]
slug: kotlin-experiments-exposed
---
I have finally managed to get the [Exposed ORM framework](https://github.com/JetBrains/Exposed) working with the MariaDB database. I had all but given up on MariaDB, and I installed Oracle's MySQL database instead. But to my horror, I faced the same problem in MySQL - a strange null pointer exception when flushing the database inserts. I [updated the ticket](https://github.com/JetBrains/Exposed/issues/132) on Exposed's github page and finally got a response. The solution was simple - update my database drivers. Slightly embarrassed that I hadn't considered that option, and slightly surprised that the driver changes could make such a difference. I would have assumed that the basics of database inserts wouldn't have changed much over the years.

Once I had my test code working with MariaDB (I had to change the way I created the database connections, and spent far too long with a username/password issue which was entirely my fault), I started writing a DAO framework to wrap around Exposed. But I first I had to wrestle with Exposed's odd dual-layer approach to modelling entites.

Consider the following - very simple - data class model for a very simple `User`:

```kotlin
data class User(val id: Int, val name: String, val age: Int?)
```

And compare it to Exposed's internal representation:

```kotlin
object UserCompanion : IntIdTable() {
  val name = varchar("name", 50).index()
  val age = integer("age")
}

class UserDB(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<UserDB>(UserCompanion)
  var name by UserCompanion.name;
  var age by UserCompanion.age
}
```

Exposed requires a companion object which defines the core 'table', and a class which handles the ORM mapping and relationships. It's odd, and I'm struggling with the naming. The companion object becomes the SQL table - so I've created a table called "`usercompanion`", which is far from ideal. Extending `IntIdTable` gives us the auto-increment primary key for the table (a `LongIdTable` option also exists).

So we have the `User` data class, the `UserCompanion` object, and the `UserDB` class. I haven't quite decided which class is responsible for converting between these models, but for now it happens at the DAO layer. The DAO object and class are stored within the <code>UserDao</code> for now, but I suspect I will need to move them into spearate files once I have more complicated table relationships.

I have defined an `AbstractDAO` class which contains the database connection details - the connection parameters are injected via the `Kodein` injection framework. I'm growing to like Kodein; I'll write more about it another time. There's also a simple `Dao` interface which might declare basic functions like `get(), save(), delete()` and so on; for now it's empty.

I surprised myself when writing the `UserDao` class by how much code I just wrote without repeated testing and planning. My main mistake was not using Exposed's `transaction { ... }` blocks. I suspect that there's a better way of handling transactions in Exposed, which I'll need to research further.

None of this code is final. I'm still exploring what's possible, rather than what's best. But I know have  the basics of a web MVC stack working. [MariaDB](https://mariadb.org/)and [Exposed](https://github.com/JetBrains/Exposed) for database; [spark-kotlin](http://sparkjava.com/) for controllers; [Kodein](https://github.com/SalomonBrys/Kodein) for DI; [Thymeleaf](http://www.thymeleaf.org/) for views. No [Spring](https://spring.io/)involved :). But to be serious as a web framework, I still need to work on security, session handling, AJAX and validation, and probably a dozen more things I haven't considered yet.

Next step is to merge my [Exposed branch](https://github.com/v79/kotlin-spark-routes/tree/exposed) into master on Github, and get the database working on Heroku.
