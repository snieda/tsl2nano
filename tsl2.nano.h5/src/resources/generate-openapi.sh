#!/bin/bash
# generate openapi specifiation or client class from specification (Thomas Schneider / 2025)
# example: generate-openapi.sh https://github.com/bump-sh-examples/train-travel-api/raw/refs/heads/main/openapi.yaml

DEF_OPENAPI="openapi.yaml"
[[ "$1" == "spec" ]] && GENERATE_SPEC=1 || OPENAPI=${1:-$DEF_OPENAPI}

[[ "$1" == "" ]] && [ ! -f $DEF_OPENAPI ] \
	&& echo "usage: $0 [spec|<url-to-openapi>]" \
	&& echo "       if no parameter was given, the file $DEF_OPENAPI will be used" \
	&& echo "ERROR: NO $DEF_OPENAPI FILE FOUND!" \
	&& exit 1

# only needed, if using standard pom.xml name
#if [ -f pom.xml ]; then mv pom.xml pom.xml.$(date +%Y%m%d_%H%M%S); fi
#cp pom-openapi.xml pom.xml

if [[ $GENERATE_SPEC != "" ]]; then
	echo "Generating openapi.json from classes"
	. mvnw -f pom-openapi.xml com.github.kongchen:swagger-maven-plugin:3.1.8:generate
	
else
	echo "Generating java client classes from $OPENAPI"
	if [[ $OPENAPI != $DEF_OPENAPI ]]; then
		if [ -f $DEF_OPENAPI ]; then mv $DEF_OPENAPI $DEF_OPENAPI.$(date +%Y%m%d_%H%M%S); fi
		curl -kL -X GET $OPENAPI -o $DEF_OPENAPI
	fi
	
	# . mvnw io.swagger:swagger-codegen-maven-plugin:generate
	bash -c "
	. mvnw -f pom-openapi.xml org.openapitools:openapi-generator-maven-plugin:7.12.0:generate \
		-Dopenapi.generator.maven.plugin.inputSpec=openapi.yaml \
		-Dopenapi.generator.maven.plugin.generatorName=java \
		-Dopenapi.generator.maven.plugin.sourceFolder=generated-src \
		-Dopenapi.generator.maven.plugin.output=generated-src"

	echo "compiling and creating beans jar file"
	bash -c ". mvnw -f pom-openapi.xml dependency:copy-dependencies -DoutputDirectory=./"
	mv --update=all generated-src/src/main/java/org generated-src
	. compilejar.cmd generated-src/src/main/java/org/openapitools/client/model/*.java
fi
