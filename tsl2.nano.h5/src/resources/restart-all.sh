echo ======================================================
echo RESTARTING ALL TSL2NANO APPLICATIONS
echo Thomas Schneider / 08.2018
echo ======================================================

###########################################################
#    All NanoH5 Apps must be inside an own sub-directory
#    having the executable script 'runasservice.sh' 
###########################################################

for d in $(ls -d */)
do
	if [[ -f $d"runasservice.sh" ]]; then
		cd $d
		./runasservice.sh stop
		sleep 2
		./runasservice.sh start &Z
		cd ..
		echo "==> $d RESTARTET"
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

if [[ "$dotail" ]]; then
	for d in $(ls -d */)
	do
		if [[ -f $d"nohup.out" ]]; then
			TAILFILES=$TAILFILES $d"nohup.out"
		fi
	done
	tail -f $TAILFILES
fi