---
title: Javascript Success
author: Liam Davison
layout: basculePost
date: 12/03/2014
tags: [software development, cms, web, groovy]
slug: javascript-success
---
Last night I spent a couple of hours wrestling with Grails and jQuery-UI. As is so often the case, the final solution to myJavaScript woes was quite simple and required only a few lines of code, but trying to find the right combination of a few lines was hard work. I'm surprised how little guidance or tutorials there are on combining Grails with jQuery-UI (or Dojo, or Twitter Bootstrap, or insert-framework-of-your-choice).

My goal, I thought, was straightforward - pop up a modal dialogue in response to a click, fetching a form through an Ajax call. Here's how I did it.

In `conf/BuildConfig.groovy`, I added the jQuery-ui plugin just after the jquery runtime line:

`compile ":jquery-ui:1.10.3"`

Next, in `views/layouts/main.gsp`, I found a combination of `<g:javascript />` and `<r:require />` statements that worked for me. This took a bit of fiddling around. In the `<head>` section, _before_ the call to `<r:layoutResources>`, I added:

```
<r:require module="jquery-ui">
<g:javascript library='jquery' />
<g:javascript library="application"/>
<g:javascript src="cinnabar.js"/>
```

`cinnabar.js` is my own javascript file; I didn't want to add things into Grails' own`application.js` file.

This worked, but I wanted to see if I could standardise it a bit. Why did I have to use `<g:javascript src="cinnabar.js"/>` and not `<g:javascript library="cinnabar"/>`? Turns out I needed to make a change to `conf/ApplicationResources.groovy`:

```
modules = {
	cinnabar {
		resource url: 'js/cinnabar.js'
		}
	}
```

This declares my javascript source file as a Grails Resource, allowing me to use `<g:javascript library="cinnabar">`.

### Popup
OK, so we now have jQuery-UI installed. How do I add my popup?

In the view where I wanted the popup to be, I added a single named div:

```html
<div id="editinputcomponent">
	<%-- render editing component here --%>
</div>
```

Next, I set up my jQuery-UI dialog in `web-app/js/cinnabar.js` (I'll probably rename some of the variables here and make it more generic for other dialogs):

```javascript
var editinputcomponent = {
	modal: true,
	resizable: false,
	draggable: false,
	autoOpen: false,
	width: 500,
	show:  { effect: "fade", duration: 400 },
};

function openDialog(selector, data, title) {
	var dialog = $(selector).dialog(editinputcomponent);
	dialog.dialog('option', 'title', title);
	dialog.html(data);
	dialog.dialog('open');
};
```

The `var editinputcomponent` sets up the default values for my dialog, such as making it modal, preventing it from being dragged or resized, and so on. The function `openDialog(selector, data, title` opens the dialog box, constructing it with the jQuery-UI `.dialog()` method, setting the title, and setting the contents using `dialog.html(data)`. This line is the trick to making it work with a Grails Ajax call, as I'll demonstrate next.

To trigger the ajax fetch for my form, I already had Grails' `<g:remoteLink>` tag working. But I needed to change it to open the dialog after the controller had successfully returned the HTML fragment for the form:

```
<g:set var="dialogTitle" value="Edit ${ic?.name}" />
<g:remoteLink controller="InputComponent" action="editIC"
	params="${[rowNum: row, pgTmplId: pageTemplateInstance.id] }"
	title="Edit Component" update="[success: 'editinputcomponent']"
	onSuccess="openDialog('#editinputcomponent',data,'${dialogTitle}')">
	Edit
</g:remoteLink>
```

This creates an Ajax request that updates the editinputcomponent div if the Ajax call is successful. After a successful Ajax response, the `onSuccess="openDialog('#editinputcomponent',data,'${dialogTitle}')"` line calls my `openDialog()` JavaScript function, passing in the id of the div, the data returned by the Ajax call (in the variable `data`), and the title of the dialog.

The returned form data is a standard Grails form which uses a `<g:actionSubmit>` to submit the form elements. It also closes the dialog, which is the desired behaviour but I'm not sure why it works!
