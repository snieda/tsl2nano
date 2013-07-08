@ECHO OFF
echo ==========================================================================
echo Windows ant-shell to start ant-scripts of shell.xml
echo Author: Thomas Schneider / Thomas Schneider 2012
echo help:
echo   call 'run.bat -help' to see all ant helps and diagnostics.
echo example:
echo   run.bat sh.java -Darg1=mypath.MyUnitTest
echo   run.bat sh.java -Djava.classname=mypath.MyMainClass -Darg1=myArg1
echo   run.bat sh.jar
echo   run.bat sh.unjar
echo   run.bat -prop my.properties sh.xml.xsl
echo ==========================================================================
if "%1"=="-help" goto HELP
if "%1"=="-?" goto HELP
if "%1"=="/?" goto HELP

if "%1"=="-prop" goto LOAD_PROPERTIES
if "%1"=="-properties" goto LOAD_PROPERTIES
if "%1"=="-propertyfile" goto LOAD_PROPERTIES

call ant.bat -buildfile shell.xml %1 %2 %3 %4 %5 %6
goto END

:LOAD_PROPERTIES
call ant.bat -propertyfile %1 -buildfile shell.xml %2 %3 %4 %5 %6
goto END

:HElP
call ant.bat -diagnostics
REM call ant.bat -help
ant.bat -projecthelp -buildfile shell.xml
call ant.bat -buildfile shell.xml man %2 %3 %4 %5 %6
:END
