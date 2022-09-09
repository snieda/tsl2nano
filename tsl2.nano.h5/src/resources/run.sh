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
if [ "$4" == "move" ] 
	then mv $PRJ $PRJ + '~' 
fi

NAME=${project.artifactId}
VERSION=${project.version}
EXTENSION="-standalone"
[ $EXTENSION != "-virgin" ] && OFFLINE=-Dtsl2nano.offline=true
#UH=-Denv.user.home=true
#USERDIR=-Duser.dir=$PRJ
#LLANG=-Duser.country=FR -Duser.language=fr -Duser.language.format=fr
ENCODING=-Dfile.encoding=UTF-8
JSU_ENC=-Dsun.jnu.encoding=UTF-8
#DEBUG="-agentlib:jdwp=transport=dt_socket,address=localhost:8787,server=y,suspend=n"
#AGENT=-javaagent:%PRJ%/aspectjweaver.jar
#PROXY=-Dhttp.proxyHost=myproxy -Dhttp.proxyPort=8080
#HPROF_CPU=-agentlib:hprof=cpu=samples
#HPROF_HEAP=-agentlib:hprof=heap=dump
#PROFILER="-agentpath:...visualvm_138/profiler/lib/deployed/jdk16/windows/profilerinterface.dll=...\visualvm_138\profiler\lib,5140"
JAVA_OPTS="-Xmx512m -Djava.awt.headless=true"
#LOG=">$PRJ.log"
IPv4="-Djava.net.preferIPv4Stack=true"
#NOSTARTPAGE=-Dapp.show.startpage=false
#NO_DB_CHECK=-Dapp.db.check.connection=false
#INTERNAL_DB=-Dapp.database.internal.server.run=true
#TSL_SERVICE=-Dservice.url=https://tsl2-timesheet.herokuapp.com:5000
SECURITY_LEAK=-Dlog4j2.formatMsgNoLookups=true
#MODULES=" --add-modules=ALL-SYSTEM --illegal-access=warn --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/javax.security.auth=ALL-UNNAMED"

java $MODULES $SECURITY_LEAK $IPv4 $OFFLINE $UH $LLANG $ENCODING $JSU_ENC $USERDIR $NANO_DEBUG $AGENT $PROXY  $DEBUG \
	$UH $HPROF_CPU $HPROF_HEAP $PROFILER $NO_DB_CHECK $NOSTARTPAGE $INTERNAL_DB $TSL_SERVICE \
	$JAVA_OPTS $RESTART_ALL -jar $NAME-$VERSION$EXTENSION.jar $PRJ $PORT $LOG 
#if [ not "$NOPAUSE" == "nopause" ] then 'read -p' fi
