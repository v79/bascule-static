# Bascule
Static site generator inspired by jekyll and griffin

Bascule merges Handlebars templates with Markdown content to generate static websites. It is used to generate my personal site, [liamjd.org](https://www.liamjd.org).

To start a new website, download the bascule-0.5.5-jar file into an appropriate folder. Then execute the command:

`java -jar bascule-0.5.5.jar --new <projectname>`

This will create a folder `<project>`, with four subfolders (`assets`, for images, CSS and JavaScript, `bulma/templates`, the default theme for the project, `site`, an empty folder for generated content, and `sources`, the home for all your Markdown content). I recommend that you copy the bascule jar file into your project folder.

> Bascule starts you up with a very basic template based on the Bulma CSS framework. But it is recommended that you create your own theme folder.

Bascule also creates the project definition file `<project>.yaml`, which contains the project configuration.

```yaml
siteName: Bascule Site
dateFormat: dd/MM/yyyy
dateTimeFormat: HH:mm:ss dd/MM/yyyy
author: bascule
theme: bulma
postsPerPage: 5
postLayouts: [post]

directories:
  source: sources
  output: site
  assets: assets
  templates: bulma/templates
plugins: []
processors: [IndexPageGenerator,PostNavigationGenerator,TaxonomyNavigationGenerator]
```

The key values to change are `siteName` and `author`, and if you are customising your theme, change `theme` and `templates`.

The default values are required. You can add your own custom properties to this list, and they will be available as variables in your templates.

### Your first blog post

Create a simple Markdown file `sources/my-first-post.md` and add some content. Then run `java -jar bascule-0.5.5.jar generate`. In the `site` folder you should see your generated website, complete with an index page, a blog post page `my-first-post.html`, and more.

Bascule as made some sensible decisions about the details of your first blog post, but it is highly recommended that you add some metadata to the source using Markdown. (E.g. the page title is `my-first-post`, whereas `My First Post` would probably be a better choice.)

Edit `sources/my-first-post.md` to include metadata at the top of the file by adding the following lines:

```yaml
---
title: My First Post
layout: post
author: Joe Smith
date: 10/07/2026
slug: first-post
tags: [welcome]
---
```

Put the body of your blog post after the last `---` line. Bascule will look for `title`, `layout`, `date`, `slug`, and `tags` but if any are missing it will try to generate a sensible default. `author` isn't required at all, and in fact bascule does not know about authorship. But any additional properties you add to the YAML at the top of the post will be available as variables in your Handlebars templates in the project theme.

Regenerate the project, and you'll see a much richer, more useful output in the `/site` folder.

### Folder structure

There's no particular folder structure mandated by Bascule – it will simply loop through all the Markdown files it can find, recursively. Good practice would be to create a folder per year, and perhaps a subfolder per month. Choose a structure that makes sense to you.

### Pages and Posts

Bascule distinguishes between pages and posts. "Posts" are sequential, dated content items that all follow the same layout – your typical blog posts. "Pages" are static content like "About" or "Contact" pages. Bascule distinguishes between the two by the `layout` property in the YAML at the start of the Markdown file. If the `layout` is set to `post`, or is missing, it is assumed to be a blog post item. If it has another value, bascule will look for a corresponding handlebars template with the name `<theme>/templates/<layout>.hbs`.

When the website is generated, a `site/posts/list<X>.html` is generated, which contains a paginated list of all your blog posts.

> [!NOTE]
> Technically, blog posts do not need to have a template called _post_ – Bascule looks for a template with a layout found in the `postLayouts` array in `<project>.yaml`. But if there's nothing specified in `postLayouts`, or if no layout is given in the YAML for a file, it will assume `post` and look for a template `<theme>/templates/post.hbs`. So it's _highly_ recommended that you stick to the default of `post` for your blog post items.

Bascule will skip any file or folder whose name starts with `__` or with `.`. Use these for any draft posts you do not want to be generated.

> [!CAUTION]
> The default bascule theme does not provide a Handlebars template for static pages. 

### Tagging

Bascule supports a powerful tagging mechanism. If a Markdown file is given a `tags: [myTag1, myTag2]` property, then Bascule will generate a listing page for each tag specified - `site/tags/myTag1.html` and `site/tags/myTag2.html`. It will also generate a page listing all the tags on the site, `site/tags/tags.html`. During the generation phase, Bascule remembers all the tags from every source file, and then generates the tag list pages in a separate step.

### Theming

The templates for your project are written using the [Handlebars](https://handlebarsjs.com/guide/) templating language. Bascule provides a default set of templates that generate a reasonable but plain project using a old version of the [Bulma CSS framework](https://bulma.io/). You'll almost certainly want to create your own theme and your own templates.

Create a folder for your theme `my-theme` and in that, a folder called `templates`. You must supply the four key handlebars files of `index.hbs`, `post.hbs`, `taglist.hbs` and `list.hbs`. Without these Bascule will fail with an error.

Bascule provides a few common Handlebars extension functions – _capitalize_, _upper_, and _slugify_, plus a couple of bespoke functions `forEach` for loops, and `localDate` to provide date formatting options.

See my [own website theme folder](https://github.com/v79/liamjd-web/tree/master/liamjd-theme/templates) for a much more extensive set of templates. I use the `{{ > }}` partials extensively to keep my templates modular and maintainable.

### Generators

Bascule supports a modular Generators pipeline, allowing you to further customise the generator process. Three generators are provided by default: **IndexPageGenerator**, **PostNavigationGenerator** and **TaxonomyNavigationGenerator**. They are responsible for the index page, the blog post list pages, and the tagging pages respectively. These are listed as `processors` in `<project>.yaml` and you could, for example, disable tag generation by removing `TaxonomyNavigationGenerator` from the project.

You can also write your own generators and add them to the pipeline, following the interface defined in the separate [bascule-lib](https://github.com/v79/bascule-lib) project. My personal website uses a custom generator to create a [lunr.js](https://lunrjs.com/) search index from the website content. You could also write a generator which generates PDFs or other, non-HTML content from your source files. Examples are in the [bascule-extras](https://github.com/v79/bascule-extras) project.

Custom generators need to be provided as JAR files in the `<project>/plugins` folder. Custom plugins must be specified using their fully qualified java class name.

You can even provide custom Handlebars extension functions in the plugins folder. They are listed in the `extensions:` section of the project definition file.

> [!WARNING]
> TODO: Document the generator interface
> 
> [!WARNING]
> TODO: Make more use of custom generators
> 
> [!WARNING]
> TODO: Verify and document Handlebars extension loading

### Caching

As Bascule generates your site, it will create a cache file representing the overall structure of the site called `sources/<project>.cache.json`. On subsequent runs, Bascule will load this file and compare the last modified date for each Markdown source file with the value in the cache. If they match, the generation for that file will be skipped. This can speed up generation on large sites where content does not change much. The cache will also track the modification time of the Handlebars templates, and will force a regeneration of all content if a corresponding template has been updated.

You can disable caching by passing the `--clean` parameter to the command line when generating the website. The cache is not useful when bascule is run as part of an automated pipeline in GitHub Actions or equivalent.

If the cache file is missing or corrupted, Bascule will perform a clean generation.

### Error handling

If there are any fatal errors during the generation process (such as a missing template file), then an error log and stack trace will be written to `<project>.log`. If a file cannot be parsed, it will be skipped and a message will be shown, but it will not normally cause the site generation to abort.

### Working with multiple blog post layouts

> TODO explain how to handle multiple blogging layouts

### Project model

> [!WARNING]
> See [ProjectModel.md] for a full list of the standard elements available to handlebars templates.
