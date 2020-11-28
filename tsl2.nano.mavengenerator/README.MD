# TSL2 Generator-Maven-Plugin

Is able to generate code through a package of classes in the classpath and their bean informations as values -  and a velocity template.

Example:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    mvn net.sf.tsl2nano:tsl2.nano.mavengenerator:run -Dbean.generation.packageFilePath=target/classes/org/anonymous/project -Dbean.generation.templateFilePath=codegen/beanconst.vm
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This will generate interfaces with constants for class members for all classes in the package or.anonymous.project (must be in the classpath)

## System-Properties

+ bean.generation.packagename     : only class in that package
+ bean.generation.outputpath      : output base path (default: src/gen)
+ bean.generation.nameprefix      : class+package name prefix (default: package + code-template) may be a regex of model
+ bean.generation.namepostfix     : class name postfix (default: {code-template}.java) may be a regex of model
+ bean.generation.unpackaged      : no package structure from origin will be inherited (default: false)
+ bean.generation.singlefile      : generate only the first occurrency (default: false)
+ bean.generation.instanceable    : filter all not instanceable classes
+ bean.generation.annotated       : annotation to search for. all not annotated classes will be filtered
+ bean.generation.instanceof      : base/super class. all not extending classes will be filtered
+ bean.generation.keyworklist     : list of comma-separated keywords to be replaced through keyword.replacement
+ bean.generation.keywordrepl     : list of csv keyword replacements. may be a single string to be added to all keywords
+ bean.generation.catchedclass    : class name not throwing exceptions on method 'cachedmethod'
+ bean.generation.catchedmethod   : method name not throwing exceptions on class 'cachedclass'

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
                </executions>
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
                <version>2.3.1</version>
                <inherited>true</inherited>
                <dependencies>
                    <dependency>
                        <groupId>net.sf.tsl2nano</groupId>
                        <artifactId>tsl2.nano.core</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.velocity</groupId>
                        <artifactId>velocity</artifactId>
                        <version>1.7</version>
                    </dependency>
                    <dependency>
                        <groupId>net.sf.tsl2nano</groupId>
                        <artifactId>tsl2.nano.generator</artifactId>
                        <version>2.3.1</version>
                    </dependency>
                    <!-- add all dependencies, your source model needs, here -->
                    <!--DEP-INSERTION-->
                    </dependencies>
                <executions>
                    <execution>
                        <goals>
                            <goal>run</goal>
                        </goals>
                        <configuration>
                            <template>target/classes/mytemplate.vm</template>
                            <model>${project.build.outputDirectory}/my.anonymous.project</model>
                            <filter>my.anonymous.project.[A-Z]+MyPostfix</filter>
                            <outputPath>target/generated-sources</outputPath>
                        </configuration>
                    </execution>
                <executions>
            </plugin>
        <plugins>
    </build>

If you need to be more detailed, there are more configuration properties:

            <configuration>
            <!-- load only classes as source model, that have a default constructor and are not abstract! -->
                <filterInstanceable>true</filterInstanceable>
            <!-- load only classes as source model, that are an instance of the given class -->
                <filterInstanceOf>my.anonymous.project.MyBaseClass</filterInstanceOf>
            <!-- load only classes as source model, having the given annotation -->
                <filterAnnotated>javax.persistence.Entity</filterAnnotated>
            <!-- replace all keywords in the keyword.list with keyword.replacement -->
                <keyword.list>if, else, case, switch</keyword.list>
            <!-- the replacements are either a list for all items in the keyword.list - or only one element, to be added to all keywords -->
                <keyword.replacement>if_, else_, case_, switch_</keyword.replacement>
            <!-- replace the default package path + class prefix with the given one. this may be a regex on the model-file-path -->
                <destinationPrefix></destinationPrefix>
            <!-- replace the default class postfix with the given one -->
                <destinationPostfix>MyClassPostfix</destinationPostfix>
            </configuration>

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

## define catching exception handler DontEscalateExceptionHandler

	#set ($catchedClass = $util.get("cls:java.lang.System").setProperty("bean.generation.catchedClass", "de.my.package.MyGeschaeftsvorfall") )
	#set ($catchedMethod = $util.get("cls:java.lang.System").setProperty("bean.generation.catchedMethod", "getMyData") )

##### return a list of enums

{code:java}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public Set<MyEnum> getMyEnums() {
		return EnumSet.of( ${util.toString(${MyEnumCollection.getEnumColleciton($model.Clazz)} )});
	}
{code}~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

##### use a for loop to catch all possible method call arguments

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

#### Adding the new source path to the project

        <plugin> <!-- add the source path 'generated-sources' to the build -->
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>build-helper-maven-plugin</artifactId>
            <version>3.0.0</version>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>add-source</goal>
                    </goals>
                    <configuration>
                        <sources>
                            <source>${project.build.directory}/generated-sources</source>
                        </sources>
                    </configuration>
                </execution>
            </executions>
        </plugin>

#### Formatting and cleaning the generated sources

            <plugin> <!-- clean and format the generated source code with google-format -->
                <groupId>com.coveo</groupId>
                <artifactId>fmt-maven-plugin</artifactId>
                <version>2.9</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>format</goal>
                        </goals>
                        <configuration>
                            <sourceDirectory>${project.build.directory}/generated-sources</sourceDirectory>
                            <verbose>true</verbose>
                            <filesNamePattern>.*\.java</filesNamePattern>
                            <additionalSourceDirectories>
                                <param>generated-sources</param>
                            </additionalSourceDirectories>
                            <skip>false</skip>
                            <skipSortingImports>false</skipSortingImports>
                            <style>google</style>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

#### Moving the generated sources to their packages

If you are on linux, the following script may move your generated files to their correct package pathes:

    #!/bin/bash

    moveToPackage() {
        # Get the package name
        package=$(grep -m 1 -Po "(?<=^package )[^; ]*" "$f")

        # Replace . with / and add src/ at the beginning
        target_folder="generated-sources/${package//./\/}"

        # Create the target folder
        mkdir -p "$target_folder"

        # move the file to the target folder
        mv -uv "$f" "$target_folder"
    }

    #Loop through the generated files
    for f in generated-sources/my/anonymous.project/*.java; do
        moveToPackage
    done
