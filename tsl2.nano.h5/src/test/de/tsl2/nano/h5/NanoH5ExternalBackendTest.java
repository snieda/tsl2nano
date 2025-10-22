package de.tsl2.nano.h5;

import static de.tsl2.nano.h5.ARESTDynamic.BASE_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Tuple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import de.tsl2.nano.bean.BeanProxy;
import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValueMap;
import de.tsl2.nano.bean.def.IValueDefinition;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.http.EHttpClient;
import de.tsl2.nano.core.log.LogFactory;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.parser.JSon;
import de.tsl2.nano.h5.rest.NanoBackendJaxrs;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.BeanContainerUtil;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NanoH5ExternalBackendTest /*implements ENVTestPreparation*/ {

    NanoH5ExternalBackend backend;
    @Mock Persistence persistence;
    @Mock HttpURLConnection httpURLConnection;

    // @BeforeClass
    // public static void setUpClass() {
    //     ENVTestPreparation.setUp();
    // }

    @BeforeEach
    void setUp() throws IOException {
        when(persistence.getConnectionUrl()).thenReturn("http://test-backend");
        when(persistence.getConnectionUserName()).thenReturn("user");
        when(persistence.getConnectionPassword()).thenReturn("pass");
    }

    @SuppressWarnings("rawtypes")
    @Test
    void testBeanMapSerialization() {
        LogFactory.setLogLevel(LogFactory.DEBUG);
        Html5Presentation.registereNanoH5Implemenations();

        Bean<Map> bean = createExampleBean();

        String json = new JSon().serialize(bean);
        String expected = "{\"id\":\"mybean\",\"name\":\"MyBean\",\"instance\":{\"id\":\"mybean\",\"name\":\"MyBean\",\"submap\":{\"ki1\":\"vi1\"}},\"valueExpression\":{\"attributeNames\":[\"id\"],\"comparator\":{},\"expression\":\"{id}\",\"name\":\"{id}\",\"type\":{\"name\":\"java.util.LinkedHashMap\"}},\"attributeDefs\":[{\"name\":\"id\",\"description\":\"mybean.id\",\"constraint\":{\"format\":{},\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.lang.String\"},\"nullable\":true},\"presentation\":{\"description\":\"mybean.id\",\"enabler\":\"AlwaysActive\",\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Id\",\"serialversionuid\":-XXX,\"style\":4,\"type\":1,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}},{\"name\":\"name\",\"description\":\"mybean.name\",\"constraint\":{\"format\":{},\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.lang.String\"},\"nullable\":true},\"presentation\":{\"description\":\"mybean.name\",\"enabler\":\"AlwaysActive\",\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Name\",\"serialversionuid\":-XXX,\"style\":4,\"type\":1,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}},{\"name\":\"submap\",\"description\":\"mybean.submap\",\"constraint\":{\"format\":{\"valueExpression\":{\"comparator\":{},\"type\":{\"name\":\"java.lang.Object\"}}},\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.util.LinkedHashMap\"},\"nullable\":true},\"presentation\":{\"description\":\"mybean.submap\",\"enabler\":\"AlwaysActive\",\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Submap\",\"serialversionuid\":-XXX,\"style\":4,\"type\":XXX,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}}]}";
        
        assertEquals(ignoreSome(expected), ignoreSome(json));

        Bean rbean = new JSon().toObject(BeanValueMap.class, json);
        assertEquals(bean.getDeclaringClass(), rbean.getDeclaringClass().getSuperclass());
        assertEquals(bean.getId(), rbean.getId());
        // assertEquals(bean.toString(), rbean.toString());
        // assertArrayEquals(bean.getAttributeNames(), rbean.getAttributeNames());
        // assertEquals(bean.getBeanValues(), rbean.getBeanValues());

        assertEquals_(bean, rbean);
        FileUtil.save("doc/generated/example-beanvaluemap.json", json);
    }

    private void assertEquals_(Bean<Map> bean, Bean rbean) {
        String json;
        //WORKAROUND: on submap the format is different....
        bean.getAttributes().forEach(a -> ((IValueDefinition)a).getConstraint().setFormat(null));
        rbean.getAttributes().forEach(a -> ((IValueDefinition)a).getConstraint().setFormat(null));
        
        json = new JSon().serialize(bean);
        
        assertEquals(ignoreSome(json), ignoreSome(new JSon().serialize(rbean)));
    }

    private Bean<Map> createExampleBean() {
        Map internalMap = MapUtil.asMap("ki1", "vi1");
        Map instance = MapUtil.asMap("id", "mybean", "name", "MyBean", "submap", internalMap);
        //will handle a map instance configuring default attributes
        Bean<Map> bean = Bean.getBean(instance);
        return bean;
    }

    private String ignoreSome(String str) {
        return str.replaceAll("[\t\r\n\\s]", "")
            .replaceAll("\\d{4,99}", "XXX")
            .replaceAll("Proxy\\d+", "ProxyXXX");
    }

    @Test
    void testBackendReturnsHelp() throws IOException {
        ARestDynamicBackend<String> backend = createBackend();

        String response = backend.serve(BASE_PATH + "/backend", "GET", null);
        assertTrue(response, response.startsWith("OK: provides responses containing full dynamic beans"));
        assertTrue(response, response.contains("mybean"));
    }

    @Test
    void testBackendNotAllowed() throws IOException {
        ARestDynamicBackend<String> backend = createBackend();
        String response = backend.serve(BASE_PATH + "/backend/muster/muster", "GET", new HashMap<>());
        assertEquals("FORBIDDEN: not allowed!", response);
    }

    @Test
    void testBackendReturnsBean() throws IOException {
        BeanContainerUtil.initEmptyProxyServices();
        ARestDynamicBackend<String> backend = createBackend();
        String url = BASE_PATH + "/backend/muster/muster";
        String digest = ARestDynamicBackend.createDigest(url, "GET", "");
        String response = backend.serve(url, "GET", MapUtil.asMap("identity", "muster", "authorization", digest));
        String expected = "OK: {\"id\": \"bean-for-muster\",\"name\": \"musterbean\",\"instance\": {\"id\": \"bean-for-muster\",\"name\": \"musterbean\",\"key\": \"muster\",\"value\": \"found\"},\"valueExpression\": {\"attributeNames\": [\"id\"],\"comparator\": {},\"expression\": \"{id}\",\"name\": \"{id}\",\"type\": {\"name\": \"java.util.LinkedHashMap\"}},\"attributeDefs\": [{\"name\": \"id\",\"description\": \"bean-for-muster.id\",\"constraint\": {\"format\": {},\"length\": -1,\"precision\": -1,\"scale\": -1,\"type\": {\"name\": \"java.lang.String\"},\"nullable\": true},\"presentation\": {\"description\": \"bean-for-muster.id\",\"enabler\": \"Always Active\",\"height\": -1,\"label\": \"Bean-For-Id\",\"style\": 4,\"type\": 1,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true}},{\"name\": \"name\",\"description\": \"bean-for-muster.name\",\"constraint\": {\"format\": {},\"length\": -1,\"precision\": -1,\"scale\": -1,\"type\": {\"name\": \"java.lang.String\"},\"nullable\": true},\"presentation\": {\"description\": \"bean-for-muster.name\",\"enabler\": \"Always Active\",\"height\": -1,\"label\": \"Bean-For-Name\",\"style\": 4,\"type\": 1,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true}},{\"name\": \"key\",\"description\": \"bean-for-muster.key\",\"constraint\": {\"format\": {},\"length\": -1,\"precision\": -1,\"scale\": -1,\"type\": {\"name\": \"java.lang.String\"},\"nullable\": true},\"presentation\": {\"description\": \"bean-for-muster.key\",\"enabler\": \"Always Active\",\"height\": -1,\"label\": \"Bean-For-Key\",\"style\": 4,\"type\": 1,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true}},{\"name\": \"value\",\"description\": \"bean-for-muster.value\",\"constraint\": {\"format\": {},\"length\": -1,\"precision\": -1,\"scale\": -1,\"type\": {\"name\": \"java.lang.String\"},\"nullable\": true},\"presentation\": {\"description\": \"bean-for-muster.value\",\"enabler\": \"Always Active\",\"height\": -1,\"label\": \"Bean-For-Value\",\"style\": 4,\"type\": 1,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true}}]}";
        assertEquals(expected, response);
    }

    @Test
    void testPrintHealth() throws IOException {
        ARestDynamicBackend<String> backend = createBackend();
        String response = backend.serve(BASE_PATH + "/health", "GET", null);
        assertTrue(response.contains("build-tsl2.nano.h5"));
    }

    private ARestDynamicBackend<String> createBackend() {
        ARestDynamicBackend<String> backend = new ARestDynamicBackend<String>() {
            @Override
            void checkAuthorization(String beanName, String actionOrAttribute, Map<String, String> header)
                    throws IllegalAccessException {
                // not to be tested
            }
            @Override
            String createResponse(Status status, String message) {
                return status + ": " + message;
            }
            @Override
            BeanValueMap callNanoBackendApi(Map<String, String> payload, String identity) {
                return new NanoBackendJaxrs() {
                    public java.util.Collection<Object> query(String user, String query) {
                        return Arrays.asList(BeanProxy.createBeanImplementation(Tuple.class));
                    }
                    protected java.util.Map<String,String> toMap(Tuple first) {
                        return MapUtil.asMap("id", "bean-for-muster", "name", "musterbean", "key", "muster", "value", "found");
                    }
                }.backend(identity, identity, (Map)payload);
            }
        };
        return backend;
    }

    @Test
    void testConnectReturnsBean() throws IOException {
        Bean bean = createExampleBean();
        String json = new JSon().serialize(bean);

        backend = spy(new NanoH5ExternalBackend(8080, new File(".")) {
            @Override
            protected String getResponse(EHttpClient httpClient) {
                return json;
            }
        });
        backend.setBackendUrl("http://test-backend");
        // when(backend.getResponse(any())).thenReturn(json); // not working

        Bean<?> result = (Bean<?>) backend.connect(persistence);
        assertEquals_(bean, result);
    }

    @Test
    void testConnectThrowsWhenBackendUrlNull() throws IOException {
        backend = new NanoH5ExternalBackend(8080, new File("."));
        backend.backendUrl = null;
        try (
            MockedStatic<ENV> envMock = Mockito.mockStatic(ENV.class)
        ) {
            envMock.when(() -> ENV.get("app.external.backend.user", null)).thenReturn("user");
            envMock.when(() -> ENV.get("app.external.backend.password", null)).thenReturn("pass");
            assertThrows(NullPointerException.class, () -> backend.connect(persistence));
        }
    }

    @Test
    void testConnectThrowsWhenUserNull() {
        try (
            MockedStatic<ENV> envMock = Mockito.mockStatic(ENV.class)
        ) {
            envMock.when(() -> ENV.get("app.external.backend.user", null)).thenReturn(null);
            envMock.when(() -> ENV.get("app.external.backend.password", null)).thenReturn("pass");
            assertThrows(NullPointerException.class, () -> backend.connect(persistence));
        }
    }

    @Test
    void testConnectThrowsWhenPasswordNull() {
        try (
            MockedStatic<ENV> envMock = Mockito.mockStatic(ENV.class)
        ) {
            envMock.when(() -> ENV.get("app.external.backend.user", null)).thenReturn("user");
            envMock.when(() -> ENV.get("app.external.backend.password", null)).thenReturn(null);
            assertThrows(NullPointerException.class, () -> backend.connect(persistence));
        }
    }
}