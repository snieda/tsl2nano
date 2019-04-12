echo "=========================================================================="
echo "Windows ant-shell to start ant-scripts of shell.xml"
echo "Author: Thomas Schneider / cp 2012"
echo "help:"
echo "  call 'runsh.sh -help' to see all ant helps and diagnostics."
echo "example:"
echo "  runsh.sh sh.java -Darg1=mypath.MyUnitTest"
echo "  runsh.sh sh.java -Djava.classname=mypath.MyMainClass -Darg1=myArg1"
echo "  runsh.sh sh.jar"
echo "  runsh.sh sh.unjar"
echo "  runsh.sh -prop my.properties sh.xml.xsl"
echo "=========================================================================="

if [ exist ant.sh ] then
	set ANTRUNNER=ant.sh
else
	if [ "$ANT_HOME" == "" ] then
		set ANTRUNNER=java -jar ant-launcher.jar
	else
		set ANTRUNNER=ant.sh
	fi
fi

if [ "$1" == "-help" ] then goto HELP fi
if [ "$1" == "-?" ] then goto HELP fi
if [ "$1" == "/?" ] then goto HELP fi

if [ "$1" == "-prop" ] then goto LOAD_PROPERTIES fi
if [ "$1" == "-properties" ] then goto LOAD_PROPERTIES fi
if [ "$1" == "-propertyfile" ] then goto LOAD_PROPERTIES fi

echo on
call $ANTRUNNER -buildfile shell.xml $1 $2 $3 $4 $5 $6
goto END

:LOAD_PROPERTIES
call $ANTRUNNER -propertyfile $1 -buildfile shell.xml $2 $3 $4 $5 $6
goto END

:HElP
call $ANTRUNNER -diagnostics
REM call $ANTRUNNER -help
ant.sh -projecthelp -buildfile shell.xml
call $ANTRUNNER -buildfile shell.xml man $2 $3 $4 $5 $6
:END
