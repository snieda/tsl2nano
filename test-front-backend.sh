#/bin/bash
# test script to run connect frontend to backend
# works, if you cloned the git project to ~/workspace
# it's only an example to understand how the mechanism works
# uasge: ./test-front-backend.sh [-stop] [-clean] [-build]
#   after frontend and backend have started: 
#       * login (in your browser http://localhost:8067) to your backend
#       * login (in your browser http://localhost:8068) to your frontend
#           * this will try to get some sample data from the backend side
#
# example:
# ./test-front-backend.sh -clean -build -stop

tsl2_home="$HOME/workspace/tsl2nano-code"
h5_target="tsl2.nano.h5/target"
url_1='http://localhost:8067/rest/backend'      # example backend implementation
url_2='http://localhost:8067/rest/party/id/1'   # nanoh5 rest interface to connected example database
DEBUG='-agentlib:jdwp=transport=dt_socket,address=localhost:$DEBUGPORT,server=y,suspend=n'

cd $tsl2_home
[[ "$@" == *"-stop"* ]] && pkill --full hsqldb && pkill --full tsl2
[[ "$@" == *"-clean"* ]] && rm -r $tsl2_home/$h5_target
if [[ "$@" == *"-build"* ]]; then
    [[ "$@" == *"-clean"* ]] && mvnd clean install -DskipTests || mvnd verify -o -DskipTests
    [[ "$?" != "0" ]] && exit
fi

cd $h5_target

# run the backend on port 8067
DEBUGPORT=8787
java $(eval echo $DEBUG) -jar tsl2.nano.h5-2.5.9-SNAPSHOT.jar backend 8067 &

echo "you have to login manually for the first time to create the environement with database anyway"

# run the frontend on 8068
DEBUGPORT=8788
java $(eval echo $DEBUG) -Dapp.external.backend.url=$url_2 -jar tsl2.nano.h5-2.5.9-SNAPSHOT.jar frontend 8068 &

tail -F $tsl2_home/$h5_target/frontend/home.log \
        $tsl2_home/$h5_target/backend/home.log \
    | awk '/==> /{print "\033[0m\033[1;36;40m";} /==> p/{print "\033[0m\033[1;33;40m";} {print $0}'
