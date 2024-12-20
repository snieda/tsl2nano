echo ======================================================
echo RESTARTING ALL TSL2NANO APPLICATIONS
echo Thomas Schneider / 08.2018
echo ======================================================

###########################################################
#    All NanoH5 Apps must be inside an own sub-directory
#    having the executable script 'runasservice.sh' 
###########################################################


# activate this block and de-activate that in your projects run.sh to admin the program-version centralized
export NAME=../tsl2.nano.h5
export VERSION=${project.version}
export EXTENSION="-standalone"
#export RESTART_ALL='-Dapp.login.secure=false -Dapp.login.administration=true -Dapp.login.jarfile.fileselector=false'

if [[ $1 == "help" ]]; then
	echo "usage:=========================================================================="
	echo "clean    : removes all backup files (tar.gz and .sik) generated with this script"
	echo "stop     : doesn't restart but stops all services, started by this script"
	echo "help     : prints this help"
	echo "================================================================================"
	exit 0
fi
if [[ $1 == "clean" ]]; then
	echo "cleaning all tsl2nano backup files..."
	rm tsl2nano-all-services.tar.gz
	find . -type f -name '*.tar.gz' -or -name '*.sik' -exec rm -I {} +
fi
echo "refreshing backup 'tsl2nano-all-services.tar.gz'..."
tar -uf tsl2nano-all-services.tar.gz . --exclude *.gz --exclude=*.*ar --exclude *.log --exclude *.sik --exclude *.lck --exclude *.out --exclude target --exclude dist

echo "<html><body><h1>Summary of all Tsl2Nano Services</h1><ul>" > app-index.html
for d in $(ls -d */)
do
	if [[ -f $d"runasservice.sh" ]]; then
		cd $d
		./runasservice.sh stop
		echo "<li><a href=http://$(hostname -I | cut -f1 -d ' '):$(grep -E 'PORT[=][0-9]' runasservice.sh | grep -E '[0-9]+' --only-matching)>$d</a></li>" >> ../app-index.html
		sleep 2
		mv nohup.out nohup.$(date -d "today" +"%Y%m%d%H%M").sik
		./runasservice.sh backup
		if [[ $1 != "stop" ]]; then
			./runasservice.sh start &Z
			echo "==> $d RESTARTET"
		fi
		cd ..
	else
		echo "==> $d has no runasservice.sh --> no nanoh5 directory"
	fi
done
echo "</ul></body></html>" >> app-index.html
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

read -p "start tail for all processes? [Y|n]: " dotail

if [[ "$dotail" != "n" ]]; then
#	for d in $(ls -d */)
#	do
#		if [[ -f $d"nohup.out" ]]; then
#			TAILFILES=$TAILFILES $d"nohup.out"
#		fi
#	done
#	tail -F $TAILFILES
    tail -F `find . -name *.out`
fi
