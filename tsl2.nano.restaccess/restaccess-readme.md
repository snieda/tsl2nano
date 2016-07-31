**nano.restaccess**
__Thomas Schneider / cp 2016__

# Introduction

## Wildfly

Der Wildfly muss vorher gestartet werden:
	{WILDFLY_HOME}\bin\standalone.bat

Die Amin-Console kann gestartet werden über:
	http://localhost:8080
	admin, admin

## maven build

mvn wildfly:deploy -DwarName=beancontainer.war -Dhost=localhost -Dport=9990

## deployment problems

* Caused by: java.lang.IllegalArgumentException: WFLYEE0040: A component named 'GenericServiceBean' is already defined in this module"}}}}
** <-- serviceaccess.jar contained MANIFEST.MF classpath with relative path entries. perhaps deploy a serviceaccess without classpath-entry and without all classes like fileconnector and other utils
* howto deploy external dependencies (local lib jars)?
* howto deploy persistence.xml?
* persistence.xml: transaction-type should be JTA instead of RESOURCE_LOCAL
* "de.tsl2.nano.service.util.FileServiceBean\".fsConnectionFactory is missing
** <-- FileServiceBean raus
* Unable to locate method: org.jboss.resteasy.client.exception.mapper .ApacheHttpClient4ExceptionMapper.mapHttpException(org.apache.http.HttpException) [ERROR] Caused by: java.lang.NoSuchMethodException: org.jboss.resteasy.client.exception.mapper.ApacheHttpClient4ExceptionMapper.mapHttpExcep tion(org.apache.http.HttpException)"}}}}
** <-- <scope>provided</scope>
* java.lang.IllegalArgumentException: WFLYEE0040: A component named 'FileServiceBean' is already defined in this module"
** <-- The problem arises with the EJB Annotation preprocessors - which take your war, and the libs compiled into it and scans them for EJB annotations. Some Jar files can have an entry in the Manifest for "Classpath: ." (or whatever but with '.' as one of the entries). This causes the annotation preprocessor to idiotically process all the jar files in the web-inf lib again. Finally it will get around to a jar file with an EJB annotation in it that it has already seen, because it was already processed earlier - this causes it to complain with "A component Named xxx is already defined".
* die lokalen libs werden nicht mitgepackt
* --> manuell reinpacken und dann jboss-cli.bat: deploy c:\eigen\tsl2\tsl2-workspace\tsl2-nano\tsl2.nano.restaccess\target\beancontainer.war*
* nur wenn die tests durchlaufen, wird deployed
* Wie finde ich die wsdl im wildfly?
* NameNotFoundException: GenericServiceBean

        <subsystem xmlns="urn:jboss:domain:naming:2.0">
            <remote-naming/>
            <bindings>
                <lookup name="java:de.idvag.swartifex.service.util/IGenericService" lookup="java:global/beancontainer/GenericServiceBean!de.idvag.swartifex.service.util.IGenericService"/>
			</bindings>
        </subsystem>

