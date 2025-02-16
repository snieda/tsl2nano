Specifications
========================
holds all definitions having any logic to be creatable on specification time.
these entries can be used by presenters to create actions, attributes, entitities, compositors, controllers and sheets
	- action: any action to be added as button on an entity - or simply usable as evaluation inside an attribute
	- rule: simple rules with simple math or logic expressions
	- rulescript: simple javascript expressions
	- ruledecisiontable: decisiontables to be uesd as rule inside an attribute
	- query: any sql queries
	- webclient: any web services calls - including rest
	- pflow: simple workflows

additionally, here are some example specifications (type: properties or csv or markdown) to create specification entries 
and to extend the running application with e.g. icons, queries/statistics, compositors etc.

to activate this samples, you have to copy the specification to the environemnt folder. the name has to be: 
    specification.properties[.csv|-documentworker.md.html]
with:
- specification.properties             : for each bean and its attributes, there are generated (disabled) entries.
- specification.csv                    : more complex and structured way to define specifications
- specification-documentworker.md.html : a full markdown specification document

the framework will move the resolved specification file back to the specification folder with extension '.done'. 

NOTE:	rule-scripts can be any scripts known by the java script engine. until jdk 8, the script engine (like rhino, nashorn) was included.
		now, we have to add the script engine manually!


for further informations, read the documentation 'nano.h5.md.html' and the readme.txt inside the environemnt folder above.


The Timesheet Sample
========================

to see the specification part working, we provide specifications (together with some util classes) to create a simple timesheet.
precondition is the existence of a script engine like 'nashorn' with its dependencies. But this is now included in the main jar file.

to activate it - after the application is running and you just have logged in - you should do the following steps:

- copy the file specification/specification.properties-timesheet.csv to the environment folder above as specification.properties.csf
- do a log out (Menu -> Session -> Logout)
- re-login and answer the specification request with 'Yes'
- Now, having the timesheet sample running, we can import some data:
	- copy the content of the file timesheet-classes.zip to ../generated-bin 
	- copy the file specification/specification-documentworker-timesheet.md.html to the environment folder above specification-documentworker.md.html
	- re-login and answer the specification request with 'Yes'

to simplify the steps, we provide a script 'create-sample-timesheet.sh'.
