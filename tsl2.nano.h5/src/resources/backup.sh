ARCHIVE_NAME=backup-${PWD##*/}-$(date -d "today" +"%Y%m%d%H%M").tar.gz
tar -czvf $ARCHIVE_NAME *.sh .nanoh5.* --exclude=*.*ar --exclude=*.zip --exclude=temp --exclude=*.log --exclude=*.lck --exclude=target --exclude=dist
