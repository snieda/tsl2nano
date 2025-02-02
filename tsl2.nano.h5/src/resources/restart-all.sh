echo ======================================================
echo RESTARTING ALL TSL2NANO APPLICATIONS
echo Thomas Schneider / 08.2018
echo ======================================================

###########################################################
#    All NanoH5 Apps must be inside an own sub-directory
#    having the executable script 'runasservice.sh' 
###########################################################

# activate this block and de-activate that in your projects run.sh to admin the program-version centralized
export NAME=${project.artifactId}
export VERSION=${project.version}
#export EXTENSION="-standalone"
[[ "$1" == "localhost" ]] && export MYIP="localhost" && shift
export MYIP=${MYIP:-"$(ip -o route get to 8.8.8.8 | sed -n 's/.*src \([0-9.]\+\).*/\1/p')"}
export OFFLINE="-Dtsl2nano.offline=true"
#export RESTART_ALL='-Dapp.login.secure=false -Dapp.login.administration=true -Dapp.login.jarfile.fileselector=false'
export TSL2_PROJECTS=${TSL2_PROJECTS:-$(ls -d */)}
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
#echo "refreshing backup 'tsl2nano-all-services.tar.gz'..."
tar -uf --exclude *.gz --exclude=*.*ar --exclude *.log --exclude *.sik --exclude *.lck --exclude *.out --exclude target --exclude dist tsl2nano-all-services.tar.gz .

echo -e "RESTARTING TSL2_PROJECTS: \n$TSL2_PROJECTS\n"
echo "with MYIP:$MYIP"

html_body=$(cat <<EOF
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content=
        "width=device-width, initial-scale=1.0" />
    <title>TSL2 Applications</title>
    <style>
        body {
            background: radial-gradient(#9999FF, #000000);
            text-align: center;
            color: white;
        }
        h1 {
            position: relative;
            font-size: 8em;
            transition: 0.5s;
            font-family: Arial, Helvetica, sans-serif;
            text-shadow: 0 1px 0 #ccc, 0 2px 0 #ccc,
                0 3px 0 #ccc, 0 4px 0 #ccc,
                0 5px 0 #ccc, 0 6px 0 #ccc,
                0 7px 0 #ccc, 0 8px 0 #ccc,
                0 9px 0 #ccc, 0 10px 0 #ccc,
                0 11px 0 #ccc, 0 12px 0 #ccc,
                0 20px 30px rgba(0, 0, 0, 0.5);
        }
        div {
            font-size: 1.5em;
        }
        .flex-container { /* the display attribute set to flex creates a "flexbox" display, please see here: https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Flexible_Box_Layout/Basic_Concepts_of_Flexbox */
            display:flex;width:100%;
        }
        .sidebar { /* this container is also set to flex, but here we change the flex-direction to column for the buttons to display in a column */
            display:flex;flex-direction:column;height:100%;justify-content:left;width:30%;align-items:left; /* giving a width of 30% to the sidebar, it will fill 30% of the available space, which is 100% of the window width as defined above */ /* by setting align-items and justify-content to "center" the buttons place themselves in the middle of the sidebar */
        }
    </style>
</head>
<body>
	<h1>TSL2 Applications</h1>
    <div class="flex-container">
	<span class="sidebar">
EOF
)

echo "$html_body" > app-index.html
for d in $TSL2_PROJECTS
do
	if [[ -f $d"runasservice.sh" ]]; then
		cd $d
		./runasservice.sh stop
		echo "<div><a target=app href=https://$MYIP:$(grep -E 'PORT[=][0-9]+' runasservice.sh | grep -E '[0-9]+' --only-matching)>$d</a></div>" >> ../app-index.html
		sleep 2
		mv nohup.out nohup.$(date -d "today" +"%Y%m%d%H%M").sik
		./runasservice.sh backup
		if [[ $1 != "stop" ]]; then
			./runasservice.sh start "-Dapp.show.startpage=false" &
			echo "==> $d RESTARTED"
		fi
		cd ..
	else
		echo "==> $d has no runasservice.sh --> no nanoh5 directory"
	fi
done
echo "</span><span class="flex-container"><iframe name=app width=70% height=100% /> </span></div>" >> app-index.html
echo "</body></html>" >> app-index.html
echo
echo ------------------------------------------------------
echo PROCESSES:
echo ------------------------------------------------------
ps -ef | grep java

if [[ $1 != "stop" ]]; then
	echo
	echo "PORTS:" $(ps -ef | grep java | grep -o -E "(80|90)[0-9]{2}" | sort | tr '\n' ' ')

	echo ======================================================
	echo RESTART SUCCESSFULL
	echo ======================================================

	xdg-open app-index.html

	read -p "start tail for all processes? [Y|n]: " dotail
fi

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
