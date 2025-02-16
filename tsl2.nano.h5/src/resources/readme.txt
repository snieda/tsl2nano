------------------------------
Nano.H5 environment directory
------------------------------

This folder holds all content for the application tsl2.nano.h5 and is known by the java classpath. 
all jar files will be added to the classpath - in an alphabetic order.

NOTE: the classloader tries to get 
	all missing libraries through jarresolver - this may result in long loading times.
	and the automatic translation tries to do online translation through 
		to avoid that, set system-property: 'tsl2nano.offline=true'

the main libraries are: 
	ant, jpa-persistence-provider with dependencies, the generated bean jar file, database drivers etc.
others:
	database ddl scripts, authentication: users.xml, authorization: *-permissions.xml, persistence properties: persistence-bean.xml


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
contains generated html documents for all entities

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