#!/bin/bash
# start script for tsl2nano as service

APPNAME=$(pwd | xargs basename)
PORT=8067
APPDIR=.nanoh5.$APPNAME
echo "==> $0 ${PWD##*/} $@"
if [ "$1" == "--help" ]
	then
	echo "usage: $0 [stop | backup | --help]"
	echo "  if no parameter was given, this application starts"
	echo "  with environment $APPDIR"
	echo ""
	echo "  stop  : stops the process $APPNAME, if running already"
	echo "  backup: creatings a compressed tar backup file"
	echo "  --help: prints the help screen and exits"
	exit 0
fi
if [ "$1" == "backup" ]
	then
	ARCHIVE_NAME=backup-${PWD##*/}-$(date -d "today" +"%Y%m%d%H%M").tar.gz
	echo "creating backup $ARCHIVE_NAME ..."
	tar --exclude=*.*ar --exclude=*.zip --exclude=temp --exclude=*.log --exclude=*.lck --exclude=target --exclude=dist -czf $ARCHIVE_NAME *.sh $APPDIR .nanoh5.*
	exit 0
fi
if [ "$1" == "stop" ]
	then
	APPID=$(ps -C java -o pid= -o command= | grep $APPNAME | grep -o -E "^[0-9]+")
	if [ "$APPID" == "" ]
		then
		echo "$APPNAME is not running yet..."
		exit 1
	fi
	kill -9 $APPID && echo "$APPNAME stopped successfully" || echo "$APPID $APPNAME could not be stopped!"
	exit 0
fi
shift
nohup ./run.sh $APPDIR $PORT "$@"
# < /dev/null & tail -F $APPDIR/logfactory.log

