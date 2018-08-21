# Tsl2Nano-Tutorial to develop thymeleaf-extension showing an Openstreetmap

The tsl2nano framework provides a full web-application presenting data of any database. The web-application can be configured or extended to include additional elements. One way is to use the plugin mechanism, implementing the interface INanoPlugin or IDOMDecorator. The following tutorial describes this way on presenting an open street map, showing a stored location.

Preconditions:

* java jdk 8 installed 
* maven 3.x installed
* at least one graphical browser like firefox or chrome available
 
## Step 1: Create a new maven project with parent "tsl2.nano.h5.thymeleaf-pack"

Create a new project directory and store the following content as **pom.xml** file:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>net.sf.tsl2nano</groupId>
		<artifactId>tsl2.nano.h5.thymeleaf-pack</artifactId>
		<version>2.1.2</version>
	</parent>
	<groupId>de.tsl2nano.myosm</groupId>
	<artifactId>extensiontest</artifactId>
	<name>TSL2NANO with OSM map</name>
	<properties>
		<project.mainclass>de.my.test.MyNanoApplication</project.mainclass>
	</properties>
</project>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Step 2: Implement the interfaces _INanoPlugin_ and _IDomDecorator_ as main application class

Create a java file in your projects source directory (src/main/java) in package _de.my.test_ with following content:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
package de.my.test;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

import org.w3c.dom.Document;

import de.tsl2.nano.action.IAction;
import de.tsl2.nano.bean.def.AttributeDefinition;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanDefinition;
import de.tsl2.nano.bean.def.IPageBuilder;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.Main;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.h5.HtmlUtil;
import de.tsl2.nano.h5.NanoH5;
import de.tsl2.nano.h5.NanoHTTPD.Method;
import de.tsl2.nano.h5.expression.URLExpression;
import de.tsl2.nano.h5.navigation.IBeanNavigator;
import de.tsl2.nano.h5.plugin.IDOMDecorator;
import de.tsl2.nano.h5.plugin.INanoPlugin;
import de.tsl2.nano.h5.thymeleaf.DOMTLocationOnMap;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.serviceaccess.IAuthorization;

public class MyNanoApplication implements INanoPlugin, IDOMDecorator {

	public static void main(String[] args) {
		Main.startApplication(NanoH5.class, null, args);
	}
	
	public void onAuthentication(IAuthorization auth) {
		// TODO Auto-generated method stub
	}

	public void configuration(SortedMap<Object, Object> properties, Map<Class<?>, Object> services) {
		ENV.extractResource("location-on-googlemaps.html");
		ENV.extractResource("location-on-osm.html");
	}

	public <PAGE, OUTPUT, T extends IPageBuilder<PAGE, OUTPUT>> T definePresentationType(T pageBuilder) {
		return pageBuilder;
	}

	public void defineBeanDefinition(BeanDefinition<?> beanDef) {
		if (beanDef instanceof Bean && beanDef.getName().equals("Location")) {
			AttributeDefinition urlAttr = beanDef.addAttribute(new URLExpression("@http://web.de"));
			urlAttr.getPresentation().setLayout((Serializable)MapUtil.asMap(HtmlUtil.TAG_EMBED, HtmlUtil.TAG_EMBED));
		}
	}

	public void definePersistence(Persistence persistence) {
		// TODO Auto-generated method stub
	}

	public void actionBeforeHandler(IAction<?> action) {
		// TODO Auto-generated method stub
	}

	public void actionAfterHandler(IAction<?> action) {
		// TODO Auto-generated method stub
	}

	public void workflowHandler(IBeanNavigator workflow) {
		// TODO Auto-generated method stub
	}

	public void exceptionHandler(Exception ex) {
		// TODO Auto-generated method stub
	}

	public void requestHandler(String uri, Method m, Map<String, String> header, Map<String, String> parms,
			Map<String, String> files) {
		// TODO Auto-generated method stub
	}

	public void decorate(Document doc, ISession<?> session) {
		BeanDefinition<?> bean = (BeanDefinition<?>) session.getWorkingObject();
		if (bean instanceof Bean && bean.getName().equals("Coordinate")) {
			DOMTLocationOnMap.decoratePanel(doc, session);
		}
	}

	@Override
	public void databaseGenerated(Persistence persistence) {
		// TODO Auto-generated method stub
	}

	@Override
	public void beansGenerated(Persistence persistence) {
		// TODO Auto-generated method stub
	}
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Step 2.b (optional): Create a simple JUnit Integration Test

The following test is only to be seen as entry point for own tests. This small implementation will only start your new application, navigating through a login and the first data page.

Create a new java file **MyNanoApplicationTest.java** in the directory _src/test/java/de/my/test_:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
package de.my.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.h5.NanoH5Unit;

public class MyNanoApplicationTest extends NanoH5Unit {

    @BeforeClass
    public static void setUp() {
        NanoH5Unit.setUp();
    }
    
	@Test
	public void testBeanDefinition() throws Exception {
		HtmlPage page = runWebClient();
		page = submit(page, BTN_LOGIN_OK);
//		assertTrue(page.getElementById("Location") != null);
		page = submit(page, BEANCOLLECTORLIST + BTN_OPEN);
	}
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Step 3: create a thymeleaf template to show an openstreetmap map above the data sheet

Create a new file in the projects resource directory (src/main/resources) with name **location-on-osm.html**:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
<html>
  <head>
    <title>OpenLayers Demo</title>
    <style type="text/css">
      html, body, #basicMap {
          width: 100%;
          height: 100%;
          margin: 0;
      }
    </style>
    <script src="http://www.openlayers.org/api/OpenLayers.js"></script>
    <script  th:inline="javascript">
      function init() {
        map = new OpenLayers.Map("basicMap");
        var mapnik = new OpenLayers.Layer.OSM();
        map.addLayer(mapnik);
        map.setCenter(new OpenLayers.LonLat(/*[[${coordinate_x}]]*/ 10, /*[[${coordinate_y}]]*/ 10) // Center of the map
          .transform(
            new OpenLayers.Projection("EPSG:4326"), // transform from WGS 1984
            new OpenLayers.Projection("EPSG:900913") // to Spherical Mercator Projection
          ), 15 // Zoom level
        );
      }
    </script>
  </head>
  <body onload="init();">
    <div id="basicMap"></div>
  </body>
</html>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Step 4: Run maven install

Let maven create our jar file. Open a console window in your projects directory and enter the following command:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
mvn clean install
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

## Step 5: Start our new application

Starting our jar file will take some minutes. A sample datbase called 'anyway' will be configured, ejb3 entity beans for that model will be generated, a default presentation for that beans will be evaluated and perhaps an online translation will be done.

Open a console, go to the target directory of your project and start the application through:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
java -jar extensiontest-2.1.2.jar
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

### Navigate to the page with the OSM map

On windows, a browser will be opened on 'http://localhost:8067', on other systems you have to do that manually.

Then, do the following:

* Click on the welcome link
* Click on the OK button to login (will take some minutes to initialize)
* Click on the 'Location' item in the list
* Do a Double Click on the first and only entry

Now the Location page will be opened, and if internet is available, open street map will try to show the given location on the map.

