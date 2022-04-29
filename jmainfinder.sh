#!/bin/bash
# find all main classes in all subprojects and call for help

output="$(pwd)/jmain-info.txt"
tmp="$(pwd)/jmain-tmp.txt"
classpath=.
for basedir in $(find . -type d -name classes); do
    cd $basedir
    classpath=$classpath:$basedir
    echo " ==> BASEDIR: $basedir"
    for class in $(find . -name "*.class" -printf "%P\n"); do
        (java -cp $classpath ${class%.*} >$tmp && [ $? == 0 ] && echo < $tmp >> $output)
    done 
done

echo "<== finished. results written to $output"