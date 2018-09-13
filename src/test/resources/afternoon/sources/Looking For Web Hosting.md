---
title: Looking for Web Hosting
author: Liam Davison
layout: post
date: 31/12/2014
tags: [software development, cms]
slug: looking-for-web-hosting
---
I've been looking for a simple way to run **Cinnabar** on a live hosted website, purely for development and demonstration purposes. I had been using a free service from Cloudbees, but they have withdrawn the service (I really should unsubscribe from their mailing lists now...). I also want it to be free, or cost just pennies. I'd hoped that I'd be able to set up an account with Amazon Web Services or Google Compute Engine - after all, cloud computing is pretty ubiquitous these days.

I signed up to AWS and gave them my credit card details, though I hope that I will fit under their free usage pricing tier. AWS's _Elastic Beanstalk_ provides a simple Linux instance with a Tomcat and Java 7 server, which should be ideal for the Grails-based Cinnabar application. I used the grails command `grails prod war` to build a .war file and deployed it to the AWS Linux instance. So far so good. Of course, Cinnabar is nothing without a database. I've been using [MariaDB](https://mariadb.org/) locally, but Amazon's _RDS_ service provides a compatible MySQL database layer, and provides 5GB of storage for free. I set up the MySQL instance and after a bit of poking around with port numbers and IP addresses, I could access the RDS from the MariaDB command line on my local machine, or from my preferred SQL client, [HeidiSQL](http://www.heidisql.com/).

Application deployed, server running, database connected - that should just work? Well, sadly not. I downloaded the logs from AWS and it was clear that Cinnabar could not connect to my database at all. I had set up all the details in Grails' `DataSource.groovy` file, but AWS seemed to be blocking access to the DB. After that, I got confused - AWS has all sorts of security features with terms like VPC and security groups source points and other things I never really understood. In the end, I just couldn't get it to work.

Particularly frustrating is that Elastic Beanstalk does not provide any easy access to the running server logs - you have to download a .ZIP file containing the contents of your instance's `/var/logs` folder. So every time I made a change and redeployed the application, I'd have to download a new .ZIP file and extract the logs. Cloudbees provided a simple web view of Tomcat's `catalina.out` file, and the Cloudbees command line program even let me run `tail -f` on the logs locally. It's definitely a weakness of AWS for new users.

I am trying again - as I type AWS is provisioning a new linux instance and MySQL database for me. I might also try Google's Compute Engine, but I'm unsure if it provides a normal Linux/MySQL/Java7 stack.