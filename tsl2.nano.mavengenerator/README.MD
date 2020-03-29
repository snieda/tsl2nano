# TSL2 Generator-Maven-Plugin

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

### Tutorial

#### use the mavengenerator plugin in your maven pom

add the following plugin configuration to your pom:

            <plugin>
                <groupId>net.sf.tsl2nano</groupId>
                <artifactId>tsl2.nano.mavengenerator</artifactId>
                <version>2.3.1</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>run</goal>
                        </goals>
                        <configuration>
                            <template>src/main/resources/gen-postfix.vm</template>
                            <model>${project.build.outputDirectory}/WEB-INF/classes/my.org.project</model>
                            <filter>my.org.project.[A-Z]+MyModel</filter>
                            <outputPath>target/generated-sources</outputPath>
                        </configuration>
                    </execution>
                <executions>
            </plugin>

this would be a minimalistic configuration to do a simple package generation.

* *algorithm* : full class name of the generators class (default: de.tsl2.nano.codegen.PackageGenerator)
* *template*  : path to your velocity template. resources in jar files will be found, too.
* *model*     : file path to the package to load the source (model) classes from. may point to a jar file.
* *filter*    : optional package or class filter, interpreting regular expressions
* *outputPath*: optional output path for the generated source classes (default: src/gen)

the package generator will loop over all found classes given by your *model* and *filter* parameters, calling the velocity engine with the given *template* and filling each *model* class as bean into the velocity context.

on generation time, all dependencies of your source model classes must be available. So, you have to provide all these dependencies in your plugin-dependencies block!

at the end, the plugin part may look like this:

    <build>
        <plugins>
            <plugin>
                <groupId>net.sf.tsl2nano</groupId>
                <artifactId>tsl2.nano.mavengenerator</artifactId>
                <version>2.3.1-SNAPSHOT</version>
                <inherited>true</inherited>
                <dependencies>
                    <dependency>
                        <groupId>net.sf.tsl2nano</groupId>
                        <artifactId>tsl2.nano.core</artifactId>
                        <version>2.3.1-SNAPSHOT</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.velocity</groupId>
                        <artifactId>velocity</artifactId>
                        <version>1.7</version>
                    </dependency>
                    <dependency>
                        <groupId>net.sf.tsl2nano</groupId>
                        <artifactId>tsl2.nano.generator</artifactId>
                        <version>2.3.1-SNAPSHOT</version>
                    </dependency>
                    <!--DEP-INSERTION: add all dependencies, your source model needs, here -->
                    </dependencies>
                <executions>
                    <execution>
                        <goals>
                            <goal>run</goal>
                        </goals>
                        <configuration>
                            <template>src/main/resources/configurator.vm</template>
                            <model>${project.build.outputDirectory}/WEB-INF/classes/de.lv1871.shared.model.persistent.vertrag.gevo</model>
                            <filter>de.lv1871.shared.model.persistent.vertrag.gevo.[A-Z]+GeVo</filter>
                            <outputPath>target/generated-sources</outputPath>
                        </configuration>
                    </execution>
                <executions>
            </plugin>
        <plugins>
    </build>

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

#### use a for loop to catch all possible method call arguments

{code:java}
    public MyResult getType(MyEnumType myType) {
        switch (myType) {
        #foreach ($type in $availableEnumTypes)
            #if ($type != MyEnumType.UNKNOWN)
            case $type: // $typeInfo.getEntry($type)
                return ${MyEvaluation.eval($type)};
            #end
        #end
            default:
                throw new IllegalArgumentException("unknown MyEnumType: " + myType);    
        }
    }
{code}
