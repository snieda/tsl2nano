------------------------------
Nano.H5 environment directory
------------------------------

This folder holds all content for the application tsl2.nano.h5 and is known by the java classpath. 
all jar files will be added to the classpath - in an alphabetic order.

sub-folders:

specification:
==============
holds all definitions having any logic to be creatable on specification time.
these entries can be used by presenters to create actions, attributes and entitities.
	- rules: simple rules with simple math or logic expressions
	- rule-scripts: simple javascript expressions
	- queries: any sql queries
	- web: any web services calls - including rest

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
holding optional styling file 'meta-frame.html' and it's contents.

generated-src:
==============
all generated java sources. java sources are generated to create a new bean jar file to be used by the given jpa-persistence-provider.
the generation starts on using a database connection without having jpa-entities in the classpath. the generation is done by the application
using the script 'mda.xml'.

generated-bin:
==============
compiled java sources from generated-src.
