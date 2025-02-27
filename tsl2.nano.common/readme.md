*tsl2.nano.common*
<font size="-1">Autor: Thomas Schneider 2009-2021</font>

{toc}

# Introduction

It is a framework as base of all tsl2.nano packages. The goal is the lightweight, having no direct dependencies to other libraries and to be usable on different java vms like the oracle standard vm and androids dalvik vm. Providing a lot of elegant solutions it doesn't bind the user to it - using other libraries and frameworks beside isn't any problem.

There are utility classes to work on javaassist, velocity, ant-scripts and some xml libraries, but they are separated through a compatibility layer. So they are only accessible, if you provide the third pary libraries inside the environments workspace directory. 

## Features

* Resource bundling with "Messages":src/main/de.tsl2.nano/Messages.java
* Different Implementations of "IAction":src/main/de.tsl2.nano/action/IAction.java to be describtors for creating GUI-Buttons
* Exception-handling through "FormattedException":src/main/de.tsl2.nano/exception/FormattedException.java and "ForwardedException":src/main/de.tsl2.nano/exception/ForwardedException.java
* Historizing of Text-Elements through "HistorizedInputFactory":src/main/de.tsl2.nano/historize/HistorizedInputFactory.java
* *Bean-Implementations* for generalization - different to apache-implementations - see "BeanAttribute":src/main/de.tsl2.nano/util/bean/BeanAttribute.java
* "ClassGenerator":src/main/de.tsl2.nano/util/codegen/ClassGenerator.java to generate bean presenter base or const classes (using Velocity).
* A lot of Utility-Classes for scripting, file-access, string-access, dates, caches, class-loading, formatting etc.
* utility classes to start *executables* *ant-scripts*, *beanshell*, *tslbase*, *velocity* scripts. encapsulated through a compatibility layer: usable only if you provide the third party jars.
* numbers with unit implementations. extended currency implementation, respecting *historized currencies*.
* bean based byte code enhancing trough *javassist*
* base generic implemenation for *event/messaging*
* base interfaces/implementions for *operations*
* *dynamic proxy* implemenations
* performance optimized collections (*segmented lists*)
* object *formatting and parsing* utlities
* *enhanced classloaders*
* *enhanced runner* definitions
* *application loader* working with own extended classloader (with dynamic classloader on runtime - loading classes of jars inside a jar) 
* en-/decryption
* printing, from xml through jasper or fop (see XmlUtil and PrintUtil)
* generic undo/redo and macros (see incubation/repeat)
* net utils to do downloads, uploads, port scans and wget

## The AppLoader and the Environment

The AppLoader helps you defining a main class with arguments, help and an own classloader. The Environment is a singelton holding a pointer to a directory to store all configurations of an application. The environment provides all properties and services of the application. It provides resource bundles, the classloader etc. in a simplified way.It may be used as a central implementation helper.

The mechanisms are similar to _OSGI_, but _OSGI_ was not used or implemented to be as small as possible.

## Beans and Bean-Attributes

The most enhanced package is the bean package. It provides to enrich any pojos or entities with informations to describe a presentation in any gui. Formatting data, creating dependencies, a simple databinding are some of the advantages to create class-describtors without any dependencies to other libraries.
 
## ToDos

* rename project to be independent of tsl2nano
* refactoring: move *bean* package out of *util*
* refactore a new *cache* package

## Deployment

If you change sources of this plugin, you should start the following ant scripts: 
* _2nano.xml_ with target _distrubute.2nano_.

