set DEBUG="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y"
java  -Djava.awt.headless=true %DEBUG% -jar tsl2.nano.h5.0.0.1.jar h5.sample 8070 >h5.sample/h5.sample.log
pause