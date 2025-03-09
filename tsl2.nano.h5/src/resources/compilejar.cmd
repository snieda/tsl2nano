#!/bin/bash
javac -g -verbose -cp "generated-bin:*" -d generated-bin  -sourcepath generated-src $(find generated-src -name *.java)
[[ "$?" == "0" ]] && jar cvf generated-model.jar -C generated-bin/ .
