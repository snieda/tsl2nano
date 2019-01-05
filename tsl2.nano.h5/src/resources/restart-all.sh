echo ======================================================
echo RESTARTING ALL TSL2NANO APPLICATIONS
echo Thomas Schneider / 08.2018
echo ======================================================

###########################################################
#    All NanoH5 Apps must be inside an own sub-directory
#    having the executable script 'runasservice.sh' 
###########################################################

# activate this block and de-activate that in your projects run.sh to admin the program-version centralized
export NAME=../tsl2.nano.h5-
export VERSION=${project.version}
export EXTENSION="-standalone"

for d in $(ls -d */)
do
	if [[ -f $d"runasservice.sh" ]]; then
		cd $d
		./runasservice.sh stop
		sleep 2
		mv nohup.out nohup.$(date -d "today" +"%Y%m%d%H%M").sik
		./backup.sh
		if [[ $1 != "stop" ]]; then
			./runasservice.sh start &Z
			echo "==> $d RESTARTET"
		fi
		cd ..
	else
		echo "==> $d has no runasservice.sh --> no nanoh5 directory"
	fi
done
echo
echo ------------------------------------------------------
echo PROCESSES:
echo ------------------------------------------------------
ps -ef | grep java

echo
echo "PORTS:" $(ps -ef | grep java | grep -o -E "(80|90)[0-9]{2}" | sort | tr '\n' ' ')

echo ======================================================
echo RESTART SUCCESSFULL
echo ======================================================

read -p "start tail for all processes? [y|N]: " dotail

#if [[ "$dotail" ]]; then
#	for d in $(ls -d */)
#	do
#		if [[ -f $d"nohup.out" ]]; then
#			TAILFILES=$TAILFILES $d"nohup.out"
#		fi
#	done
#	tail -F $TAILFILES
#fi
tail -F `find . -name *.out`
