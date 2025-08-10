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
    void testBeanSerialization() {
        Map internalMap = MapUtil.asMap("ki1", "vi1");
        Map instance = MapUtil.asMap("key1", internalMap);
        //will handle a map instance configuring default attributes
        Bean<Map> bean = Bean.getBean(instance);

        String json = new JSon().serialize(bean);
        Object expected = "{\"actions\": [{\"actionMode\": 2,\"id\": \"linkedhashmap.save\",\"imagePath\": \"icons/save.png\",\"instance\": {\"key1\": {\"ki1\": \"vi1\"}},\"longDescription\": \"&Übernehmen\",\"shortDescription\": \"&Übernehmen\",\"allPermission\": false,\"closingMode\": false,\"creatingExternalContent\": false,\"default\": false,\"enabled\": false,\"running\": false,\"synchron\": true}],\"attributeNames\": [\"key1\",\"key1\"],\"attributes\": [{\"constraint\": {\"format\": {\"valueExpression\": {\"comparator\": {},\"type\": {\"name\": \"java.lang.Object\"}}},\"length\": -1,\"precision\": -1,\"scale\": -1,\"type\": {\"name\": \"java.util.LinkedHashMap\"},\"nullable\": true},\"declaringClass\": {\"name\": \"java.util.LinkedHashMap\"},\"description\": \"{ki1=vi1}key1.tooltip\",\"format\": \"@9\",\"id\": \"{ki1=vi1}key1.tooltip\",\"instance\": \"@3\",\"name\": \"key1\",\"parent\": \"@0\",\"parentBean\": {\"clazz\": {\"name\": \"java.lang.Object\"},\"declaringClass\": {\"name\": \"java.lang.Object\"},\"definitionDirectory\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/presentation/\",\"id\": \"{ki1=vi1}key1\",\"name\": \"{ki1=vi1}key1\",\"nameAndPath\": \"{ki1=vi1}key1\",\"path\": \"java.lang\",\"presentable\": {\"description\": \"{ki1=vi1}key1.tooltip\",\"enabler\": {},\"height\": -1,\"label\": \"{Ki1=Vi1}Key1\",\"style\": 0,\"type\": 0,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true},\"presentationHelper\": {\"simpleTextualPresentation\": \"{ki1=vi1}key1\n" + //
                        "\"},\"valueExpression\": {\"comparator\": {},\"expression\": \"{ki1=vi1}key1\",\"name\": \"{ki1=vi1}key1\",\"type\": {\"name\": \"java.lang.Object\"}},\"creatable\": true,\"default\": true,\"defaultInstanceable\": true,\"final\": false,\"interface\": false,\"multiValue\": false,\"nested\": false,\"persistable\": false,\"selectable\": true,\"singleton\": false,\"stale\": false,\"virtual\": true},\"path\": \"java.util.LinkedHashMap.key1\",\"presentation\": {\"description\": \"{ki1=vi1}key1.tooltip\",\"enabler\": \"@14\",\"height\": -1,\"label\": \"{Ki1=Vi1}Key1\",\"style\": 4,\"type\": 4097,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true},\"selectorAction\": {\"actionMode\": 0,\"id\": \"key1.selector\",\"longDescription\": \"...\",\"shortDescription\": \"...\",\"allPermission\": false,\"closingMode\": false,\"creatingExternalContent\": false,\"default\": false,\"enabled\": false,\"running\": false,\"synchron\": true},\"type\": {\"name\": \"java.util.LinkedHashMap\"},\"value\": \"@4\",\"valueExpression\": \"@10\",\"valueFile\": {\"absoluteFile\": {\"absoluteFile\": \"@21\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"canonicalFile\": \"@21\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"freeSpace\": 53678366720,\"name\": \"{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"parent\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"parentFile\": {\"absoluteFile\": \"@22\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"canonicalFile\": \"@22\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"freeSpace\": 53678366720,\"name\": \"temp\",\"parent\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5\",\"parentFile\": {\"absoluteFile\": \"@23\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5\",\"canonicalFile\": \"@23\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5\",\"freeSpace\": 53678366720,\"name\": \"tsl2.nano.h5\",\"parent\": \"/home/ts/workspace/tsl2nano-code\",\"parentFile\": {\"absoluteFile\": \"@24\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code\",\"canonicalFile\": \"@24\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code\",\"freeSpace\": 53678366720,\"name\": \"tsl2nano-code\",\"parent\": \"/home/ts/workspace\",\"parentFile\": {\"absoluteFile\": \"@25\",\"absolutePath\": \"/home/ts/workspace\",\"canonicalFile\": \"@25\",\"canonicalPath\": \"/home/ts/workspace\",\"freeSpace\": 53678366720,\"name\": \"workspace\",\"parent\": \"/home/ts\",\"parentFile\": {\"absoluteFile\": \"@26\",\"absolutePath\": \"/home/ts\",\"canonicalFile\": \"@26\",\"canonicalPath\": \"/home/ts\",\"freeSpace\": 53678366720,\"name\": \"ts\",\"parent\": \"/home\",\"parentFile\": {\"absoluteFile\": \"@27\",\"absolutePath\": \"/home\",\"canonicalFile\": \"@27\",\"canonicalPath\": \"/home\",\"freeSpace\": 53678366720,\"name\": \"home\",\"parent\": \"/\",\"parentFile\": {\"absoluteFile\": \"@28\",\"absolutePath\": \"/\",\"canonicalFile\": \"@28\",\"canonicalPath\": \"/\",\"freeSpace\": 53678366720,\"path\": \"/\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace/tsl2nano-code\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": true,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": true,\"directory\": false,\"file\": true,\"hidden\": false},\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"canonicalFile\": \"@21\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"freeSpace\": 53678366720,\"name\": \"{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"parent\": \"temp\",\"parentFile\": {\"absoluteFile\": \"@22\",\"absolutePath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"canonicalFile\": \"@22\",\"canonicalPath\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/temp\",\"freeSpace\": 53678366720,\"name\": \"temp\",\"path\": \"temp\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": false,\"directory\": true,\"file\": false,\"hidden\": false},\"path\": \"temp/{ki1=vi1}key1.tooltip-f9ededb6-00b3-3dfe-ae14-6d42b9e0f9fc\",\"totalSpace\": 479285747712,\"usableSpace\": 29256769536,\"absolute\": false,\"directory\": false,\"file\": true,\"hidden\": false},\"valueId\": \"LinkedHashMap.LinkedHashMap.key1\",\"valueText\": \"{\"ki1\": \"vi1\"}\",\"bean\": false,\"beanCollector\": false,\"doValidation\": true,\"multiValue\": true,\"relation\": false,\"selectable\": true,\"transient\": false,\"virtual\": true}],\"beanAttributes\": \"@6\",\"beanValues\": \"@6\",\"clazz\": {\"name\": \"java.util.LinkedHashMap\"},\"declaringClass\": {\"name\": \"java.util.LinkedHashMap\"},\"definitionDirectory\": \"/home/ts/workspace/tsl2nano-code/tsl2.nano.h5/presentation/\",\"id\": \"LinkedHashMap\",\"instance\": \"@3\",\"interfaces\": [{\"name\": \"java.util.Map\"},{\"name\": \"java.lang.Cloneable\"},{\"name\": \"java.io.Serializable\"}],\"multiValueAttributes\": \"@6\",\"name\": \"LinkedHashMap\",\"nameAndPath\": \"java.util.LinkedHashMap\",\"path\": \"java.util\",\"presentable\": {\"description\": \"{ki1=vi1}.tooltip\",\"enabler\": \"@14\",\"height\": -1,\"label\": \"{Ki1=Vi1}\",\"style\": 0,\"type\": 0,\"width\": -1,\"nesting\": false,\"searchable\": true,\"visible\": true},\"presentationHelper\": {},\"valueExpression\": {\"attributeNames\": [\"key1\"],\"comparator\": {},\"expression\": \"{key1}\",\"name\": \"{key1}\",\"type\": {\"name\": \"java.util.LinkedHashMap\"}},\"creatable\": false,\"default\": true,\"defaultInstanceable\": true,\"final\": false,\"interface\": false,\"multiValue\": true,\"nested\": false,\"persistable\": false,\"selectable\": true,\"singleton\": false,\"stale\": false,\"virtual\": false}";
        assertEquals(expected, json);

        Bean rbean = new JSon().toObject(Bean.class, json);
        assertEquals(json, new JSon().serialize(rbean));
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