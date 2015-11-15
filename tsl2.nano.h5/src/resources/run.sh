#!bash
###########################################################################
# Start script for Nano.H5
# Creation: Thomas Schneider 09/2013
#
# Arguments:
#  1. project path `e.g. h5.sample`
#  2. http port
#  3. 'debug', 'test' or 'nopause' option
###########################################################################

if [ "$1"== "" ] then `set PRJ=config` else `set PRJ=$1` fi
if [ "$2"== "" ] then `set PORT=8067` else `set PORT=$2` fi
if [ "$3"== "debug" ] then `set DEBUG="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n"` fi
if [ "$3"== "debug" ] then `set NANO_DEBUG=-Dtsl2.nano.log.level=debug` fi
if [ "$3"== "test" ] then `set NANO_TEST=-Dtsl2.nano.test=true` fi
if [ "$3"== "nopause" ] then `set NOPAUSE=nopause` fi
#set OFFLINE=-Dtsl2nano.offline=true
#set UH=-Denv.user.home=true
#set USERDIR=-Duser.dir=$PRJ
#set LANG=-Duser.country=FR -Duser.language=fr
#@start javaw
java $OFFLINE $LANG $USERDIR $NANO_DEBUG  -Xmx512m -Djava.awt.headless=true $DEBUG $UH -jar tsl2.nano.h5.0.8.0.jar $PRJ $PORT 
#-agentpath:...visualvm_138/profiler/lib/deployed/jdk16/windows/profilerinterface.dll=...\visualvm_138\profiler\lib,5140
#>$PRJ.log
if [ not "$NOPAUSE" == "nopause" ] then pause fi