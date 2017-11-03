@ECHO OFF
echo ==========================================================================
echo Windows ant-shell to start ant-scripts of shell.xml
echo Author: Thomas Schneider / cp 2012
echo help:
echo   call 'runsh.bat -help' to see all ant helps and diagnostics.
echo example:
echo   runsh.bat sh.java -Darg1=mypath.MyUnitTest
echo   runsh.bat sh.java -Djava.classname=mypath.MyMainClass -Darg1=myArg1
echo   runsh.bat sh.jar
echo   runsh.bat sh.unjar
echo   runsh.bat -prop my.properties sh.xml.xsl
echo ==========================================================================

if exist ant.bat (
	set ANTRUNNER=ant.bat
) else (
	if "%ANT_HOME%"=="" (
		set ANTRUNNER=java -jar ant-launcher.jar
	) else (
		set ANTRUNNER=ant.bat
	)
)

if "%1"=="-help" goto HELP
if "%1"=="-?" goto HELP
if "%1"=="/?" goto HELP

if "%1"=="-prop" goto LOAD_PROPERTIES
if "%1"=="-properties" goto LOAD_PROPERTIES
if "%1"=="-propertyfile" goto LOAD_PROPERTIES

echo on
call %ANTRUNNER% -buildfile shell.xml %1 %2 %3 %4 %5 %6
goto END

:LOAD_PROPERTIES
call %ANTRUNNER% -propertyfile %1 -buildfile shell.xml %2 %3 %4 %5 %6
goto END

:HElP
call %ANTRUNNER% -diagnostics
REM call %ANTRUNNER% -help
ant.bat -projecthelp -buildfile shell.xml
call %ANTRUNNER% -buildfile shell.xml man %2 %3 %4 %5 %6
:END
