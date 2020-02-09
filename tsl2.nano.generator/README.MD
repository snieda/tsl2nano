# TSL2 Generator

## Introduction

You can start the generator via *java -jar {tsl2.nano.generator-x.x.x.jar}* or directly through *de.tsl2.nano.codegen.ACodeGenerator.main()*.

A more convenient way is to use the maven plugin *tsl2.nano.mavengenerator*.

## Tutorial

TODO

    #foreach( $att in $class.Attributes)    
                            <span class="th">${att.Name}</span>
    #end

## using the $util as implementation of GeneratorUtiltiy

### util implmentation classes

there are three implmentations of Utitly classes. Each Generator implementation/algorithm may use an own utility implementation.

* *ClassGenerator*  : GeneratorBeanUtility
* *PackageGenerator*: GeneratorBeanUtility
* *XmlGenerator*    : GeneratorXmlUtility
* PropertiesAsConstantsGenerator*: GeneratorBeanUtility

### GeneratorUtility

Base implementation. Provides string transformations, and transfomrations to classes, instances or beans (providing all properties of a class per reflection).

#### method reference with examples

TODO

generate method reference:
CODEBLOCK="\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
grep -C1 -E "([ /]\*[ \*/])|(public.*\(.*\).*\{)" src/main/java/de/tsl2/nano/codegen/GeneratorUtility.java | echo "$CODEBLOCK\t" {} "$CODEBLOCK" >> reference.md 

generate examples:
cat src/main/java/de/tsl2/nano/codegen/GeneratorUtility.java | sed -E "s/public \w+ (\w+)\(.*\).*\{/$1/p" | xargs grep -e {} src/main --exclude=*.html --exclude=*.java


### GeneratorBeanUtility

### GeneratorXmlUtility



### Tutorial

* *algorithm* : full class name of the generators class (default: de.tsl2.nano.codegen.PackageGenerator)
* *template*  : path to your velocity template. resources in jar files will be found, too.
* *model*     : file path to the package to load the source (model) classes from. may point to a jar file.
* *filter*    : optional package or class filter, interpreting regular expressions
* *outputPath*: optional output path for the generated source classes (default: src/gen)

the package generator will loop over all found classes given by your *model* and *filter* parameters, calling the velocity engine with the given *template* and filling each *model* class as bean into the velocity context.

on generation time, all dependencies of your source model classes must be available.

#### create a velocity template

* velocity user guide: https://velocity.apache.org/engine/1.7/user-guide.html

the mavengenerator provides some additional evaluations, to get informations from the source model on runtime.

this is done by utitliy class, invoked as *$util*


here are some examples:

##### exclude unwantet or unfiltered model classes

    #if (!$model.Name.endsWith("MyModel") || $model.Name.startsWith("Abstract"))
    #break
    #end

this will stop the generation of that file at that position. normally you would use the *filter* in your pom instead.

##### load and initialize weld-se as CDI reference implementation

    #set ($cdi = ${util.get("obj:org.jboss.weld.environment.se.Weld").initialize()})

##### load a class to be used in your template

    #set( $myFactoryClass = ${util.get("cls:my.org.any.MyFactory")})

##### instantiate an object of a  class to be used in your template

    #set( $myFactory = ${util.get("obj:my.org.any.MyFactory")})

##### call any method of that instance giving the model class and evaluating the first element of a list

    #set( $myMethodCall = $myFactory.myMethod($model.Clazz).get(0))
