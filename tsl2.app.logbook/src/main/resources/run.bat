@echo off
rem ##########################################################################
rem Start script for Nano.H5
rem Creation: Thomas Schneider 09/2013
rem
rem Arguments:
rem   1. project path (e.g. h5.sample)
rem   2. http port
rem   3. 'debug', 'test' or 'nopause' option
rem ##########################################################################

if "%1"=="" (set PRJ=.nanoh5.logbook) else (set PRJ=%1)
if "%2"=="" (set PORT=8067) else (set PORT=%2)
if "%3"=="debug" (set DEBUG="-agentlib:jdwp=transport=dt_socket,address=localhost:8787,server=y,suspend=n")
if "%3"=="ndebug" (set NANO_DEBUG=-Dtsl2.nano.log.level=debug)
if "%3"=="test" (set NANO_TEST=-Dtsl2.nano.test=true)
if "%3"=="nopause" (set NOPAUSE=nopause)
if "%4"=="move" (mv %PRJ% %PRJ%~)
set NAME=${project.artifactId}
set VERSION=${project.version}
rem set EXTENSION=-standalone
set OFFLINE=-Dtsl2nano.offline=true
rem set UH=-Denv.user.home=true
rem set USERDIR=-Duser.dir=%PRJ%
rem set LANG=-Duser.country=FR -Duser.language=fr
set ENCODING=-Dfile.encoding=UTF-8
set JSU_ENC=-Dsun.jnu.encoding=UTF-8
set DEBUG="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n"
rem set AGENT=-javaagent:%PRJ%/aspectjweaver.jar
rem set PROXY=-Dhttp.proxyHost=myproxy -Dhttp.proxyPort=8080
REM set PROFILER="-agentpath:...visualvm_138/profiler/lib/deployed/jdk16/windows/profilerinterface.dll=...\visualvm_138\profiler\lib,5140"
set JAVA_OPTS=-Xmx512m -Djava.awt.headless=true
rem set LOG=">%PRJ%.log"
set IPv4="-Djava.net.preferIPv4Stack=true"
rem set SILENT=true
if "%SILENT%"=="true" (set JAVA=@start javaw) else (set JAVA=java)
set SECURITY_LEAK=-Dlog4j2.formatMsgNoLookups=true
set MODULES=" --add-modules=ALL-SYSTEM --illegal-access=warn \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.util=ALL-UNNAMED \
    --add-opens java.base/java.text=ALL-UNNAMED \
    --add-opens java.base/java.time.format=ALL-UNNAMED \
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens java.base/sun.reflect.annotation=ALL-UNNAMED \
    --add-opens java.base/jdk.internal.module=ALL-UNNAMED \
    --add-opens java.base/sun.security.x509=ALL-UNNAMED \
    --add-opens java.base/javax.security.auth=ALL-UNNAMED \
    --add-opens java.base/java.io=ALL-UNNAMED \
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens java.base/java.net=ALL-UNNAMED \
    --add-opens java.base/sun.security.ssl=ALL-UNNAMED \
    --add-opens java.xml/javax.xml.stream.events=ALL-UNNAMED \
    --add-opens java.xml/org.w3c.dom=ALL-UNNAMED \
    --add-opens java.xml/javax.xml.namespace=ALL-UNNAMED \
    --add-exports java.base/jdk.internal.module=ALL-UNNAMED \
    --add-exports java.management/sun.management=ALL-UNNAMED \
    --add-exports java.xml/com.sun.org.apache.xerces.internal.dom=ALL-UNNAMED"

%JAVA% %MODULES% %SECURITY_LEAK% %IPv4% %OFFLINE% %LANG% %ENCODING% %JSU_ENC% %USERDIR% %NANO_DEBUG% %AGENT% %PROXY% %PROFILER% %JAVA_OPTS% %DEBUG% %UH% %RESTART_ALL% -jar %NAME%-%VERSION%%EXTENSION%.jar %PRJ% %PORT%  %LOG%
if not "%SILENT%"=="true" pause
