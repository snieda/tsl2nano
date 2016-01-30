@echo off
rem ##########################################################################
rem Start script for Nano.H5
rem Creation: Thomas Schneider 09/2013
rem
rem Arguments:
rem   1. project path (e.g. h5.sample)
rem   2. http port
rem   3. 'debug', 'test' or 'nopause' option
rem ##########################################################################

if "%1"=="" (set PRJ=.nanoh5.environment) else (set PRJ=%1)
if "%2"=="" (set PORT=8067) else (set PORT=%2)
if "%3"=="debug" (set DEBUG="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n")
if "%3"=="debug" (set NANO_DEBUG=-Dtsl2.nano.log.level=debug)
if "%3"=="test" (set NANO_TEST=-Dtsl2.nano.test=true)
if "%3"=="nopause" (set NOPAUSE=nopause)
rem set OFFLINE=-Dtsl2nano.offline=true
rem set UH=-Denv.user.home=true
rem set USERDIR=-Duser.dir=%PRJ%
rem set LANG=-Duser.country=FR -Duser.language=fr
rem set DEBUG="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n"
rem set AGENT=-javaagent:%PRJ%/aspectjweaver.jar
rem set JAVA_OPTS=-Dhttp.proxyHost=myproxy -Dhttp.proxyPort=8080
rem @start javaw
java %OFFLINE% %LANG% %USERDIR% %NANO_DEBUG% %AGENT% -Xmx512m -Djava.awt.headless=true %DEBUG% %UH% -jar tsl2.nano.h5.0.8.0.jar %PRJ% %PORT% 
REM -agentpath:...visualvm_138/profiler/lib/deployed/jdk16/windows/profilerinterface.dll=...\visualvm_138\profiler\lib,5140
REM >%PRJ%.log
if not "%NOPAUSE%"=="nopause" pause