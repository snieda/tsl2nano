package de.tsl2.nano.h5;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.bean.def.BeanValueMap;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.core.util.NetUtil;
import de.tsl2.nano.core.util.parser.JSon;


class NanoH5ExternalBackendTest {

    NanoH5ExternalBackend backend;

    @BeforeEach
    void setUp() throws IOException {
        backend = new NanoH5ExternalBackend(8080, new File("."));
        backend.setBackendUrl("http://test-backend");
    }

    @Test
    void testBeanMapSerialization() {
        Html5Presentation.registereNanoH5Implemenations();

        Map internalMap = MapUtil.asMap("ki1", "vi1");
        Map instance = MapUtil.asMap("id", "mybean", "name", "MyBean", "submap", internalMap);
        //will handle a map instance configuring default attributes
        Bean<Map> bean = Bean.getBean(instance);

        String json = new JSon().serialize(bean);
        String expected = "{\"id\":\"mybean\",\"name\":\"MyBean\",\"valueExpression\":{\"attributeNames\":[\"id\"],\"comparator\":{},\"expression\":\"{id}\",\"name\":\"{id}\",\"type\":{\"name\":\"java.util.LinkedHashMap\"}},\"attributeNames\":[\"id\",\"name\",\"submap\"],\"beanValues\":[{\"constraint\":{\"format\":\"[\\x00-\\xFF€]{0,XXX}\",\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.lang.String\"},\"nullable\":true},\"description\":\"mybeanid\",\"presentation\":{\"description\":\"mybeanid\",\"enabler\":{},\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Mybeanid\",\"serialversionuid\":-XXX,\"style\":4,\"type\":1,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}},{\"constraint\":{\"format\":\"[\\x00-\\xFF€]{0,XXX}\",\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.lang.String\"},\"nullable\":true},\"description\":\"mybeanname\",\"presentation\":{\"description\":\"mybeanname\",\"enabler\":\"de.tsl2.nano.action.IActivable$1@9\",\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Mybeanname\",\"serialversionuid\":-XXX,\"style\":4,\"type\":1,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}},{\"constraint\":{\"format\":{\"valueExpression\":{\"comparator\":{},\"type\":{\"name\":\"java.lang.Object\"}}},\"length\":-1,\"precision\":-1,\"scale\":-1,\"type\":{\"name\":\"java.util.LinkedHashMap\"},\"nullable\":true},\"description\":\"mybeansubmap\",\"presentation\":{\"description\":\"mybeansubmap\",\"enabler\":\"de.tsl2.nano.action.IActivable$1@9\",\"gridHeight\":0,\"gridWidth\":0,\"height\":-1,\"label\":\"Mybeansubmap\",\"serialversionuid\":-XXX,\"style\":4,\"type\":XXX,\"width\":-1,\"nesting\":false,\"searchable\":true,\"visible\":true}}]}";
        
        assertEquals(ignoreSome(expected), ignoreSome(json));

        Bean rbean = new JSon().toObject(BeanValueMap.class, json);
        assertEquals(bean.getDeclaringClass(), rbean.getDeclaringClass().getSuperclass());
        assertEquals(bean.getId(), rbean.getId());
        assertArrayEquals(bean.getAttributeNames(), rbean.getAttributeNames());
        assertEquals(bean.getBeanValues(), rbean.getBeanValues());
        assertEquals(bean.toString(), rbean.toString());

        assertEquals(ignoreSome(json), ignoreSome(new JSon().serialize(rbean)));
    }

    private String ignoreSome(String str) {
        return str.replaceAll("[\t\r\n\\s]", "")
            .replaceAll("\\d{4,99}", "XXX")
            .replaceAll("Proxy\\d+", "ProxyXXX");
    }

    @Test
    void testConnectReturnsBean() {
        try (
            MockedStatic<ENV> envMock = Mockito.mockStatic(ENV.class);
            MockedStatic<NetUtil> netUtilMock = Mockito.mockStatic(NetUtil.class);
            MockedStatic<JSon> jsonMock = Mockito.mockStatic(JSon.class, Mockito.CALLS_REAL_METHODS)
        ) {
            envMock.when(() -> ENV.get("app.external.backend.user", null)).thenReturn("user");
            envMock.when(() -> ENV.get("app.external.backend.password", null)).thenReturn("pass");
            netUtilMock.when(() -> NetUtil.get("http://test-backend")).thenReturn("{\"data\":{}}");

            Bean<?> expectedBean = Mockito.mock(Bean.class);
            JSon jsonParser = Mockito.mock(JSon.class);
            jsonMock.when(JSon::new).thenReturn(jsonParser);
            Mockito.when(jsonParser.toObject(Mockito.eq(Bean.class), anyString())).thenReturn(expectedBean);

            Bean<?> result = backend.connect();
            assertSame(expectedBean, result);
        }
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
            assertThrows(NullPointerException.class, backend::connect);
        }
    }

    @Test
    void testConnectThrowsWhenUserNull() {
        try (
            MockedStatic<ENV> envMock = Mockito.mockStatic(ENV.class)
        ) {
            envMock.when(() -> ENV.get("app.external.backend.user", null)).thenReturn(null);
            envMock.when(() -> ENV.get("app.external.backend.password", null)).thenReturn("pass");
            assertThrows(NullPointerException.class, backend::connect);
        }
    }

    @Test
    void testConnectThrowsWhenPasswordNull() {
        try (
            MockedStatic<ENV> envMock = Mockito.mockStatic(ENV.class)
        ) {
            envMock.when(() -> ENV.get("app.external.backend.user", null)).thenReturn("user");
            envMock.when(() -> ENV.get("app.external.backend.password", null)).thenReturn(null);
            assertThrows(NullPointerException.class, backend::connect);
        }
    }
}