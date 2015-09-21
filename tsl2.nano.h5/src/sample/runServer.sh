# echo connect-string: jdbc:hsqldb:hsql://localhost:9003
# set HSQLDB=hsqldb-2.3.2.jar

java -cp * org.hsqldb.Server -database timedb -port 9003 -silent false -trace true $1 $2 $3 $4 $5 $6 $7 $8 $9
# exit
read