javac -cp * -sourcepath "generated-src" -d generated-bin generated-src/org/anonymous/project/*.java
jar cvf generated-model.jar -C generated-bin/ .
pause