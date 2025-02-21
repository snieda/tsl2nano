------------------------------
Nano.H5 environment directory
------------------------------

This folder holds all content for the application tsl2.nano.h5 and is known by the java classpath. 
all jar files will be added to the classpath - in an alphabetic order. most of all files will be
re-created if you delete them.

NOTE: 
  - the classloader tries to get all missing libraries through jarresolver - this may result in long loading times.
  - the automatic translation tries to do online translation
  => to avoid that, set system-property: 'tsl2nano.offline=true'

the main libraries are: 
	ant, jpa-persistence-provider with dependencies, the generated bean jar file, database drivers etc.
others:
	database ddl scripts, authentication: users.xml, authorization: *-permissions.xml, persistence properties: persistence-bean.xml

The following files are used to generate database and beans:
- ant*.jar         ant libraries
- mda.*            ant-scripts using shell.xml to build database and beans jar file
- shell.xml        ant base scripts
- reverse-eng.xml  ant script parts for hibernate-tools and openjpa
- hibernate*.*     (if used hibernate-tools)
- *.sql            (*anyway.sql if using standard database)

For persistence, user login (authentication+authorization) the following files are used:
- META-INF/persistence*.*ml  jpa templates     
- persistence-bean.xml       jpa+login properties
- peristence.properties      jpa+login properties
- jdbc-connection.properties jdbc properties
- users.xml                  (secure: will only be re-created in admin mode!)
- *-permission.xml           (secure: will only be re-created in admin mode!)

Other files:
- environment.xml                    main application environment property file
- network.classloader.unresolvables  classes/libs that couldn't be resolved/downloaded through jarresolver
- runServer.cmd                      generated script to start the selected local database
- logfactory.xml                     log properties
- apploader.log                      application starting log
- environment.log                    standard application log

SUB-FOLDERS:

META-INF
==============
holding jpa persistence template files and the resulting persistence.xml file - used by the o/r mapper like openjpa and hibernate

specification:
==============
holds all definitions having any logic to be creatable on specification time. used by presenters
see specification/readme.txt

presentation:
==============
holds all presentation informations for all entities. virtual entities are inside the sub-folder: virtual. 
if deleted it will be re-created with defaults by the application.

doc:
==============
contains generated html and svg documents for all entities

icons:
==============
all images to be used by html pages

temp:
==============
temporary files like backups, connection-informations, user-session-definitions and attachments

css:
==============
holding optional styling file 'meta-frame.html' and it's contents. not esed by default!

generated-src:
==============
all generated java sources. java sources are generated to create a new bean jar file to be used by the given jpa-persistence-provider.
the generation starts on using a database connection without having jpa-entities in the classpath. the generation is done by the application
using the script 'mda.xml'.

generated-bin:
==============
compiled java sources from generated-src.

lib:
==============
unused...

maven
==============
exists only, if you are not in "tsl2nano.offline" mode and some dependencies could not be resolved. then, the jarresolver tries to 
reolve the dependencies through maven on runtime. maven libraries are downloaded before into this folder. if you use the tsl2.nano.h5 virgin
library, "tsl2nano.offline=false" will be standard to resolve through maven.
