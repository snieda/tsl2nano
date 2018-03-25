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
if "%3"=="debug" (set DEBUG="-agentlib:jdwp=transport=dt_socket,address=localhost:8787,server=y,suspend=n")
if "%3"=="ndebug" (set NANO_DEBUG=-Dtsl2.nano.log.level=debug)
if "%3"=="test" (set NANO_TEST=-Dtsl2.nano.test=true)
if "%3"=="nopause" (set NOPAUSE=nopause)
set VERSION=2.1
set EXTENSION=-standalone
rem set OFFLINE=-Dtsl2nano.offline=true
rem set UH=-Denv.user.home=true
rem set USERDIR=-Duser.dir=%PRJ%
rem set LANG=-Duser.country=FR -Duser.language=fr
set ENCODING=-Dfile.encoding=UTF-8
set JSU_ENC=-Dsun.jnu.encoding=UTF-8
rem set DEBUG="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n"
rem set AGENT=-javaagent:%PRJ%/aspectjweaver.jar
rem set PROXY=-Dhttp.proxyHost=myproxy -Dhttp.proxyPort=8080
REM set PROFILER="-agentpath:...visualvm_138/profiler/lib/deployed/jdk16/windows/profilerinterface.dll=...\visualvm_138\profiler\lib,5140"
set JAVA_OPTS=-Xmx512m -Djava.awt.headless=true
rem set LOG=">%PRJ%.log"
rem set SILENT=true
if "%SILENT%"=="true" (set JAVA=@start javaw) else (set JAVA=java)

%JAVA% %OFFLINE% %LANG% %ENCODING% %JSU_ENC% %USERDIR% %NANO_DEBUG% %AGENT% %PROXY% %PROFILER% %JAVA_OPTS% %DEBUG% %UH% -jar tsl2.nano.h5-%VERSION%%EXTENSION%.jar %PRJ% %PORT%  %LOG%
if not "%SILENT%"=="true" pause
