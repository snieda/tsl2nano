#!/bin/bash
###########################################################################
# Start script for Nano.H5
# Creation: Thomas Schneider 09/2013
#
# Arguments:
#  1. project path `e.g. h5.sample`
#  2. http port
#  3. 'debug', 'test' or 'nopause' option
###########################################################################

if [ "$1" == "" ] 
	then PRJ=.nanoh5.environment
	else PRJ=$1
fi
if [ "$2" == "" ] 
	then PORT=8067
	else PORT=$2
fi
if [ "$3" == "debug" ] 
	then DEBUG="-agentlib:jdwp=transport=dt_socket,address=localhost:8787,server=y,suspend=n"
fi
if [ "$3" == "ndebug" ] 
	then NANO_DEBUG=-Dtsl2.nano.log.level=debug
fi
if [ "$3" == "test" ] 
	then NANO_TEST=-Dtsl2.nano.test=true
fi
if [ "$3" == "nopause" ] 
	then NOPAUSE=nopause
fi

NAME=${project.artifactId}
VERSION=${project.version}
EXTENSION="-standalone"
#OFFLINE=-Dtsl2nano.offline=true
#UH=-Denv.user.home=true
#USERDIR=-Duser.dir=$PRJ
#LLANG=-Duser.country=FR -Duser.language=fr
ENCODING=-Dfile.encoding=UTF-8
JSU_ENC=-Dsun.jnu.encoding=UTF-8
#DEBUG="-agentlib:jdwp=transport=dt_socket,address=localhost:8787,server=y,suspend=n"
#AGENT=-javaagent:%PRJ%/aspectjweaver.jar
#PROXY=-Dhttp.proxyHost=myproxy -Dhttp.proxyPort=8080
#PROFILER="-agentpath:...visualvm_138/profiler/lib/deployed/jdk16/windows/profilerinterface.dll=...\visualvm_138\profiler\lib,5140"
JAVA_OPTS="-Xmx512m -Djava.awt.headless=true"
#LOG=">$PRJ.log"

java $OFFLINE $UH $LLANG $ENCODING $JSU_ENC $USERDIR $NANO_DEBUG $AGENT $PROXY  $DEBUG $UH $PROFILER $JAVA_OPTS -jar $NAME-$VERSION$EXTENSION.jar $PRJ $PORT $LOG 
#if [ not "$NOPAUSE" == "nopause" ] then 'read -p' fi
