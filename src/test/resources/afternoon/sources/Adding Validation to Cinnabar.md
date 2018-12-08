---
title: Adding Validation to Cinnabar
author: Liam Davison
layout: basculePost
date: 11/04/2014
tags: [software development, cms, web, groovy]
slug: adding-validation-to-cinnabar
---
Grails has some really neat mechanisms for validating user input. Simply add some constraints to your domain model, and Grails' scaffolding will build nice forms with helpful error messages. There are loads of pre-set constraints, such as _maxSize_ and _email_. Here's an example from my base <code>Site</code> class:

<pre>
static constraints = {
	name(blank:false, nullable:false, maxSize: 127)
	createdBy(email:true)
	description (nullable:true, maxSize: 1023) 
	root(nullable:true)
    }
</pre>

This ensures that the _name_ can't be blank, and can be no more than 127 characters long. The _description_ can be blank, and has a maximum size of 1023 characters. The _createdBy_ field must be an email address - no hacking together regexes in JavaScript.

Cinnabar's requirements are a bit trickier though, as it's the user who defines what the rules are, not the domain model. In one `Page Section` a text field called _Headline_ might be marked as Mandatory and have a maximum length of 400 characters. A second text field called _Author_ might not be mandatory at all, and have a different length. All the values for every possible field the user can set up - text fields, text areas, images, dates and so on - are stored in a single domain model called `ContentValue`. So I can't use Grails' domain validation.

Reading the documentation suggested that I could use Command Objects and add validation constraints there - but again, these rules are too variable to be stored in a Command Class. So I've decided to write a `ValidationService`. I experimented with throwing and catching various Exceptions - that's how I would have handled in it JSF2. But in the end I realised that I could piggy-back onto Grails on validation routines.

It's not made explicit, but all Grails Domain Classes have a field called `errors`, and you can check the existence of the errors field in your GSP. This is all based on the [Spring Errors Framework](http://docs.spring.io/spring/docs/1.2.x/api/org/springframework/validation/Errors.html "Spring Errors Framework 1.2").

```
<g:hasErrors bean="${icVal}">
	<ul class="errors" role="alert">
		<g:eachError bean="${icVal}" var="error">
			<li
				<g:if test="${error in org.springframework.validation.FieldError}">
				data-field-id="${error.field}"</g:if>>
				<g:message error="${error}" />
			</li>
		</g:eachError>
	</ul>
</g:hasErrors>
```

So all my validation service has to do is to add values to the errors object whenever a validation fails, using the `rejectValue()` method:

```groovy
if(value == null || value.toString() == "") {
       contentValue.errors.rejectValue('stringValue',
		'page.editor.validation.mandatoryValueBlank',
		[contentValue.component.name].toArray(),
		null)
}
```

My validation service is still in its infancy. It needs to be able to handle different data types and different rules - so far it can only check for Mandatory fields, and the maximum length of text fields and text areas. My `ContentValue` model has different fields for saving strings, floats, dates and binary data, so I will need to add additional code to handle these too. Still, I now have a framework in place.

### A MaxSize Gotcha
Grails allows you to specify various parameters when setting up a field in your GSP. For my basic text box, I was using:

```
<g:field name="${icValiD}" type="text" required="${required}"
	value="${icVal?.stringValue}" maxlength="${maxLength}"
	title="${component.name}, ${maxLength} characters" />
```

Unfortunately, that `maxlength` attribute simply truncates the user's input to the length declared, rather than displaying an error message. So I've had to remove the ` maxlength="${maxLength}"` instructions from my fields in GSP, and rely on my `ValidationService` to report length errors.
