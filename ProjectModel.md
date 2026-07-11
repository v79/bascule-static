# Project and Post Model for Bascule

All project attributes are available as variables to be used in Handlebars templates. In addition, there are attributes
that are applicable in certain contexts, most notably in lists and paginated pages. Finally, each post or page also has
its own set of attributes. Any attribute can be accessed in Handlebars through the `{{ attribute }}` syntax.

Some generators may add their own model attributes

## Project Model

| attribute      | type           | required | default value               | notes                                     |
|----------------|----------------|----------|-----------------------------|-------------------------------------------|
| siteName       | string         | true     |                             | Set on project creation                   |
| dateFormat     | String         | true     | dd/MM/yyyy                  | Standard ISO formats available            |
| dateTimeFormat | String         | true     | HH:mm:ss dd/MM/yyyy         | Standard ISO formats available            |
| postsPerPage   | Int            | true     |                             | Controls when pagination happens          |
| theme          | string         | true     |                             |                                           |
| posts          | array of posts | true     |                             | Contains all posts, across all layouts    |
| postCount      | int            | true     |                             | Total number of posts, across all layouts |
| __nowDate      | Date           | true     | Date of generation          | Set automatically                         |
| __now          | DateTime       | true     | Date and time of generation | Set automatically                         |

- Also in model, but will be removed: postLayouts, directories, plugins, processors

## IndexPageGenerator Model

> [!Caution]
> TODO

| $thisPage |

## BasculePost Model

This represents all blog-post type content, that is dated and paginated.

| attribute      | calculated | notes                                             | default if not supplied                 |
|----------------|------------|---------------------------------------------------|-----------------------------------------|
| title          |            |                                                   | from file name if note specified        |
| url            | true       |                                                   | from sources path and slug              |
| sourceFileName | true       | absolute file path of source file                 |                                         |
| layout         | true       | the template file, default post                   |                                         |
| date           |            |                                                   | from creation date of file              |
| tags           |            | supply as YAML array: \[tag1, tag2]               | empty array                             |
| slug           | true       | final file name of output                         | from source file name, santised for web |
| attributes     |            | any additional attributes from YAML               |                                         |
| newer          | true       | URL of following post                             |                                         |
| older          | true       | URL of preceding post                             |                                         |
| __currentPage  | true       |                                                   | slug of current post                    |
| content        | true       | rendered html, include with triple {{{ braces }}} |                                         |

Anything additional properties added to the YAML of a post or page will appear in the `attributes` map. Some calculated
attributes can be overridden. For example, given this Markdown file called `xmas1990.md` created on boxing day:

```yaml
---
title: My Christmas blog post
date: 25/12/1990
layout: post
slug: my-christmas-post
favouritePresent: jumper
tags: [ xmas, presents ]
---
Merry Christmas
```

Then the model values will be:

| model      | default value | actual value              |
|------------|---------------|---------------------------|
| title      | xmas1990      | My Christmas blog post    |
| date       | 26/12/1990    | 25/12/1990                |
| layout     | post          | post                      |
| slug       | xmas1990      | my-christmas-post         |
| url        | xmas1990.html | my-christmas-post.html    |
| attributes |               | favouritePresent → jumper |
| tags       |               | \[xmas, presents]         | 
| content    |               | Merry Christmas           |

## PostNavigationGenerator Model

These model objects are added when generating listing pages for paginated pages such as lists of blog posts. The first page is number 1.

| attribute    | value                                                 |
|--------------|-------------------------------------------------------|
| currentPage  | index of the current page in pagination               |
| totalPages   | the total number of pages in the list                 |
| isFirst      | true if this is the first page                        |
| isLast       | true if this is the last page                         |
| previousPage | index of previous page (0 if isFirst is true)         |
| nextPage     | index of next page (totalPages + 1 if isLast is true) |
| nextIsLast   | this is the penultimate page                          |
| prevIsFirst  | this isthe second page in the list                    |
| totalPosts   | total number of posts for this given layout           |
| layout       | layout for these posts (may not be 'post')            |
| pagination   | special attribute for the paginator                   |
