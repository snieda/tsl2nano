#!/bin/bash
# usage: user.sh {add|check <username> <password>} | {hash <password>}
java -cp ../tsl2.nano.h5-${project.version}.jar:* de.tsl2.nano.h5.User $1 $2 $3
