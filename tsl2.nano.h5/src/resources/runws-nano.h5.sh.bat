rem jarsigner -keystore tsl2store -storepass tsl2tsl2 tsl2.nano.h5.1.1.0.jar tsl2
javaws -uninstall -clearcache
javaws -verbose -offline -J-Xdebug -J-Xnoagent -J-Xrunjdwp:transport=dt_socket,server=n,suspend=y,address=8788 nano.h5-debug.jnlp