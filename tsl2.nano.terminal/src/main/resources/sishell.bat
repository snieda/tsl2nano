@echo off
REM ----------------------------------------------------------------
REM Starts the Incubation SI-Shell to provide tsl2nano Tools
REM ----------------------------------------------------------------
REM call ant -buildfile=shell.xml sh.java -Dsh.java.classname=de.tsl2.nano.terminal.SIShell -Dsh.java.arg1=sishell.xml -Dsh.java.classpath=./
java -cp * de.tsl2.nano.terminal.SIShell sishell.xml