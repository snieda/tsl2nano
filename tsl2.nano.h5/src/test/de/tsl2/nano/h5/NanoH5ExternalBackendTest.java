package de.tsl2.nano.h5;

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
        Map internalMap = MapUtil.asMap("ki1", "vi1");
        Map instance = MapUtil.asMap("key1", internalMap);
        //will handle a map instance configuring default attributes
        Bean<Map> bean = Bean.getBean(instance);

        String json = new JSon().serialize(bean);
        String expected = 
                        "{\"attributeNames\": [\"key1\",\"key1\"],\"attributes\": [{\"constraint\": {\"format\": {\"valueExpression\": {\"comparator\": {},\"type\": {\"name\": \"java.lang.Object\"}}},\"length\": -1,\"precision\": -1,\"scale\": -1,\"type\": {\"name\": \"java.util.LinkedHashMap\"},\"nullable\": true},\"declaringClass\": {\"name\": \"java.util.LinkedHashMap\"},\"description\": \"{ki1=vi1}key1.tooltip\",\"format\": \"@5\",\"id\": \"{ki1=vi1}key1.tooltip\",\"instance\": {\"key1\": {\"ki1\": \"vi1\"}},\"name\": \"key1\",\"parent\": \"@0\",\"parentBean\": {\"clazz\": {\"name\": \"java.lang.Object\"},\"declaringClass\": {\"name\": \"java.lang.Object\"},\"definitionDirectory\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/presentation/\",\"id\": \"{ki1=vi1}key1\",\"name\": \"{ki1=vi1}key1\",\"nameAndPath\": \"{ki1=vi1}key1\",\"path\": \"java.lang\",\"presentable\": {\"description\": \"{ki1=vi1}key1.tooltip\",\"enabler\": {},\"height\": -1,\"label\": \"{Ki1=Vi1}Key1\",\"style\": 0,\"type\": 0,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true},\"presentationHelper\": {\"simpleTextualPresentation\": \"{ki1=vi1}key1\n" + //
                        "\"},\"valueExpression\": {\"comparator\": {},\"expression\": \"{ki1=vi1}key1\",\"name\": \"{ki1=vi1}key1\",\"type\": {\"name\": \"java.lang.Object\"}},\"creatable\": true,\"default\": true,\"defaultInstanceable\": true,\"final\": false,\"interface\": false,\"multiValue\": false,\"nested\": false,\"persistable\": false,\"selectable\": true,\"singleton\": false,\"stale\": false,\"virtual\": true},\"path\": \"java.util.LinkedHashMap.key1\",\"presentation\": {\"description\": \"{ki1=vi1}key1.tooltip\",\"enabler\": \"@12\",\"height\": -1,\"label\": \"{Ki1=Vi1}Key1\",\"style\": 4,\"type\": XXX,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true},\"selectorAction\": {\"actionMode\": 0,\"id\": \"key1.selector\",\"longDescription\": \"...\",\"shortDescription\": \"...\",\"allPermission\": false,\"closingMode\": false,\"creatingExternalContent\": false,\"default\": false,\"enabled\": false,\"running\": false,\"synchron\": true},\"type\": {\"name\": \"java.util.LinkedHashMap\"},\"value\": \"@9\",\"valueExpression\": \"@6\",\"valueFile\": {\"absoluteFile\": {\"absoluteFile\": \"@19\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"canonicalFile\": \"@19\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"freeSpace\": XXX,\"name\": \"{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"parent\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"parentFile\": {\"absoluteFile\": \"@20\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"canonicalFile\": \"@20\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"freeSpace\": XXX,\"name\": \"temp\",\"parent\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5\",\"parentFile\": {\"absoluteFile\": \"@21\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5\",\"canonicalFile\": \"@21\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5\",\"freeSpace\": XXX,\"name\": \"tsl2.nano.h5\",\"parent\": \"/home/ts/workspace/tsl2nano-code\",\"parentFile\": {\"absoluteFile\": \"@22\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code\",\"canonicalFile\": \"@22\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code\",\"freeSpace\": XXX,\"name\": \"tsl2nano-code\",\"parent\": \"/home/ts/workspace\",\"parentFile\": {\"absoluteFile\": \"@23\",\"absolutePath\": \"/home/ts/workspace\",\"canonicalFile\": \"@23\",\"canonicalPath\": \"/home/ts/workspace\",\"freeSpace\": XXX,\"name\": \"workspace\",\"parent\": \"/home/ts\",\"parentFile\": {\"absoluteFile\": \"@24\",\"absolutePath\": \"/home/ts\",\"canonicalFile\": \"@24\",\"canonicalPath\": \"/home/ts\",\"freeSpace\": XXX,\"name\": \"ts\",\"parent\": \"/home\",\"parentFile\": {\"absoluteFile\": \"@25\",\"absolutePath\": \"/home\",\"canonicalFile\": \"@25\",\"canonicalPath\": \"/home\",\"freeSpace\": XXX,\"name\": \"home\",\"parent\": \"/\",\"parentFile\": {\"absoluteFile\": \"@26\",\"absolutePath\": \"/\",\"canonicalFile\": \"@26\",\"canonicalPath\": \"/\",\"freeSpace\": XXX,\"path\": \"/\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace/tsl2nano-code\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": true,\"directory\": false,\"file\": true,\"hidden\": false},\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"canonicalFile\": \"@19\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"freeSpace\": XXX,\"name\": \"{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"parent\": \"temp\",\"parentFile\": {\"absoluteFile\": \"@20\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"canonicalFile\": \"@20\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"freeSpace\": XXX,\"name\": \"temp\",\"path\": \"temp\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": false,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"totalSpace\": XXX,\"usableSpace\": XXX,\"absolute\": false,\"directory\": false,\"file\": true,\"hidden\": false},\"valueId\": \"LinkedHashMap.LinkedHashMap.key1\",\"valueText\": \"{\"ki1\": \"vi1\"}\",\"bean\": false,\"beanCollector\": false,\"doValidation\": true,\"multiValue\": true,\"relation\": false,\"selectable\": true,\"transient\": false,\"virtual\": true}],\"beanAttributes\": \"@2\",\"beanValues\": \"@2\",\"clazz\": {\"name\": \"java.util.LinkedHashMap\"},\"declaringClass\": {\"name\": \"java.util.LinkedHashMap\"},\"definitionDirectory\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/presentation/\",\"id\": \"LinkedHashMap\",\"instance\": \"@8\",\"interfaces\": [{\"name\": \"java.util.Map\"},{\"name\": \"java.lang.Cloneable\"},{\"name\": \"java.io.Serializable\"}],\"multiValueAttributes\": \"@2\",\"name\": \"LinkedHashMap\",\"nameAndPath\": \"java.util.LinkedHashMap\",\"path\": \"java.util\",\"presentable\": {\"description\": \"{ki1=vi1}.tooltip\",\"enabler\": \"@12\",\"height\": -1,\"label\": \"{Ki1=Vi1}\",\"style\": 0,\"type\": 0,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true},\"presentationHelper\": {\"simpleTextualPresentation\": \"LinkedHashMap\n" + //
                        "{Ki1=Vi1}Key1\t{\"ki1\": \"vi1\"}\n" + //
                        "\"},\"valueExpression\": {\"attributeNames\": [\"key1\"],\"comparator\": {},\"expression\": \"{key1}\",\"name\": \"{key1}\",\"type\": {\"name\": \"java.util.LinkedHashMap\"}},\"creatable\": false,\"default\": true,\"defaultInstanceable\": true,\"final\": false,\"interface\": false,\"multiValue\": false,\"nested\": false,\"persistable\": false,\"selectable\": false,\"singleton\": false,\"stale\": false,\"virtual\": false}";
        
        // assertEquals(ignoreSome(expected), ignoreSome(json));

        Bean rbean = new JSon().toObject(Bean.class, json);
        // rbean.setInstance(instance);
        assertEquals(ignoreSome(json), ignoreSome(new JSon().serialize(rbean)));
    }

    private String ignoreSome(String str) {
        return str.replaceAll("[\t\r\n\\s]", "").replaceAll("\\d{4,99}", "XXX");
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