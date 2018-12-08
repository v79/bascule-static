---
title: More Many-to-Many Problems
author: Liam Davison
layout: basculePost
date: 13/04/2014
tags: [software development, cms, web, groovy]
slug: more-many-to-many-problems
---
I thought I had it all working. I thought that my odd `ICT_PROP_JOIN` table was working correctly. My test data SQL file was masking a hidden problem though - `ICT_PROP_JOIN` was still not using the right primary keys. When I investigated why `BootStrap.groovy` was not creating the entries in `ICT_PROP_JOIN<` table, I ran into my old foe, _Field 'id' doesn't have a default value_. Looking at the SQL schema for the table, I could see that 'id' still wasn't the primary key.

```sql
CREATE TABLE `ict_prop_join` (
	`id` BIGINT(20) NOT NULL,
	`ict_property_id` BIGINT(20) NOT NULL,
	`ict_id` BIGINT(20) NOT NULL,
	`version` BIGINT(20) NOT NULL,
	`default_value` VARCHAR(255) NOT NULL,
	PRIMARY KEY (`ict_id`, `ict_property_id`),
	INDEX `FK5975D01E7AC239F` (`ict_property_id`),
	INDEX `FK5975D0128F982EA` (`ict_id`),
	CONSTRAINT `FK5975D0128F982EA` FOREIGN KEY (`ict_id`) REFERENCES `input_component_type` (`id`),
	CONSTRAINT `FK5975D01E7AC239F` FOREIGN KEY (`ict_property_id`) REFERENCES `input_component_type_property` (`id`)
)
COLLATE='utf8_general_ci'
ENGINE=InnoDB;
```

I decided to bite the bullet - if Grails wanted a composite primary key, I'd give it one myself. The Grails documentation makes it clear that if you want a composite primary key, then the domain class must implement `Serializable` and it must override the `equals()`and `hashCode()` methods. Here is my new domain class:

```groovy
class ICT_PROP_JOIN implements Serializable {

	String defaultValue
	Long ict_property_id
	Long ict_id
	
	static mapping = {
		table 'ICT_PROP_JOIN'
		id composite: ['ict_property_id','ict_id']
	}
	
	static constraints = {
		ict_property_id nullable:false
		ict_id nullable:false 
	}
	
	boolean equals(other) {
		if(!(other instanceof ICT_PROP_JOIN)) {
			return false
		}
		return (other.ict_property_id == this.ict_property_id && other.ict_id == this.ict_id && other.defaultValue == this.defaultValue)
	}
	
	int hashCode() {
		return new HashCodeBuilder().append(ict_property_id).append(ict_id).append(defaultValue).toHashCode()
	}
}
```

And it works. I had to add additional information to the mappings on the two joined classes - for the class `InputComponentTypeProperty`, this is:

```groovy
static mapping = {
	inputTypes joinTable: [name: "ICT_PROP_JOIN", key: 'ict_property_id', column: 'ict_id' ]
}
```

I've explicitly defined the foreign key ID using the `column`> instruction.

Amazingly, my `ReferenceDataService` did not need changing - my queries on `ICT_PROP_JOIN` still worked. Maybe I can finally move on with this project...
