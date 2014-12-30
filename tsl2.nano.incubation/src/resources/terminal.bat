@echo off
REM ----------------------------------------------------------------
REM Starts the Incubation Terminal Console to provide tsl2nano Tools
REM ----------------------------------------------------------------
call ant -buildfile=shell.xml sh.java -Dsh.java.classname=de.tsl2.nano.incubation.terminal.Terminal -Dsh.java.arg1=terminal.xml -Dsh.java.classpath=./