#!/bin/bash
javac -g -cp "generated-bin:*" -d generated-bin  -sourcepath generated-src $(find generated-src -name *.java)
[[ "$?" == "0" ]] && jar cvf generated-model.jar -C generated-bin/ . && echo "jar CREATED SUCCESSFUL" || echo "ERRORS OCCURRED!"
