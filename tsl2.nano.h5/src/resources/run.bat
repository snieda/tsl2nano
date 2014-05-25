@echo off
rem ##########################################################################
rem Start script for Nano.H5
rem Creation: Thomas Schneider 09/2013
rem
rem Arguments:
rem   1. project path (e.g. h5.sample)
rem   2. debug option
rem   3. nopause option
rem ##########################################################################

if "%1"=="" (set PRJ=config) else (set PRJ=%1)
if "%2"=="debug" (set DEBUG="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y")
if "%2"=="debug" (set NANO_DEBUG=-Ddebug)
rem set USERDIR=-Duser.dir=%PRJ%
java  %USERDIR% %NANO_DEBUG% -Djava.awt.headless=true %DEBUG% -jar tsl2.nano.h5.0.0.5.jar %PRJ% 8067 
REM >%PRJ%.log
if not "%3"=="nopause" pause