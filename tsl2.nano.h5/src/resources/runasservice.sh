#!/bin/bash
# start script for tsl2nano as service

APPNAME=environment
PORT=8067
APPDIR=.nanoh5.$APPNAME
if [ "$1" == "--help" ]
	then
	echo "usage: $0 [stop | --help]"
	echo "  if no parameter was given, this application starts"
	echo "  with environment $APPDIR"
	echo ""
	echo "  stop  : stops the process $APPNAME, if running already"
	echo "  --help: prints the help screen and exits"
	exit 0
fi
if [ "$1" == "stop" ]
	then
	APPID=$(ps -C java -o pid= -o command= | grep $APPNAME | grep -o -E "^[0-9]+" | line)
	if [ "$APPID" == "" ]
		then
		echo "$APPNAME is not running yet..."
		exit 1
	fi
	kill -9 $APPID
	echo "$APPNAME stopped successfully"
	exit 0
fi
nohup ./run.sh $APPDIR $PORT &Z 
# < /dev/null & tail -F $APPDIR/logfactory.log

