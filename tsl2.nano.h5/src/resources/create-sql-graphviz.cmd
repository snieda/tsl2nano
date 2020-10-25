mkdir -p doc/graph
curl https://raw.githubusercontent.com/rm-hull/sql_graphviz/master/sql_graphviz.py -o sql_graphviz.py
for i in $(ls -1 *.sql); do python3 sql_graphviz.py $i | tee doc/graph/$i.dot| dot -Tsvg > doc/graph/$i.svg; done;
