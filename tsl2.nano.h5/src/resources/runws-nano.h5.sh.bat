jarsigner -keystore tsl2store -storepass tsl2tsl2 tsl2.nano.h5.0.7.0.jar tsl2
javaws -uninstall -clearcache
javaws -offline -J-Xdebug -J-Xnoagent -J-Xrunjdwp:transport=dt_socket,server=n,suspend=y,address=8788 nano.h5.jnlp