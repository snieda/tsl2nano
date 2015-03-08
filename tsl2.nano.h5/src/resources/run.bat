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

if "%1"=="" (set PRJ=config) else (set PRJ=%1)
if "%2"=="" (set PORT=8067) else (set PORT=%2)
if "%3"=="debug" (set DEBUG="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n")
if "%3"=="debug" (set NANO_DEBUG=-Dtsl2.nano.log.level=debug)
if "%3"=="test" (set NANO_TEST=-Dtsl2.nano.test=true)
if "%3"=="nopause" (set NOPAUSE=nopause)
rem set USERDIR=-Duser.dir=%PRJ%
rem @start javaw
java  %USERDIR% %NANO_DEBUG%  -Xmx512m -Djava.awt.headless=true %DEBUG% -jar tsl2.nano.h5.0.7.0.jar %PRJ% %PORT% 
REM -agentpath:...visualvm_138/profiler/lib/deployed/jdk16/windows/profilerinterface.dll=...\visualvm_138\profiler\lib,5140
REM >%PRJ%.log
if not "%NOPAUSE%"=="nopause" pause