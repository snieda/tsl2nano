# Generator-Maven-Plugin

Is able to generate code through a package of classes in the classpath and their bean informations as values -  and a velocity template.

Example:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    mvn net.sf.tsl2nano:tsl2.nano.mavengenerator:run -Dbean.generation.packageFilePath=target/classes/org/anonymous/project -Dbean.generation.templateFilePath=codegen/beanconst.vm
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This will generate interfaces with constants for class members for all classes in the package or.anonymous.project (must be in the classpath)

## System-Properties

+ bean.generation.packagename     : only class in that package
+ bean.generation.outputpath      : output base path
+ bean.generation.nameprefix      : class+package name prefix (default: package + code-template)
+ bean.generation.namepostfix     : class name postfix (default: {code-template}.java)
