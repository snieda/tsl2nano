**nano.restaccess**
__Thomas Schneider / cp 2016__

# Introduction

# maven build

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
