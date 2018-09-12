---
title: Many-to-Many Relationships in Grails and Grom
author: Liam Davison
layout: post
date: 04/04/2014
tags: [software development, cms, web, groovy]
slug: many-to-many-relationships
---
I have been struggling many to many relationships in Grails and GORM for some time. So much so that I have two different implementations for two different data models. For basic many to many relationship all we need to do is add a hasMany instruction to both our main classes. By default this will create a join table between the two domain classes. However i have always had to add ditional properties two the join table, and Grails does not make this easy.

My first join was between _InputComponentType_ and _InputComponentTypeProperty_. This allows me to specify the mandatory properties of an InputComponent type, such as "Text Box", and it's properties, such as "maxSize". The joining table has additional properties, such as a default value. In Cinnabar, Text Boxes have a default maxSize of 255 characters. This value can be overridden by the user.

So, an `InputComponentType hasMany InputComponentTypeProperty`, and vice versa.

I created my own domain model to represent this - `ICT_PROP_JOIN` - which contains `fields ict_id, ictprop_id`, and `defaultValue`. Both the base classes refer to this join table under `static mapping = {}`:

```groovy
class InputComponentType {

    String name
    String referenceName

    static hasMany = [ictProperties:InputComponentTypeProperty]

    static constraints = {
        name(nullable:false, unique: true, maxSize: 127)
    }

    static mapping = {
        ictProperties joinTable: [name: "ICT_PROP_JOIN", key: 'ict_id']
    }

    String toString() {
        return "InputComponentType: " + id + ", " + name
    }

}

class InputComponentTypeProperty {

    String name
    String type
    String description

    static belongsTo = InputComponentType
    static hasMany = [inputTypes: InputComponentType]

    static constraints = {
        name(nullable: false, unique: true, maxSize: 127)
        type(nullable: false, maxSize: 127)
    }

    static mapping = {
        inputTypes joinTable: [name: "ICT_PROP_JOIN", key: 'ictProperty_id' ]
    }

    String toString() {
        return "[InputComponentTypeProperty " + id + "] " + name + " [" + type + "] '" + description + "'"
    }
}
class ICT_PROP_JOIN {

    String defaultValue
    Long ictProperty_id
    Long ict_id

    static mapping = {
        table 'ICT_PROP_JOIN'
    }

    static constraints = {
        ictProperty_id nullable:false
        ict_id nullable:false 
    }
}
```

Notice how both classes refer to the join table in their `static mapping = { }` sections. This works, but it has some unusual side effects. For instance, when the application starts and the data model is generated, there are some SQL errors:

```
pre>| Error 2014-04-05 10:32:27,978 [localhost-startStop-1] ERROR hbm2ddl.SchemaExport  - Unsuccessful: create table ict_prop_join (ict_id bigint not null, ictProperty_id bigint not null, primary key (ict_id, ictProperty_id))
| Error 2014-04-05 10:32:27,979 [localhost-startStop-1] ERROR hbm2ddl.SchemaExport  - Table 'ict_prop_join' already exists
| Error 2014-04-05 10:32:29,213 [localhost-startStop-1] ERROR hbm2ddl.SchemaExport  - Unsuccessful: alter table ict_prop_join add index FKC0E8312122BF6AEA (ictProperty_id), add constraint FKC0E8312122BF6AEA foreign key (ictProperty_id) references input_component_type_property (id)
| Error 2014-04-05 10:32:29,213 [localhost-startStop-1] ERROR hbm2ddl.SchemaExport  - Key column 'A' doesn't exist in table
```
The final error is particularly perplexing: "Key column 'A' doesn't exist in table". Sometimes it's "Key column 'ă' doesn't exist in table", sometimes it's "Key column 'Ɲ' doesn't exist in table" - basically, a random character. I'm guessing that the main error about being unable to create the table `ict_prop_join` is because both domain classes try to set up the mapping. I'll experiment with removing one of these entries, but I do need both sides of the join to have equal access to each other.

Querying this join table is horrendous. For instance, if I want to find all the possible properties of text boxes, here is my SQL:

```SQL
select * from input_component_type_properties as ictP
         inner join ict_prop_join as ipj on ictP.id = ipj.ict_property_id
         inner join input_component_type as ict on ipj.ict_id = ict.id
         where ict.name = "Text Box";
```
And here is the horrible mashup of HQL and GORM dynamic queries I've put together to replicate this relatively simple SQL query:

```groovy
def query = "FROM InputComponentTypeProperty AS ictProps, " +
	"InputComponentType AS ict, " +
	"ICT_PROP_JOIN AS propJoin "+
	"WHERE ict.id = ${type.id} AND propJoin.ict_id = ict.id AND propJoin.ictProperty_id = ictProps.id"

List typeProps = InputComponentTypeProperty.findAll(query)

List ictPropIDsOnly = new ArrayList()
	typeProps.each {
	ictPropIDsOnly.add(it[0].id)
}

List typeProperties = InputComponentTypeProperty.getAll(ictPropIDsOnly)
```

