#!/bin/bash
javac -g -verbose -cp "*" -d generated-bin  generated-src/org/anonymous/project/*.java $1 $2 $3
jar cvf generated-model.jar -C generated-bin/ .
