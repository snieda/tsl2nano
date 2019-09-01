package de.tsl2.nano.codegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.json.XML;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Ignore;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import de.tsl2.nano.core.serialize.YamlUtil;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.util.codegen.XmlGenerator;

public class XmlGeneratorTest {

    private static JSONObject convertToJson(String yamlFile) throws ParseException {
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(FileUtil.getFile(yamlFile));
        JSONObject jsonObject = (JSONObject) new JSONParser().parse(JSONObject.toJSONString(map));
        return jsonObject;
    }

    @Ignore("howto convert json to xml....")
    @Test
    public void testXmlGenerationFromYAML() throws Exception {
        JSONObject json = convertToJson("tsl2.nano.common/src/test/de/tsl2/nano/codegen/petstore-v3.0.yaml");
        String xml = XML.toString(json);

        String file = "tsl2.nano.common/target/petstore-v3.0.xml";
        FileUtil.writeBytes(xml.getBytes(), file, false);
        XmlGenerator.main(new String[] { file, "//schema", "codegen/schema.vm" });
    }
    @Test
    public void testXmlGeneration() throws Exception {
        String file = "tsl2.nano.common/pom.xml";
        XmlGenerator.main(new String[] { file, "//dependency", "codegen/schema.vm" });
    }
}