Yes, that is two separate queries to the database because my HQL query returns a bizarre set containing not just the IDs of the table I'm interested in, but the IDs of the other two tables in the query too. This code is crying out to be rewritten. I've never really got my head around Criteria in Hibernate, in either Java or Groovy. But I'm sure that it can be done.

#### A Second Join Table, and a Second Approach

The `ICT_PROP_JOIN` table works, it's queryable, it stores the correct information in the correct place. But it _feels_ ugly and wrong. As my understanding of the needs of Cinnabar has grown, I've also realised that the table isn't going to be used for very much at all. I do need a second many-to-many relationship with additional properties for another part of the project, so I'm trying a different approach.

In Cinnabar, a _Page Template_ will be divided into a number of _Content Areas_ - think header, body, footer, sidebar etc - and those content areas can have one ore more assigned _Section Templates_ - such as news item, rich text area, picture gallery. As the same Section Template can be used in different Content Areas, we have another many-to-many relationship. In practice, it's an uneven relationship, as Section Templates don't really care about which Content Areas they are attached to.

A Section Template may be mandatory for a Content Area, or not. And it may be unique to a Content Area (cannot be copied) or not. So I needed a join table with additional attributes. In code, a `ContentArea hasMany sectionTemplates`; a `SectionTemplate hasMany contentAreas`, so Grails automatically created a join table called `content_area_section_template`. I've decided to re-purpose it for myself:

```groovy
class ContentArea extends Template {

	String description

	boolean mandatory = false
	Date dateCreated
	Date lastUpdated
	
	static belongsTo = [pageTemplate:PageTemplate]
	static hasMany = [cast:ContentAreaSectionTemplate]
	
        List<SectionTemplate> possibleSectionTemplates
	List<SectionTemplate> assignedSectionTemplates
	static transients = ['possibleSectionTemplates','assignedSectionTemplates']

	static mapping = {
	}
}

class SectionTemplate {

	String name
	String description

	static hasMany = [inputs:InputComponent, cast:ContentAreaSectionTemplate]
	static belongsTo = [site:Site]
	
	static constraints = {
		name(unique:true, nullable:false, blank: false, maxSize: 127)
		description(maxSize: 1023)
		site(nullable: false)
	}
}

class ContentAreaSectionTemplate {

	ContentArea contentArea
	SectionTemplate sectionTemplate
	Boolean mandatory = false
	Boolean cloneable = true

	static mapping = {
		 table 'content_area_section_template' }

	static constraints = {
	}

	static ContentAreaSectionTemplate link(ContentArea contentArea, SectionTemplate sectionTemplate) {
		println("Creating new CAST for " + contentArea.name + " and " + sectionTemplate.name)
		def cast = new ContentAreaSectionTemplate()
		contentArea?.addToCast(cast)
		sectionTemplate?.addToCast(cast)
		cast.save()
	}

	static void unlink(ContentArea contentArea, SectionTemplate sectionTemplate) {
		def cast = ContentAreaSectionTemplate.findByContentAreaAndSectionTemplate(contentArea,sectionTemplate)
		if(cast) {
			contentArea?.removeFromCast(cast)
			sectionTemplate?.removeFromCast(cast)
			cast.delete()
		} else {
			println("ContentArea/SectionTemplate combination not found")
		}
	}
}
```

A completely different approach to handling the many-to-many relationship. My join table class ContentAreaSectionTemplate provides two methods, _link_ and _unlink_, to connect the two relevant classes. This means I'm having to manage adding and removing items to the relationship myself - Grails won't do it for me. Notice that `ContentArea hasMany ContentAreaSectionTemplates`, and also `SectionTemplate hasMany ContentAreaSectionTemplates`. I'm probably going to write helper methods in ContentArea and SectionTemplate to hide some of this, something like `contentArea.addSectionTemplate()` and `contentArea.removeSectionTemplate()`.

Notice that ContentArea has two transient lists of SectionTemplates - these lists are never saved in the database. Their values are calculated by comparing the list of SectionTemplates defined in the Site with those entries in the ContentAreaSectionTemplates join table.

### Is This Correct?

I'm really not sure if I'm going about this the right way. I've tried two different approaches to creating many-to-many relationships with additional properties. Both work. Both require additional supporting code. Both _smell_ wrong. I had all sorts of strange errors when I first created the ContentAreaSectionTemplate class - Hibernate errors like "unable to find default value for field 'version'".

So for now I'm going to keep both approaches, and see what shakes out in the end.