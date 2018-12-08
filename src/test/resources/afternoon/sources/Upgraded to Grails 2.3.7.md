---
title: Upgraded to Grails 2.3.7
author: Liam Davison
layout: basculePost
date: 11/04/2014
tags: [software development, cms, web, groovy]
slug: upgraded-grails-237
---
As I had recently moved to Spring Tool Suite 3.5.0, I thought I'd take advantage of the opportunity to upgrade the version of Grails Cinnabar was running on, from 2.2.4 to 2.3.7. After all, when you search the Grails documentation it mostly returns results for 2.3.7.

It took me a while to work out how to make the change. First of all, in `application.properties`, I changed the version of grails to 2.3.7: `app.grails.version=2.3.7`. Then I needed to change the version of Tomcat and Hibernate - a quick google told me I needed to change some lines in `conf/BuildConfig.groovy`. In the plugins section, I changed `runtime ":hibernate:$grailsVersion"` to `runtime ':hibernate:3.6.10.10'` and `build ":tomcat:$grailsVersion"` to `build ':tomcat:7.0.52.1'<`, then refreshed the dependencies.

I ran the application... and it blew up in my face. My admittedly nasty many-to-many relationship `ICT_PROP_JOIN` no longer worked, with the rather perplexing error `Field 'id' doesn't have a default value`. How could 'id' not have a default value? It's the primary key. Well, it used to be... Digging in to the database, Grails/Hibernate was now generating a rather different table than before:

```sql
CREATE TABLE `ict_prop_join` (
	`id` BIGINT(20) NOT NULL,
	`version` BIGINT(20) NOT NULL,
	`default_value` VARCHAR(255) NOT NULL,
	`ict_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
	`ict_property_id` BIGINT(20) NOT NULL,
	PRIMARY KEY (`ict_id`, `ict_property_id`),
	INDEX `FK5975D01E7AC239F` (`ict_property_id`),
	INDEX `FK5975D0128F982EA` (`ict_id`),
	CONSTRAINT `FK5975D0128F982EA` FOREIGN KEY (`ict_id`) REFERENCES `input_component_type` (`id`),
	CONSTRAINT `FK5975D01E7AC239F` FOREIGN KEY (`ict_property_id`) REFERENCES `input_component_type_property` (`id`)
)
```

Notice that 'id' was no longer a primary key? Somehow it had decided to use a composite primary key instead. (Semantically, this makes sense and I had considered using a composite primary key for this table, but the Grails documentation recommends against doing this.) In the end, I managed to make it work with only a few changes to my `ICT_PROP_JOIN` domain model, though bizarrely `BootStrap.groovy` no longer saves any values in this table.

```groovy
InputComponentType textBox = new InputComponentType(name: "Text Box",referenceName:"textBox").save()
InputComponentTypeProperty mandatoryField = new InputComponentTypeProperty(name: "mandatory", type:"checkbox", description:"Mandatory").save()
ICT_PROP_JOIN textBoxMandatory = new ICT_PROP_JOIN(ict_id: textBox.id, ictProperty_id: mandatoryField.id, defaultValue: "true").save()
```

The `InputComponentType textBox` and `InputComponentTypeProperty mandatoryField`> both get saved, but the `ICT_PROP_JOIN textBoxMandatory` does not.

### Changes to Date Parsing

Once that was fixed, I launched Cinnabar, dumped some sample data into my database, and checked to see that everything else was working. I was not expecting to find that date parsing had changed... I went to a page that contained a text box and a date field, pressed save, and was presented with this:

```
saveSection() params:
	3: date.struct
	3_day: 12
	6: 789
	5: 123
	pageId: 1
	3_year: 2014
	4: 456
	3_month: 4
	id: 1
	action: saveSection
	controller: page
2014-04-12 14:32:36,569 [http-bio-8080-exec-10] INFO  page.PageController  - Saving PageSection Page Section [1], 5 sectionValues
VS: ==== validation rules for ddd
	mandatory->true
*********** trying to save date value date.struct
Error |
2014-04-12 14:32:36,604 [http-bio-8080-exec-10] ERROR errors.GrailsExceptionResolver  - MissingMethodException occurred when processing request: [POST] /cinnabar/page/saveSection/1 - parameters:
3: date.struct
3_day: 12
6: 789
5: 123
pageId: 1
3_year: 2014
4: 456
3_month: 4
No signature of method: org.ljd.cinnabar.site.pages.content.ContentValue.setDateValue() is applicable for argument types: (java.lang.String) values: [date.struct]
```

I don't remember how dates were handled in the `params` map in 2.2.4, but this was clearly not right. Where did `date.struct`> come from? No matter, I could see that the date values where all there in the `params<` map, I just had to parse them. Naively, I tried:

```groovy
int day = Integer.valueOf(params[keyNameInt.toString() + "_day"])
int month = Integer.valueOf(params[keyNameInt.toString() + "_month"])
int year = Integer.valueOf(params[keyNameInt.toString() + "_year"])
println(day + " / " + month + " / " + year)
fieldValue = new Date(year,month,day)
```

With day=12, month=4 and year=2014, `fieldValue` now had the value of... 12th May 3914. Groovy's `Date()` constructor is a strange beast... Days are numbered 1 to 31, months are numbered 0 to 11, and years are 1900 years later than you think they are. I've written a simple method to parse these new dates in my `BaseController.groovy` class. I wonder what else has changed?
