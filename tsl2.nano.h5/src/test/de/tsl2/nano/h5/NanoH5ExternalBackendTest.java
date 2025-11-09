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

import org.junit.BeforeClass;
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
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.http.EHttpClient;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.parser.JSon;
import de.tsl2.nano.h5.rest.NanoBackendJaxrs;
import de.tsl2.nano.persistence.Persistence;
import de.tsl2.nano.service.util.BeanContainerUtil;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
// @TestMethodOrder(MethodOrderer.Random.class)
public class NanoH5ExternalBackendTest implements ENVTestPreparation {

    NanoH5ExternalBackend backend;
    @Mock Persistence persistence;
    @Mock HttpURLConnection httpURLConnection;

    @BeforeClass
    public static void setUpClass() {
        ENVTestPreparation.setUp();
    }


    @BeforeEach
    void setUp() throws IOException {
        when(persistence.getConnectionUrl()).thenReturn("http://test-backend");
        when(persistence.getConnectionUserName()).thenReturn("user");
        when(persistence.getConnectionPassword()).thenReturn("pass");
    }

    @Test
    void testBackendReturnsHelp() throws IOException {
        ARestDynamicBackend<String> backend = createBackend();

        String response = backend.serve(BASE_PATH + "/backend/help", "GET", null);
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
        Map<String, ?> auth = EHttpClient.createBasicAuthorization("muster", new char[] {'m', 'u', 's', 't', 'e', 'r'});
        String response = backend.serve(url, "GET", MapUtil.asMap("identity", "muster", "authorization", digest, "Authorization", auth.get("Authorization")));
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
            @SuppressWarnings("unchecked")
            BeanValueMap callNanoBackendApi(Map<String, String> payload, String identity) {
                return new NanoBackendJaxrs() {
                    public java.util.Collection<Object> query(String query, Object...args) {
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
        Bean bean = BeanValueMapTest.createExampleBean();
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
        BeanValueMapTest.assertEquals_(bean, result);
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