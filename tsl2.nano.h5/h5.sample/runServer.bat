@echo off
rem echo connect-string: jdbc:hsqldb:hsql://localhost:9003
if "%1"=="" (set DBNAME=timedb) else (set DBNAME=%1)
set HSQLDB=hsqldb.jar
java -cp %HSQLDB% org.hsqldb.Server -database %DBNAME% -port 9002 -silent false -trace true %1 %2 %3 %4 %5 %6 %7 %8 %9
rem exit
pause