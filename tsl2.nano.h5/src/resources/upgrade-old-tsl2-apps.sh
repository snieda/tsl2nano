# prepare old tsl2 apps to be usable with new tsl2 framework version
# Thomas Schneider /2025-02

baseapp=$(pwd | xargs basename)

for a in $@;
do
	echo "=========================================================================="
	echo "UPGRADING $a"
	if -f ../$a/run.sh.old || -f ../$a/run.sh_ || -f ../$a/run.sh~ ;then echo "already done; breaking" && continue; fi;

	envdir=../$a/.nanoh5.$a
	cp -b -v run.sh ../$a
	cp -b -v runasservice.sh ../$a

	[[ ! -f $envdir/icons/blue-planets.jpg ]] && cp .nanoh5.$baseapp/icons/blue-planets.jpg  $envdir/icons

	cp -b -v .nanoh5.$baseapp/environment.xml $envdir
	cp -b -v .nanoh5.$baseapp/users.xml $envdir
	cp -b -v .nanoh5.$baseapp/SADMIN-permissions.xml $envdir
	mv $envdir/hibernate.reveng.xml $envdir/hibernate.reveng.xml.old 2>/dev/null

	rm -y $envdir/hibernate.reveng.xml
	sed -i -e "s/org\.hibernate\.ejb\.HibernatePersistence/org.hibernate.jpa.HibernatePersistenceProvider/g" $envdir/persistence-bean.xml

	bakdir=../$a/backup
	mkdir $bakdir
	#jarfilesToDelete=$(ls *.jar --ignore=h2*.jar --ignore=ddl*.jar) # option ignore does not work togehter with positive file wildcard (*.jar)
	#mv jarfilesToDelete $a/backup
	mv $envdir/*.jar $bakdir 2>/dev/null
	mv $bakdir/$a*.jar $envdir 2>/dev/null
	mv $bakdir/h2-*.jar $envdir 2>/dev/null
	mv $bakdir/ddl*.jar $envdir 2>/dev/null
done
