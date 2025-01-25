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

# generic way to include source script through BASH_SOURCE
SRC_FILE=mainargs.sh
DIR="${BASH_SOURCE%/*}"
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
if [[ ! -f "$DIR/$SRC_FILE"  && ! -f "~/.local/bin/$SRC_FILE" ]]; then
    [[ -d "~/.local/bin" ]] && BINDIR="~/.local/bin/"
    curl -L https://github.com/snieda/tsl2-bash-scripts/raw/refs/heads/master/divers/mainargs.sh -o $BINDIR$SRC_FILE && chmod +x $BINDIR$SRC_FILE
    #(umask 755 && curl -LO https://github.com/snieda/tsl2-bash-scripts/raw/refs/heads/master/divers/mainargs.sh)
    #install -m 755 <( curl -Lf https://github.com/snieda/tsl2-bash-scripts/raw/refs/heads/master/divers/mainargs.sh) ~/.local/bin/$SRC_FILE
fi
. $DIR/mainargs.sh || . mainargs.sh

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
# workaround: maven filter is not able to replace nested variables
NAME_=${project.artifactId}
VERSION_=${project.version}

NAME=${NAME:-$NAME_}
VERSION=${VERSION:-$VERSION_}
(cd tsl2.nano.h5/target && ls *-standalone*.jar) && EXTENSION="-standalone"
[ $EXTENSION != "-virgin" ] && OFFLINE=-Dtsl2nano.offline=true
#MYIP="$(ip -o route get to 8.8.8.8 | sed -n 's/.*src \([0-9.]\+\).*/\1/p')"
#SERVICEURL="-Dservice.url=https://$MYIP:$PORT"
#UH=-Denv.user.home=true
#USERDIR=-Duser.dir=$PRJ
#LLANG="-Duser.country=US -Duser.language=us -Duser.language.format=us"
#COMPAT=-Djava.locale.providers=COMPAT,CLDR # use locale formats of JDK8
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
SECURITY_LEAK=-Dlog4j2.formatMsgNoLookups=true
MODULES=" --add-modules=ALL-SYSTEM --illegal-access=warn \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.util=ALL-UNNAMED \
    --add-opens java.base/java.text=ALL-UNNAMED \
    --add-opens java.base/java.time.format=ALL-UNNAMED \
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens java.base/sun.reflect.annotation=ALL-UNNAMED \
    --add-opens jdk.unsupported/jdk.internal.module=ALL-UNNAMED \
    --add-opens java.base/sun.security.x509=ALL-UNNAMED \
    --add-opens java.base/javax.security.auth=ALL-UNNAMED \
    --add-opens java.base/java.io=ALL-UNNAMED \
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens java.base/java.net=ALL-UNNAMED \
    --add-opens java.base/sun.security.ssl=ALL-UNNAMED \
    --add-opens java.xml/javax.xml.stream.events=ALL-UNNAMED \
    --add-opens java.xml/org.w3c.dom=ALL-UNNAMED \
    --add-opens java.xml/javax.xml.namespace=ALL-UNNAMED \
    --add-opens java.base/java.nio.charset=ALL-UNNAMED \
    --add-exports java.management/sun.management=ALL-UNNAMED \
    --add-exports java.xml/com.sun.org.apache.xerces.internal.dom=ALL-UNNAMED \
    --add-exports jdk.unsupported/jdk.internal.module=ALL-UNNAMED"

java $MODULES $SECURITY_LEAK $IPv4 $OFFLINE $SERVICEURL $UH $COMPAT $LLANG $ENCODING $JSU_ENC $USERDIR $NANO_DEBUG $AGENT $PROXY  $DEBUG \
	$UH $HPROF_CPU $HPROF_HEAP $PROFILER $NO_DB_CHECK $NOSTARTPAGE $INTERNAL_DB \
	$JAVA_OPTS $RESTART_ALL -jar $NAME-$VERSION$EXTENSION.jar $PRJ $PORT $LOG 
#if [ not "$NOPAUSE" == "nopause" ] then 'read -p' fi
